#!/bin/bash
set -o errexit
set -o pipefail
set -o nounset

readonly START_DIR=$PWD
readonly SCRIPT_DIR=$(dirname "${BASH_SOURCE[0]}")

if [[ -n ${POSTGRES_HOST:-} ]]; then
    readonly PGCONN="PG:host=$POSTGRES_HOST port=$POSTGRES_PORT user=$POSTGRES_USER password=$POSTGRES_PASSWORD dbname=$POSTGRES_DB"
else
    readonly PGCONN="PG:user=$POSTGRES_USER password=$POSTGRES_PASSWORD dbname=$POSTGRES_DB"
fi

function exec_psql() {
    if [[ -n ${POSTGRES_HOST:-} ]]; then
        PGPASSWORD=$POSTGRES_PASSWORD psql -v ON_ERROR_STOP=1 --host="$POSTGRES_HOST" --port="$POSTGRES_PORT" --dbname="$POSTGRES_DB" --username="$POSTGRES_USER"
    else
        PGPASSWORD=$POSTGRES_PASSWORD psql -v ON_ERROR_STOP=1 --dbname="$POSTGRES_DB" --username="$POSTGRES_USER"
    fi
}

function exec_psql_file() {
    if [[ -n ${POSTGRES_HOST:-} ]]; then
        PGPASSWORD=$POSTGRES_PASSWORD psql -v ON_ERROR_STOP=1 --host="$POSTGRES_HOST" --port="$POSTGRES_PORT" --dbname="$POSTGRES_DB" --username="$POSTGRES_USER" -f $1
    else
        PGPASSWORD=$POSTGRES_PASSWORD psql -v ON_ERROR_STOP=1 --dbname="$POSTGRES_DB" --username="$POSTGRES_USER" -f $1
    fi
}

function wrap_drop_geometry_commands() {
    sed -e '/SELECT DropGeometryColumn/i \\\set ON_ERROR_STOP off' -e '/SELECT DropGeometryColumn/a \\\set ON_ERROR_STOP on'
}

function hide_inserts() {
    grep -v "INSERT 0 1"
}

function import_iso_map() {
    echo "Initial setup"

    echo "Creating PostGIS extension"
    echo "CREATE EXTENSION IF NOT EXISTS postgis;" | exec_psql
    echo "CREATE EXTENSION IF NOT EXISTS postgis_topology;" | exec_psql

    echo "Importing ISO alpha code map"

    curl -fSs https://api.gbif.org/v1/enumeration/country | jq -r '.[] | [ .iso2, .iso3, .title ] | @csv' > /tmp/iso_code_map

    echo "DROP TABLE IF EXISTS iso_map;" | exec_psql
    exec_psql <<EOF
    CREATE TABLE iso_map (
        iso2     char(2) NOT NULL,
        iso3     char(3) NOT NULL,
        title    text    NOT NULL
    );
EOF

    echo "\copy iso_map FROM '/tmp/iso_code_map' CSV;" | exec_psql
    echo "INSERT INTO iso_map VALUES('XK', 'XKX', 'Kosovo');" | exec_psql
}

function import_centroids() {
    echo "Importing Centroids dataset"

    echo "(Re) creating tables"
    echo "DROP TABLE IF EXISTS geolocate_centroids CASCADE;" | exec_psql
    echo "DROP TABLE IF EXISTS coordinatecleaner_centroids CASCADE;" | exec_psql
    echo "
        DROP TABLE IF EXISTS centroids_catalogue;
        CREATE TABLE centroids_catalogue (
            iso3                 CHAR(3),
            iso2                 CHAR(2),
            gadm1                TEXT,
            gbif_name            TEXT,
            type                 TEXT,
            area_sqkm            NUMERIC,
            decimal_longitude    NUMERIC,
            decimal_latitude     NUMERIC,
            source_locality_name TEXT,
            source_reference     TEXT,
            source               TEXT);
        " | exec_psql

    catalogue=https://github.com/jhnwllr/catalogue-of-centroids/raw/v0.0.1/centroids.tsv

    echo "Importing Centroids to PostGIS"
    echo "\copy centroids_catalogue FROM PROGRAM 'curl -LSs --fail $catalogue' DELIMITER E'\t' CSV HEADER" | exec_psql

    echo "Generating Centroids table"
    echo "DROP TABLE IF EXISTS centroids;
        CREATE TABLE centroids AS
            SELECT DISTINCT
                   REGEXP_REPLACE(source || '_' || COALESCE(source_locality_name, ''), '[^[:alpha:][:digit:]_]','','g') AS id,
                   iso2 AS isoCountryCode2Digit,
                   COALESCE(gbif_name, iso2) AS title,
                   decimal_longitude,
                   decimal_latitude,
                   source_reference AS source,
                   ST_SetSRID(ST_MakePoint(decimal_longitude, decimal_latitude), 4326) AS geom
            FROM centroids_catalogue
            WHERE type = 'PCLI'
              AND decimal_longitude IS NOT NULL
              AND decimal_latitude IS NOT NULL
              AND source_reference IS NOT NULL;
        CREATE INDEX centroid_geom_idx ON centroids USING GIST (geom);" | exec_psql

    echo "Centroids import complete"
    echo
}

# Within a bounding box, to mr1 add osmu and remove osmd. To mr2, remove osmu and add osmd.
function use_osm_border_instead() {
    bbox=$1
    osmu=$2
    osmd=$3
    mr1=$4
    mr2=$5

    echo "WITH bbox AS (SELECT ST_GeomFromEWKT('SRID=4326;$bbox') AS geom)," \
        "mru AS (SELECT ST_Intersection(osm.geom, bbox.geom) AS geom FROM bbox, osm WHERE osm_id = '$osmu')," \
        "mrd AS (SELECT ST_Intersection(osm.geom, bbox.geom) AS geom FROM bbox, osm WHERE osm_id = '$osmd')" \
        "UPDATE political SET geom = ST_Multi(ST_Difference(ST_UNION(political.geom, mru.geom), mrd.geom)) FROM mru, mrd WHERE mrgid_eez = $mr1;" | exec_psql

    echo "WITH bbox AS (SELECT ST_GeomFromEWKT('SRID=4326;$bbox') AS geom)," \
        "mru AS (SELECT ST_Intersection(osm.geom, bbox.geom) AS geom FROM bbox, osm WHERE osm_id = '$osmd')," \
        "mrd AS (SELECT ST_Intersection(osm.geom, bbox.geom) AS geom FROM bbox, osm WHERE osm_id = '$osmu')" \
        "UPDATE political SET geom = ST_Multi(ST_Difference(ST_UNION(political.geom, mru.geom), mrd.geom)) FROM mru, mrd WHERE mrgid_eez = $mr2;" | exec_psql
}

function import_political() {
    echo "Downloading Political (Marine Regions Land Union) dataset"

    # EEZ + land union (we're currently on version 3, which uses EEZ version 11):
    # Download from here: http://www.marineregions.org/downloads.php

    mkdir -p /var/tmp/import
    cd /var/tmp/import
    curl -LSs --remote-name --continue-at - --fail https://download.gbif.org/MapDataMirror/2022/05/EEZ_land_union_v3_202003.zip
    mkdir -p political
    unzip -oj EEZ_land_union_v3_202003.zip -d political/

    shp2pgsql -d -D -s 4326 -i -I -W UTF-8 political/EEZ_Land_v3_202030.shp public.political | wrap_drop_geometry_commands > political/political.sql

    echo "Dropping old tables"
    echo "DROP TABLE IF EXISTS political;" | exec_psql

    echo "Importing Marine Regions to PostGIS"
    exec_psql_file political/political.sql

    rm EEZ_land_union_v3_202003.zip political/ -Rf

    echo "DROP TABLE IF EXISTS osm;" | exec_psql

    echo "Importing OSM from $START_DIR/osm"
    cd $START_DIR/osm
    for id in 13407035 192756 304751 2108121 2425963 3777250 2088990 52411 913110 1867188 62269 36989 1650407; do
        [[ -f $id.xml ]] || curl -Ssfo $id.xml https://www.openstreetmap.org/api/0.6/relation/$id/full
        ogr2ogr -append -f PostgreSQL "$PGCONN dbname=$POSTGRES_DB" $id.xml -nln osm -lco GEOMETRY_NAME=geom multipolygons
    done

    # Morocco 8367 https://www.openstreetmap.org/relation/13407035
    # Algeria 8378 https://www.openstreetmap.org/relation/192756
    use_osm_border_instead 'POLYGON((-8.777 27.517,-8.777 35.247,-0.593 35.247,-0.593 27.517,-8.777 27.517))' \
        13407035 192756 8367 8378

    # Indonesia 8492 https://www.openstreetmap.org/relation/304751
    # Malaysia 8483 https://www.openstreetmap.org/relation/2108121
    use_osm_border_instead 'POLYGON((109.067 -1.523,109.067 5.187,119.624 5.187,119.624 -1.523,109.067 -1.523))' \
        304751 2108121 8492 8483

    # Missing Bouvet Island waters: 260 https://www.openstreetmap.org/relation/2425963
    echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '2425963')" \
        "UPDATE political SET geom = osm.geom FROM osm WHERE iso_ter1 = 'BVT';" | exec_psql

    # China / Taiwan: remove Fukien Province from China and add to Taiwan
    # 8486 / 8321 / https://www.openstreetmap.org/relation/3777250
    # https://github.com/gbif/geocode/issues/2
    echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '3777250')" \
        "UPDATE political SET geom = ST_Difference(political.geom, osm.geom) FROM osm WHERE mrgid_eez = '8486';" | exec_psql
    echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '3777250')" \
        "UPDATE political SET geom = ST_Union(political.geom, osm.geom) FROM osm WHERE mrgid_eez = '8321';" | exec_psql

    # Serbia / Kosovo
    # SRB / XKX / https://www.openstreetmap.org/relation/2088990
    echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '2088990')," \
        "xkx AS (SELECT ST_Intersection(osm.geom, political.geom) AS geom FROM political, osm WHERE iso_ter1 = 'SRB')" \
        "INSERT INTO political (\"union\", territory1, iso_ter1, iso_sov1, geom) SELECT 'Kosovo', 'Kosovo', 'XKX', 'XKX', ST_Multi(geom) FROM xkx;" | exec_psql
    echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '2088990')" \
        "UPDATE political SET geom = ST_Difference(political.geom, osm.geom) FROM osm WHERE iso_ter1 = 'SRB';" | exec_psql

    # China / Hong Kong
    # CHN / HKG / https://www.openstreetmap.org/relation/913110
    echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '913110')," \
        "hkg AS (SELECT ST_Intersection(osm.geom, political.geom) AS geom FROM political, osm WHERE iso_ter1 = 'CHN')" \
        "INSERT INTO political (\"union\", territory1, iso_ter1, iso_sov1, geom) SELECT 'Hong Kong', 'Hong Kong', 'HKG', 'HKG', ST_Multi(geom) FROM hkg;" | exec_psql
    echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '913110')" \
        "UPDATE political SET geom = ST_Multi(ST_Difference(political.geom, osm.geom)) FROM osm WHERE iso_ter1 = 'CHN';" | exec_psql
    echo "DELETE FROM political WHERE iso_ter1 = 'HKG' AND ST_NPoints(geom) = 0;" | exec_psql

    # China / Macao
    # CHN / MAC / https://www.openstreetmap.org/relation/1867188
    echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '1867188')," \
        "mac AS (SELECT ST_Intersection(osm.geom, political.geom) AS geom FROM political, osm WHERE iso_ter1 = 'CHN')" \
        "INSERT INTO political (\"union\", territory1, iso_ter1, iso_sov1, geom) SELECT 'Macao', 'Macao', 'MAC', 'MAC', ST_Multi(geom) FROM mac;" | exec_psql
    echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '1867188')" \
        "UPDATE political SET geom = ST_Multi(ST_Difference(political.geom, osm.geom)) FROM osm WHERE iso_ter1 = 'CHN';" | exec_psql
    echo "DELETE FROM political WHERE iso_ter1 = 'MAC' AND ST_NPoints(geom) = 0;" | exec_psql

    # Netherlands / Belgium water bit
    # NLD / BEL / https://www.openstreetmap.org/relation/52411
    echo "WITH bbox AS (SELECT ST_GeomFromEWKT('SRID=4326;POLYGON((4.15 51.008,4.15 51.408,4.439 51.408,4.439 51.008,4.15 51.008))') AS geom)," \
        "osm AS (SELECT ST_Intersection(osm.geom, bbox.geom) AS geom FROM bbox, osm WHERE osm_id = '52411')" \
        "UPDATE political SET geom = ST_Union(political.geom, osm.geom) FROM osm WHERE iso_ter1 = 'BEL';" | exec_psql
    echo "WITH bbox AS (SELECT ST_GeomFromEWKT('SRID=4326;POLYGON((4.15 51.008,4.15 51.408,4.439 51.408,4.439 51.008,4.15 51.008))') AS geom)," \
        "osm AS (SELECT ST_Intersection(osm.geom, bbox.geom) AS geom FROM bbox, osm WHERE osm_id = '52411')" \
        "UPDATE political SET geom = ST_Multi(ST_Difference(political.geom, osm.geom)) FROM osm WHERE iso_ter1 = 'NLD';" | exec_psql

    # United Kingdom / Isle of Man
    # GRB / IMN / https://www.openstreetmap.org/relation/62269/full
    echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '62269')," \
        "imn AS (SELECT ST_Intersection(osm.geom, political.geom) AS geom FROM political, osm WHERE iso_ter1 = 'GBR')" \
        "INSERT INTO political (\"union\", territory1, iso_ter1, iso_sov1, geom) SELECT 'Isle of Man', 'Isle of Man', 'IMN', 'IMN', ST_Multi(geom) FROM imn WHERE NOT ST_IsEmpty(geom);" | exec_psql
    echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '62269')" \
        "UPDATE political SET geom = ST_Multi(ST_Difference(political.geom, osm.geom)) FROM osm WHERE iso_ter1 = 'GBR';" | exec_psql

    # Italy / Vatican City
    # ITA / VCT / https://www.openstreetmap.org/relation/36989
    echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '36989')," \
        "vct AS (SELECT ST_Intersection(osm.geom, political.geom) AS geom FROM political, osm WHERE iso_ter1 = 'ITA')" \
        "INSERT INTO political (\"union\", territory1, iso_ter1, iso_sov1, geom) SELECT 'Vatican City', 'Vatican City', 'VCT', 'VCT', ST_Multi(geom) FROM vct;" | exec_psql
    echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '36989')" \
        "UPDATE political SET geom = ST_Difference(political.geom, osm.geom) FROM osm WHERE iso_ter1 = 'ITA';" | exec_psql

    # Finland / Åland Islands
    # FIN / ALA / https://www.openstreetmap.org/relation/1650407
    echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '1650407')," \
        "ala AS (SELECT ST_Intersection(osm.geom, political.geom) AS geom FROM political, osm WHERE iso_ter1 = 'FIN')" \
        "INSERT INTO political (\"union\", territory1, iso_ter1, iso_sov1, geom) SELECT 'Åland Islands', 'Åland Islands', 'ALA', 'ALA', ST_Multi(geom) FROM ala;" | exec_psql
    echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '1650407')" \
        "UPDATE political SET geom = ST_Difference(political.geom, osm.geom) FROM osm WHERE iso_ter1 = 'FIN';" | exec_psql

    # Remove Antarctica EEZ, based on advice from VLIZ etc (emails with Tim Hirsch, 2019-02-19).
    echo "UPDATE political SET geom = ST_Multi(ST_Difference(political.geom, iho.geom)) FROM iho WHERE iso_ter1 = 'ATA' AND name = 'South Atlantic Ocean';" | exec_psql
    echo "UPDATE political SET geom = ST_Multi(ST_Difference(political.geom, iho.geom)) FROM iho WHERE iso_ter1 = 'ATA' AND name = 'Southern Ocean';" | exec_psql
    echo "UPDATE political SET geom = ST_Multi(ST_Difference(political.geom, iho.geom)) FROM iho WHERE iso_ter1 = 'ATA' AND name = 'South Pacific Ocean';" | exec_psql
    echo "UPDATE political SET geom = ST_Multi(ST_Difference(political.geom, iho.geom)) FROM iho WHERE iso_ter1 = 'ATA' AND name = 'Indian Ocean';" | exec_psql
    echo "UPDATE political SET geom = ST_Multi(ST_Difference(geom, ST_MakeEnvelope(-67.0, -60.1, -35.0, -59.9, 4326))) WHERE iso_ter1 = 'ATA';" | exec_psql
    # And add the sliver that's missing from the bottom
    echo "UPDATE political SET geom = ST_Multi(ST_Union(geom, ST_SetSRID(ST_MakeBox2D(ST_Point(-180, -90), ST_Point(180, -88)), 4326))) WHERE iso_ter1 = 'ATA';" | exec_psql

    # Remove Overlapping Claim South China Sea, MR DB doesn't mark a sovereign claim here (only case of this).
    echo "DELETE from political WHERE mrgid_eez = 49003;" | exec_psql

    # Delete the Joint Regime Areas that completely overlap an ordinary EEZ.
    echo "DELETE FROM political WHERE mrgid_eez IN(48961, 21795, 48970, 48968, 50167, 48974, 48969, 48973, 48975, 48976, 48962, 48964, 48966, 48967);" | exec_psql

    # Add ISO codes where they are missing
    echo "UPDATE political SET iso_ter1 = iso_sov1 WHERE iso_ter1 IS NULL;" | exec_psql
    echo "UPDATE political SET iso_ter2 = iso_sov2 WHERE iso_ter2 IS NULL;" | exec_psql
    echo "UPDATE political SET iso_ter3 = iso_sov3 WHERE iso_ter3 IS NULL;" | exec_psql

    # Add mrgid_ter1 codes where these are oddly missing
    echo "UPDATE political SET mrgid_ter1 = 17589 WHERE iso_ter1 = 'MAC' AND mrgid_ter1 IS NULL;" | exec_psql
    echo "UPDATE political SET mrgid_ter1 = 8759  WHERE iso_ter1 = 'HKG' AND mrgid_ter1 IS NULL;" | exec_psql
    echo "UPDATE political SET mrgid_ter1 = 48519 WHERE iso_ter1 = 'XKX' AND mrgid_ter1 IS NULL;" | exec_psql

    # Set ISO code for Chagos Archiplago, consistent with ISO 3166-1.
    echo "UPDATE political SET iso_ter1 = 'IOT', iso_ter2 = 'MUS' WHERE mrgid_eez = 62589;" | exec_psql

    # Use sovereign codes rather than territory codes for disputed territories
    echo "UPDATE political SET iso_ter2 = iso_sov2 WHERE iso_ter1 IN ('FLK', 'SGS', 'MYT', 'ESH');" | exec_psql

    # France considers the Tromelin Island part of the French Southern and Antarctic Lands
    echo "UPDATE political SET iso_ter1 = 'ATF' WHERE mrgid_eez = 48946;" | exec_psql
    # And the Matthew and Hunter Islands part of New Caledonia
    echo "UPDATE political SET iso_ter1 = 'ATF' WHERE mrgid_eez = 48948;" | exec_psql

    # Note the areas having overlapping claims of some form, particularly these which include land:
    #
    # SELECT DISTINCT "union", iso_ter1, iso_ter2, iso_ter3 FROM political, esri_countries WHERE iso_ter2 IS NOT NULL AND political.geom && esri_countries.geom AND ST_Intersects(political.geom, esri_countries.geom);
    #                            union                           │ iso_ter1 │ iso_ter2 │ iso_ter3
    # ───────────────────────────────────────────────────────────┼──────────┼──────────┼──────────
    #  Abu musa, Greater and Lesser Tunb                         │ ARE      │ IRN      │ ␀
    #  Alhucemas Islands                                         │ ESP      │ MAR      │ ␀
    #  Ceuta                                                     │ ESP      │ MAR      │ ␀
    #  Chafarinas Islands                                        │ ESP      │ MAR      │ ␀
    #  Doumeira Islands                                          │ ERI      │ DJI      │ ␀
    #  Falkland Islands                                          │ FLK      │ ARG      │ ␀
    #  Gibraltar                                                 │ GIB      │ ESP      │ ␀
    #  Glorioso Islands                                          │ MDG      │ ATF      │ ␀
    #  Hala'ib Triangle                                          │ SDN      │ EGY      │ ␀
    #  Kuril Islands                                             │ JPN      │ RUS      │ ␀
    #  Matthew and Hunter Islands                                │ ATF      │ VUT      │ ␀
    #  Mayotte                                                   │ MYT      │ COM      │ ␀
    #  Melilla                                                   │ ESP      │ MAR      │ ␀
    #  Navassa Island                                            │ HTI      │ USA      │ JAM
    #  Overlapping claim: Kenya / Somalia                        │ KEN      │ SOM      │ ␀
    #  Palestine                                                 │ PSE      │ ISR      │ ␀
    #  Perejil Island                                            │ ESP      │ MAR      │ ␀
    #  Peñón de Vélez de la Gomera                               │ ESP      │ MAR      │ ␀
    #  Protected Zone established under the Torres Strait Treaty │ PNG      │ AUS      │ ␀
    #  Senkaku Islands                                           │ TWN      │ JPN      │ CHN
    #  South Georgia and the South Sandwich Islands              │ SGS      │ ARG      │ ␀
    #  Tromelin Island                                           │ ATF      │ MDG      │ MUS
    #  Western Sahara                                            │ ESH      │ MAR      │ ␀

    echo "SELECT AddGeometryColumn('political', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
    echo "UPDATE political SET centroid_geom = ST_Centroid(geom);" | exec_psql

    echo "CREATE INDEX political_eez_iso_3digit ON political (iso_ter1);" | exec_psql

    echo "Marine Regions Land Union import complete"
    echo
}

function import_gadm() {
    echo "Downloading GADM dataset"

    # GADM, version 4.1: https://gadm.org/download_world.html

    mkdir -p /var/tmp/import
    cd /var/tmp/import
    curl -LSs --remote-name --continue-at - --fail https://download.gbif.org/MapDataMirror/2022/08/gadm_410-gpkg.zip || \
        curl -LSs --remote-name --continue-at - --fail https://geodata.ucdavis.edu/gadm/gadm4.1/gadm_410-gpkg.zip
    mkdir -p gadm
    unzip -oj gadm_410-gpkg.zip -d gadm/

    echo "Dropping old tables"
    for i in 0 1 2 3 4 ''; do echo "DROP TABLE IF EXISTS gadm$i CASCADE;" | exec_psql; done

    echo "Importing GADM to PostGIS"
    ogr2ogr -lco GEOMETRY_NAME=geom -f PostgreSQL "$PGCONN" gadm/gadm_410.gpkg -nln gadm

    rm gadm_410-gpkg.zip gadm/ -Rf

    echo "SELECT AddGeometryColumn('gadm', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
    echo "UPDATE gadm SET centroid_geom = ST_Centroid(geom);" | exec_psql

    # GBIF policy
    echo "UPDATE gadm SET name_0 = 'Chinese Taipei' WHERE gid_0 = 'TWN';" | exec_psql
    echo "UPDATE gadm SET name_0 = 'Falkland Islands (Malvinas)' WHERE gid_0 = 'FLK';" | exec_psql

    # UKR.11.1_1 "Darnyts'kyi" (part of Kyiv) is missing vs GADM 3.
    echo "UPDATE gadm SET gid_1 = 'UKR.11_1', name_1 = 'Kiev City', varname_1 = 'Kyiv', nl_name_1 = 'Київ', hasc_1 = 'UA.KC', type_1 = 'Independent City', engtype_1 = 'Independent City', validfr_1 = '~1955', gid_2 = 'UKR.11.1_1', name_2 = 'Darnyts''kyi', varname_2 = 'Darnytskyi', hasc_2 = 'UA.KC.DA', type_2 = 'Raion', engtype_2 = 'Raion', validfr_2 = 'Unknown' WHERE fid = 328778;" | exec_psql

    # MHL.19_1 is a duplicate id, used for both Kili/Bikini/Ejit and Rongelap.
    echo "UPDATE gadm SET gid_1 = 'MHL.24_1' WHERE gid_1 = 'MHL.19_1' AND name_1 = 'Kili / Bikini / Ejit';" | exec_psql

    # Several gid_2 and gid_3 values for Indonesia do not include a _1 or similar version
    echo "UPDATE gadm SET gid_2 = CONCAT(gid_2, '_1') WHERE gid_2 !~ '^.*_[0-9]$' AND gid_0 = 'IDN';" | exec_psql
    echo "UPDATE gadm SET gid_3 = CONCAT(gid_3, '_1') WHERE gid_3 !~ '^.*_[0-9]$' AND gid_0 = 'IDN';" | exec_psql

    # BEN.7_1 "Kouffo" contains level 2 regions marked as part of BEN.9_1
    echo "UPDATE gadm SET gid_2 = 'BEN.7.3_1', gid_3 = REPLACE(gid_3, 'BEN.9.6', 'BEN.7.3') WHERE gid_1 = 'BEN.7_1' AND name_2 = 'Dogbo';" | exec_psql
    echo "UPDATE gadm SET gid_2 = 'BEN.7.6_1', gid_3 = REPLACE(gid_3, 'BEN.9.6', 'BEN.7.6') WHERE gid_1 = 'BEN.7_1' AND name_2 = 'Toviklin';" | exec_psql

    # Similar error for BLR.7_1→BLR.5.11_1 Minsk
    echo "UPDATE gadm SET gid_2 = 'BLR.7.0_1' WHERE gid_2 = 'BLR.5.11_1' AND name_2 = 'Minsk';" | exec_psql

    # And TON.5_1→TON.4.0_1
    echo "UPDATE gadm SET gid_2 = 'TON.5.0_1' WHERE gid_2 = 'TON.4.0_1' AND name_1 = 'Vava''u';" | exec_psql

    # Sanity checks. These could be written as assertions:
    #   DO $$ BEGIN ASSERT (SELECT COUNT(*) FROM gadm WHERE gid_0 !~ '^...$') = 0, 'Bad gid_0 values.'; END; $$;
    # but they are not, as there are outstanding errors in GADM.
    #
    # gid_0 correct form: SELECT gid_0 FROM gadm WHERE gid_0 !~ '^...$';
    # gid_1 correct form: SELECT * FROM gadm WHERE gid_1 !~ '^.*_[0-9]$';
    # gid_2 correct form: SELECT * FROM gadm WHERE gid_2 !~ '^.*_[0-9]$';
    # gid_3 correct form: SELECT * FROM gadm WHERE gid_3 !~ '^.*_[0-9]$';
    # gid_4 correct form: SELECT * FROM gadm WHERE gid_4 !~ '^.*_[0-9]$';
    # gid_5 correct form: SELECT * FROM gadm WHERE gid_5 !~ '^.*_[0-9]$';

    # Britain is too much of a mess to fix.
    # SELECT DISTINCT g1.gid_0, g1.gid_1, g1.gid_2, g1.gid_3, g1.name_1, g1.name_2, g1.name_3 FROM gadm g1 INNER JOIN gadm g2 ON g1.gid_2 = g2.gid_2
    #   WHERE g1.name_2 != g2.name_2 AND g1.gid_2 IS NOT NULL ORDER BY g1.gid_0, g1.gid_1, g1.gid_2, g1.gid_3;
    # Currently shows lots of Britain and a few more.

    nullif="gid_0 = NULLIF(gid_0, '')"
    for i in  name_0 varname_0 \
        gid_1 name_1 varname_1 nl_name_1 iso_1 hasc_1 cc_1 type_1 engtype_1 validfr_1 \
        gid_2 name_2 varname_2 nl_name_2 hasc_2 cc_2 type_2 engtype_2 validfr_2 \
        gid_3 name_3 varname_3 nl_name_3 hasc_3 cc_3 type_3 engtype_3 validfr_3 \
        gid_4 name_4 varname_4 cc_4 type_4 engtype_4 validfr_4 \
        gid_5 name_5 cc_5 type_5 engtype_5 \
        governedby sovereign disputedby region varregion country continent subcont
    do
        nullif="$nullif, $i = NULLIF($i, '')"
    done
    echo "Setting '' to NULL in gadm table"
    echo "UPDATE gadm SET $nullif;" | exec_psql

    echo "Creating gadm4 table"
    echo "
        CREATE TABLE gadm4 AS SELECT
            MIN(fid) AS fid,MIN(uid) AS uid,
            gid_0,name_0,varname_0,
            gid_1,name_1,varname_1,nl_name_1,hasc_1,cc_1,type_1,engtype_1,validfr_1,
            gid_2,name_2,varname_2,nl_name_2,hasc_2,cc_2,type_2,engtype_2,validfr_2,
            gid_3,name_3,varname_3,nl_name_3,hasc_3,cc_3,type_3,engtype_3,validfr_3,
            gid_4,name_4,varname_4,                 cc_4,type_4,engtype_4,validfr_4,
            ST_UNION(geom) AS geom
        FROM gadm
        GROUP BY
            gid_0,name_0,varname_0,
            gid_1,name_1,varname_1,nl_name_1,hasc_1,cc_1,type_1,engtype_1,validfr_1,
            gid_2,name_2,varname_2,nl_name_2,hasc_2,cc_2,type_2,engtype_2,validfr_2,
            gid_3,name_3,varname_3,nl_name_3,hasc_3,cc_3,type_3,engtype_3,validfr_3,
            gid_4,name_4,varname_4,                 cc_4,type_4,engtype_4,validfr_4;" | exec_psql
    echo "CREATE INDEX gadm4_geom_geom_idx ON gadm4 USING GIST (geom);" | exec_psql
    echo "SELECT AddGeometryColumn('gadm4', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
    echo "UPDATE gadm4 SET centroid_geom = ST_Centroid(geom);" | exec_psql

    echo "Creating gadm3 table"
    echo "
        CREATE TABLE gadm3 AS SELECT
            MIN(fid) AS fid,MIN(uid) AS uid,
            gid_0,name_0,varname_0,
            gid_1,name_1,varname_1,nl_name_1,hasc_1,cc_1,type_1,engtype_1,validfr_1,
            gid_2,name_2,varname_2,nl_name_2,hasc_2,cc_2,type_2,engtype_2,validfr_2,
            gid_3,name_3,varname_3,nl_name_3,hasc_3,cc_3,type_3,engtype_3,validfr_3,
            ST_UNION(geom) AS geom
        FROM gadm4
        GROUP BY
            gid_0,name_0,varname_0,
            gid_1,name_1,varname_1,nl_name_1,hasc_1,cc_1,type_1,engtype_1,validfr_1,
            gid_2,name_2,varname_2,nl_name_2,hasc_2,cc_2,type_2,engtype_2,validfr_2,
            gid_3,name_3,varname_3,nl_name_3,hasc_3,cc_3,type_3,engtype_3,validfr_3;" | exec_psql
    echo "CREATE INDEX gadm3_geom_geom_idx ON gadm3 USING GIST (geom);" | exec_psql
    echo "SELECT AddGeometryColumn('gadm3', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
    echo "UPDATE gadm3 SET centroid_geom = ST_Centroid(geom);" | exec_psql

    echo "Creating gadm2 table"
    echo "
        CREATE TABLE gadm2 AS SELECT
            MIN(fid) AS fid,MIN(uid) AS uid,
            gid_0,name_0,varname_0,
            gid_1,name_1,varname_1,nl_name_1,hasc_1,cc_1,type_1,engtype_1,validfr_1,
            gid_2,name_2,varname_2,nl_name_2,hasc_2,cc_2,type_2,engtype_2,validfr_2,
            ST_UNION(geom) AS geom
        FROM gadm3
        GROUP BY
            gid_0,name_0,varname_0,
            gid_1,name_1,varname_1,nl_name_1,hasc_1,cc_1,type_1,engtype_1,validfr_1,
            gid_2,name_2,varname_2,nl_name_2,hasc_2,cc_2,type_2,engtype_2,validfr_2;" | exec_psql
    echo "CREATE INDEX gadm2_geom_geom_idx ON gadm2 USING GIST (geom);" | exec_psql
    echo "SELECT AddGeometryColumn('gadm2', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
    echo "UPDATE gadm2 SET centroid_geom = ST_Centroid(geom);" | exec_psql

    echo "Creating gadm1 table"
    echo "
        CREATE TABLE gadm1 AS SELECT
            MIN(fid) AS fid,MIN(uid) AS uid,
            gid_0,name_0,varname_0,
            gid_1,name_1,varname_1,nl_name_1,hasc_1,cc_1,type_1,engtype_1,validfr_1,
            ST_UNION(geom) AS geom
        FROM gadm2
        GROUP BY
            gid_0,name_0,varname_0,
            gid_1,name_1,varname_1,nl_name_1,hasc_1,cc_1,type_1,engtype_1,validfr_1;" | exec_psql
    echo "CREATE INDEX gadm1_geom_geom_idx ON gadm1 USING GIST (geom);" | exec_psql
    echo "SELECT AddGeometryColumn('gadm1', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
    echo "UPDATE gadm1 SET centroid_geom = ST_Centroid(geom);" | exec_psql

    echo "Creating gadm0 table"
    echo "
        CREATE TABLE gadm0 AS SELECT
            MIN(fid) AS fid,MIN(uid) AS uid,
            gid_0,name_0,varname_0,
            ST_UNION(geom) AS geom
        FROM gadm1
        GROUP BY
            gid_0,name_0,varname_0;" | exec_psql
    echo "CREATE INDEX gadm0_geom_geom_idx ON gadm0 USING GIST (geom);" | exec_psql
    echo "SELECT AddGeometryColumn('gadm0', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
    echo "UPDATE gadm0 SET centroid_geom = ST_Centroid(geom);" | exec_psql

    echo "GADM import complete"
    echo
}

function import_iho() {
    echo "Downloading IHO dataset"

    # IHO version 3: http://www.marineregions.org/downloads.php#iho

    mkdir -p /var/tmp/import
    cd /var/tmp/import
    curl -LSs --remote-name --continue-at - --fail http://download.gbif.org/MapDataMirror/2020/05/World_Seas_IHO_v3.zip
    mkdir -p iho
    unzip -oj World_Seas_IHO_v3.zip -d iho/

    shp2pgsql -d -D -s 4326 -i -I -W UTF-8 iho/World_Seas_IHO_v3.shp public.iho | wrap_drop_geometry_commands > iho/iho.sql

    echo "Dropping old tables"
    echo "DROP TABLE IF EXISTS iho;" | exec_psql

    echo "Importing IHO to PostGIS"
    exec_psql_file iho/iho.sql

    rm World_Seas_IHO_v3.zip iho/ -Rf

    echo "UPDATE iho SET geom = ST_MakeValid(geom) WHERE NOT ST_IsValid(geom);" | exec_psql

    echo "SELECT AddGeometryColumn('iho', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
    echo "UPDATE iho SET centroid_geom = ST_Centroid(geom);" | exec_psql

    echo "IHO import complete"
    echo
}

function import_wgsrpd() {
    echo "Downloading WGSRPD dataset"

    # WGSRPD version 2.0: http://www.kew.org/gis/tdwg/index.html or http://web.archive.org/web/20170215024211/http://www.kew.org/gis/tdwg/index.html

    mkdir -p /var/tmp/import
    cd /var/tmp/import

    for i in 1 2 3 4; do
        curl -LSs --remote-name --continue-at - --fail http://download.gbif.org/MapDataMirror/2020/05/level$i.zip || \
            curl -LSs --remote-name --continue-at - --fail http://web.archive.org/web/20170215024211/http://www.kew.org/gis/tdwg/downloads/level$i.zip
        mkdir -p wgsrpd/level$i
        unzip -oj level$i.zip -d wgsrpd/level$i/

        shp2pgsql -d -D -s 4326 -i -I -W UTF-8 wgsrpd/level$i/level$i.shp public.wgsrpd_level$i | wrap_drop_geometry_commands > wgsrpd/level$i/wgsrpd_level$i.sql

        echo "Dropping old tables"
        echo "DROP TABLE IF EXISTS wgsrpd_level$i;" | exec_psql

        echo "Importing WGSRPD level $i to PostGIS"
        exec_psql_file wgsrpd/level$i/wgsrpd_level$i.sql

        rm level$i.zip wgsrpd/level$i/ -Rf

        echo "UPDATE wgsrpd_level$i SET geom = ST_MakeValid(geom) WHERE NOT ST_IsValid(geom);" | exec_psql

        echo "SELECT AddGeometryColumn('wgsrpd_level$i', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
        echo "UPDATE wgsrpd_level$i SET centroid_geom = ST_Centroid(geom);" | exec_psql
    done

    echo "WGSRPD import complete"
    echo
}

function import_esri_countries() {
    mkdir -p /var/tmp/import
    cd /var/tmp/import
    for i in `seq 8`; do
        curl -LSs --remote-name --fail https://download.gbif.org/MapDataMirror/2022/08/World_Countries_$i.zip
    done

    echo "Dropping old tables"
    echo "DROP TABLE IF EXISTS esri_countries;" | exec_psql
    echo "DROP TABLE IF EXISTS world_countries;" | exec_psql

    echo "Importing ESRI World Countries to PostGIS"
    unzip -d world_countries_1 World_Countries_1.zip
    ogr2ogr -lco GEOMETRY_NAME=geom -f PostgreSQL "$PGCONN" -nlt PROMOTE_TO_MULTI -select 'FID, COUNTRY, ISO_CC, CONTINENT' world_countries_1/World_Countries.shp
    for i in `seq 2 8`; do
        unzip -d world_countries_$i World_Countries_$i.zip
        ogr2ogr -append -lco GEOMETRY_NAME=geom -f PostgreSQL "$PGCONN" -nlt PROMOTE_TO_MULTI -fieldmap '0,1,2,3,-1,-1,-1,-1,-1' world_countries_$i/World_Countries.shp
    done

    echo "ALTER TABLE world_countries RENAME TO esri_countries;" | exec_psql
    rm World_Countries* world_countries* -Rf

    echo "UPDATE esri_countries SET iso_cc = 'XAS' WHERE iso_cc IS NULL AND continent = 'Asia';" | exec_psql

    for i in AGO ARE ARG AUS BGD BRA CAN CHL CHN CMR DNK ECU ESP FIN GLP GRL HND IDN IND IRN ISL JPN KEN MDG MEX MYS NGA NOR QAT RUS SLE SWE TKM URY USA VNM; do
        echo "UPDATE esri_countries SET geom = ST_MakeValid(geom) WHERE iso_cc = '$i' AND NOT ST_IsValid(geom);" | exec_psql
    done
}

function insert_continent_whole_country() {
    echo "
        INSERT INTO continent_step1 (continent, gid_0, geom)
        SELECT '$1', CONCAT(iso_cc, '-', fid), ST_Multi(geom) FROM esri_countries
        WHERE iso_cc IN ($2)
    ;" | exec_psql
}

function insert_continent_cut_country() {
    echo "… $i"
    echo "
        INSERT INTO continent_step1 (continent, gid_0, geom)
        SELECT '$1', CONCAT('$2', '-', '$1'), ST_MakeValid(ST_Multi(ST_Union(ST_Intersection(ccc.geom, ec.geom))))
        FROM
            (SELECT * FROM continent_cutter WHERE UPPER(continent) = '$1') AS ccc,
            (SELECT * FROM esri_countries WHERE iso_cc = '$2') AS ec
    ;" | exec_psql
}

function import_continents() {
    echo "Downloading Continent Cutter dataset"

    # Continent Cutter dataset, version 2021-04-27: https://github.org/gbif/continents

    mkdir -p /var/tmp/import
    cd /var/tmp/import
    curl -LSs --remote-name --fail https://github.com/gbif/continents/raw/master/continent_cookie_cutter.gpkg

    echo "Dropping old tables"
    echo "DROP TABLE IF EXISTS continent_cutter;" | exec_psql

    echo "Importing Continent Cutter to PostGIS"
    ogr2ogr -lco GEOMETRY_NAME=geom -f PostgreSQL "$PGCONN" continent_cookie_cutter.gpkg

    rm continent_cookie_cutter.zip -Rf

    echo "
        DROP TABLE IF EXISTS continent_step1;
        CREATE TABLE continent_step1 (
            key        SERIAL        PRIMARY KEY,
            continent  VARCHAR(128)  NOT NULL,
            gid_0      CHAR(20)      NOT NULL);
        SELECT AddGeometryColumn('continent_step1', 'geom', 4326, 'MULTIPOLYGON', 2);
        " | exec_psql

    # Africa
    echo "Adding Pol/EEZ areas to Africa"
    insert_continent_whole_country AFRICA "'AGO', 'BDI', 'BEN', 'BWA', 'BFA', 'CAF', 'COM', 'CIV', 'CMR', 'COD', 'COG', 'CPV', 'DJI', 'DZA', 'ERI', 'ESH', 'ETH', 'GAB', 'GHA', 'GIN', 'GMB', 'GNB', 'GNQ', 'KEN', 'LBR', 'LBY', 'LSO', 'MAR', 'MDG', 'MLI', 'MYT', 'MOZ', 'MWI', 'MRT', 'MUS', 'NAM', 'NER', 'NGA', 'REU', 'RWA', 'SDN', 'SEN', 'SHN', 'SOM', 'SSD', 'STP', 'SWZ', 'SYC', 'SLE', 'TCD', 'TGO', 'TUN', 'TZA', 'UGA', 'ZMB', 'ZWE'"
    for i in ATF EGY ESP PRT YEM ZAF; do
        insert_continent_cut_country AFRICA $i
    done

    # Antarctica
    echo "Adding Pol/EEZ areas to Antarctica"
    insert_continent_whole_country ANTARCTICA "'ATA', 'BVT', 'HMD', 'SGS'"
    for i in ATF ZAF; do
        insert_continent_cut_country ANTARCTICA $i
    done

    # Asia
    echo "Adding Pol/EEZ areas to Asia"
    insert_continent_whole_country ASIA "'AFG', 'ARE', 'BGD', 'BHR', 'BRN', 'BTN', 'CHN', 'CCK', 'CXR', 'CYP', 'HKG',  'IND', 'IOT', 'IRN', 'IRQ', 'ISR', 'JOR', 'KGZ', 'KHM', 'KOR', 'KWT', 'LAO', 'LBN', 'LKA', 'MAC', 'MDV', 'MMR', 'MNG', 'MYS', 'NPL', 'OMN', 'PAK', 'PHL', 'PRK', 'PSE', 'QAT',  'SAU', 'SGP', 'SYR', 'THA', 'TJK', 'TKM', 'TLS', 'TWN', 'UZB', 'VNM', 'XAS'"
    for i in ARM AZE EGY GEO GRC JPN KAZ IDN RUS TUR YEM; do
        insert_continent_cut_country ASIA $i
    done

    # Europe
    echo "Adding Pol/EEZ areas to Europe"
    insert_continent_whole_country EUROPE "'ALA', 'ALB', 'AND', 'AUT', 'BLR', 'BEL', 'BIH', 'BGR', 'HRV', 'CZE', 'DNK', 'EST', 'FRO', 'FIN', 'DEU', 'GIB', 'GGY', 'HUN', 'ISL', 'IRL', 'IMN', 'ITA', 'JEY', 'LVA', 'LIE', 'LTU', 'LUX', 'MLT', 'MDA', 'MCO', 'MNE', 'NLD', 'MKD', 'NOR', 'POL', 'ROU', 'SMR', 'SRB', 'SVK', 'SVN', 'SJM', 'SWE', 'CHE', 'UKR', 'GBR', 'VAT'"
    for i in AZE ESP FRA GEO GRC KAZ PRT RUS TUR; do
        insert_continent_cut_country EUROPE $i
    done

    # North America
    echo "Adding Pol/EEZ areas to North America"
    insert_continent_whole_country NORTH_AMERICA "'AIA', 'ATG', 'BHS', 'BLM', 'BLZ', 'BMU', 'BRB', 'CAN', 'CRI', 'CUB', 'CYM', 'DMA', 'DOM', 'GLP', 'GRL', 'GRD', 'GTM', 'HND', 'HTI', 'JAM', 'KNA', 'LCA', 'MAF', 'MEX', 'MSR', 'MTQ', 'NIC', 'PAN', 'PRI', 'SLV', 'SPM', 'SXM', 'VCT', 'TCA', 'VGB', 'VIR'"
    for i in BES COL FRA VEN USA UMI; do
        insert_continent_cut_country NORTH_AMERICA $i
    done

    # Oceania
    echo "Adding Pol/EEZ areas to Oceania"
    insert_continent_whole_country OCEANIA "'ASM', 'AUS', 'COK', 'FJI', 'FSM', 'GUM', 'KIR', 'MHL', 'MNP', 'NCL', 'NFK', 'NIU', 'NRU', 'NZL', 'PCN', 'PLW', 'PNG', 'PYF', 'SLB', 'TKL', 'TON', 'TUV', 'VUT', 'WLF', 'WSM'"
    for i in CHL IDN JPN USA UMI; do
        insert_continent_cut_country OCEANIA $i
    done

    # South America
    echo "Adding Pol/EEZ areas to South America"
    insert_continent_whole_country SOUTH_AMERICA "'ARG', 'ABW', 'BOL', 'BRA', 'CUW', 'ECU', 'FLK', 'GUF', 'GUY', 'PER', 'PRY', 'SUR', 'TTO', 'URY'"
    for i in BES CHL COL VEN; do
        insert_continent_cut_country SOUTH_AMERICA $i
    done

    # Caspian Sea (just a box since it will be unioned anyway)
    for i in ASIA EUROPE; do
        echo "… Caspian sea ($i)"
        echo "
            WITH caspian AS (SELECT ST_SetSRID(ST_Expand(ST_Extent(geom), 1.0), 4326) AS geom FROM gadm0 WHERE gid_0 = 'XCA')
            INSERT INTO continent_step1 (continent, gid_0, geom)
            SELECT '$i', CONCAT('XCA', '-', '$i'), ST_MakeValid(ST_Multi(ST_Union(ST_Intersection(ccc.geom, caspian.geom))))
            FROM
                (SELECT * FROM continent_cutter WHERE UPPER(continent) = '$i') AS ccc,
                caspian
        ;" | exec_psql
    done

    # Bottom bit of Antarctica is missing
    echo "INSERT INTO continent_step1 (continent, gid_0, geom) SELECT 'ANTARCTICA', 'ANT-89-90', ST_Multi(ST_SetSRID(ST_MakeBox2D(ST_Point(-180, -90), ST_Point(180, -88)), 4326));" | exec_psql

    echo "Deleting empty geometries"
    echo "DELETE FROM continent_step1 WHERE ST_IsEmpty(geom) OR ST_Area(geom) = 0;" | exec_psql

    echo "Calculating union of continent parts"
    # https://gis.stackexchange.com/questions/431664/deleting-small-holes-in-polygons-specifying-the-size-with-postgis
    exec_psql <<EOF
    CREATE OR REPLACE FUNCTION ST_RemoveHolesInPolygonsByArea(geom GEOMETRY, area real)
    RETURNS GEOMETRY AS
    \$BODY\$
    WITH tbla AS (SELECT ST_Dump(geom))
    SELECT ST_Collect(ARRAY(SELECT ST_MakePolygon(ST_ExteriorRing(geom),
        ARRAY(SELECT ST_ExteriorRing(rings.geom) FROM ST_DumpRings(geom) AS rings
        WHERE rings.path[1]>0 AND ST_Area(rings.geom)>=area))
        FROM ST_Dump(geom))) AS geom FROM tbla;
    \$BODY\$
    LANGUAGE SQL;
EOF
    echo "DROP TABLE IF EXISTS continent;" | exec_psql
    echo "CREATE TABLE continent AS SELECT continent, ST_RemoveHolesInPolygonsByArea(ST_Union(geom), 0.0001) AS geom FROM continent_step1 GROUP BY continent;" | exec_psql

    echo "Creating index"
    echo "CREATE INDEX continent_geom_geom_idx ON continent USING GIST (geom);" | exec_psql
    echo "SELECT AddGeometryColumn('continent', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
    echo "UPDATE continent SET centroid_geom = ST_Centroid(geom);" | exec_psql

    echo "Continents import complete"
    echo
}

function create_cache() {

    exec_psql <<EOF
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO PUBLIC;

    CREATE TABLE IF NOT EXISTS tile_cache (
        layer      varchar(128)  NOT NULL,
        z          int           NOT NULL,
        x          int           NOT NULL,
        y          int           NOT NULL,
        tile       bytea         NOT NULL,
        timeTaken  int           NULL,
        created    timestamp     DEFAULT NOW()
    );
    GRANT INSERT ON tile_cache TO eez;
EOF
}

function create_combined_function() {
    cd $START_DIR
    exec_psql_file $SCRIPT_DIR/all_layer_function.sql
}

function create_gadm_indices() {
    cd $START_DIR
    exec_psql_file $SCRIPT_DIR/gadm_indices.sql
}

if [[ -e import-complete ]]; then
    echo "Data already imported"
else
    echo "Importing data"
    import_iso_map
    create_cache
    import_centroids
    import_iho
    import_political
    import_gadm
    create_gadm_indices
    import_wgsrpd
    import_esri_countries
    import_continents
    create_combined_function
    touch import-complete
fi
