#!/bin/bash
set -o errexit
set -o pipefail
set -o nounset

readonly START_DIR=$PWD
readonly SCRIPT_DIR=$(dirname "${BASH_SOURCE[0]}")

function exec_psql_file() {
    if [[ -n ${POSTGRES_HOST:-} ]]; then
        PGPASSWORD=$POSTGRES_PASSWORD psql -v ON_ERROR_STOP=1 --host="$POSTGRES_HOST" --port="$POSTGRES_PORT" --dbname="$POSTGRES_DB" --username="$POSTGRES_USER" -f $1
    else
        PGPASSWORD=$POSTGRES_PASSWORD psql -v ON_ERROR_STOP=1 --dbname="$POSTGRES_DB" --username="$POSTGRES_USER" -f $1
    fi
}

function exec_pgsql2shp() {
    outfile=$1
    shift
    if [[ -n ${POSTGRES_HOST:-} ]]; then
        pgsql2shp -f $outfile -h $POSTGRES_HOST -p $POSTGRES_PORT -u $POSTGRES_USER -P "$POSTGRES_PASSWORD" $POSTGRES_DB "$@"
    else
        PGPASSWORD=$POSTGRES_PASSWORD psql -v ON_ERROR_STOP=1 --dbname="$POSTGRES_DB" --username="$POSTGRES_USER"
    fi
}

function exec_ogr2ogr() {
    outfile=$1
    shift
    if [[ -n ${POSTGRES_HOST:-} ]]; then
        ogr2ogr -f GPKG $outfile.gpkg "PG:host=$POSTGRES_HOST dbname=$POSTGRES_DB user=$POSTGRES_USER port=$POSTGRES_PORT" -sql "$@"
    else
        ogr2ogr -f GPKG $outfile.gpkg "PG:dbname=$POSTGRES_DB user=$POSTGRES_USER" -sql "$@"
    fi
}

function export_centroids() {
    echo "Exporting Centroids to shapefile"
    exec_pgsql2shp layers/centroids "SELECT id, isoCountryCode2Digit AS name, isoCountryCode2Digit, ST_Expand(geom, 0.00001) FROM centroids"

    echo "Centroids shapefile export complete"
    echo
}

function export_political() {
    echo "Exporting Political to shapefile"
    exec_pgsql2shp layers/political_subdivided "SELECT COALESCE(mrgid_eez, mrgid_ter1) AS id, \"union\" AS name, CONCAT_WS(' ', im1.iso2, im2.iso2, im3.iso2) AS isoCountryCode2Digit, geom FROM political_subdivided eez LEFT OUTER JOIN iso_map im1 ON eez.iso_ter1 = im1.iso3 LEFT OUTER JOIN iso_map im2 ON eez.iso_ter2 = im2.iso3 LEFT OUTER JOIN iso_map im3 ON eez.iso_ter3 = im3.iso3"

    echo "Political shapefile export complete"
    echo
}

function export_gadm() {
    echo "Exporting GADM to shapefile"
    exec_pgsql2shp layers/gadm_subdivided "SELECT gid_0, gid_1, gid_2, gid_3, name_0, name_1, name_2, name_3, iso_map.iso2 AS isoCountryCode2Digit, geom FROM gadm3_subdivided LEFT OUTER JOIN iso_map ON gadm3_subdivided.gid_0 = iso_map.iso3"

    echo "GADM shapefile export complete"
    echo
}

function export_iho() {
    echo "Exporting IHO to shapefile"
    exec_pgsql2shp layers/iho_subdivided "SELECT 'http://marineregions.org/mrgid/' || mrgid AS id, name, NULL AS isoCountryCode2Digit, geom FROM iho_subdivided"

    echo "IHO shapefile export complete"
    echo
}

function export_iucn() {
    echo "Exporting IUCN to shapefile"
    for i in `seq 0 25`; do
        let 'o = i * 500000' || true
        exec_pgsql2shp layers/iucn_subdivided_$i "SELECT id_no::text AS id, CONCAT_WS(' ', sci_name, subspecies, subpop, island) AS title, NULL AS isoCountryCode2Digit, geom FROM iucn_subdivided LIMIT 500000 OFFSET $o;"
    done

    echo "IUCN shapefile export complete"
    echo
}

function export_wdpa() {
    echo "Exporting WDPA to geopackage"
    exec_pgsql2shp layers/wdpa_1 "SELECT \"wdpaId\"::text AS id, name, NULL AS isoCountryCode2Digit, geom FROM wdpa_subdivided ORDER BY \"wdpaParcelId\" LIMIT 500000;"
    exec_pgsql2shp layers/wdpa_2 "SELECT \"wdpaId\"::text AS id, name, NULL AS isoCountryCode2Digit, geom FROM wdpa_subdivided ORDER BY \"wdpaParcelId\" LIMIT 500000 OFFSET 500000;"
    exec_pgsql2shp layers/wdpa_3 "SELECT \"wdpaId\"::text AS id, name, NULL AS isoCountryCode2Digit, geom FROM wdpa_subdivided ORDER BY \"wdpaParcelId\" LIMIT 500000 OFFSET 1000000;"

    echo "WDPA geopackage export complete"
    echo
}

function export_wgsrpd() {
    echo "Exporting WGSRPD to shapefile"
    exec_pgsql2shp layers/wgsrpd_subdivided "SELECT 'WGSRPD:' || level4_cod AS id, level_4_na AS name, iso_code AS isoCountryCode2Digit, geom FROM wgsrpd_level4_subdivided"

    echo "WGSRPD shapefile export complete"
    echo
}

function export_continents() {
    echo "Exporting Continents to shapefile"
    exec_pgsql2shp layers/continents_subdivided "SELECT continent AS id, continent AS name, NULL AS isoCountryCode2Digit, geom FROM continent_subdivided"

    echo "Continents shapefile export complete"
    echo
}

function subdivide_layers() {
    cd $START_DIR
    exec_psql_file $SCRIPT_DIR/subdivide_layers.sql

    echo "Subdividing layers complete"
    echo
}

if [[ -e layers-subdivided ]]; then
    echo "Layers already subdivided"
else
    echo "Subdividing layers"
    subdivide_layers
    [[ -w . ]] && touch layers-subdivided
fi

if [[ -e export-complete ]]; then
    echo "Data already exported"
else
    echo "Exporting data"
    mkdir -p layers
    export_centroids
    export_political
    export_gadm
    export_iho
    export_iucn
    export_wdpa
    export_wgsrpd
    export_continents
    [[ -w . ]] && touch export-complete
fi
