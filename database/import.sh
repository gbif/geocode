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

function import_centroids() {
	echo "Importing Centroids dataset"

	echo "Dropping old tables"
	# Generated (for the moment) with GeoLocateCentroids.java "test" in the geocode-ws module.
	echo "DROP TABLE IF EXISTS geolocate_centroids CASCADE;" | exec_psql
	# Generated from the R script in the comment in this file.
	echo "DROP TABLE IF EXISTS coordinatecleaner_centroids CASCADE;" | exec_psql

	echo "Importing Centroids to PostGIS"
	exec_psql_file $SCRIPT_DIR/geolocate_centroids.sql
	exec_psql_file $SCRIPT_DIR/coordinatecleaner_centroids.sql

	echo "Centroids import complete"
	echo
}

# Note: Natural Earth replaced with the Marine Regions Countries Union polygons.
function import_natural_earth() {
	echo "Downloading Natural Earth dataset"

	# Download the [1:10m Cultural Vectors, Admin 0 - Countries file](http://www.naturalearthdata.com/downloads/10m-cultural-vectors/10m-admin-0-countries/)
	# and the [1:10m Cultural Vectors, Admin 0 - Details map units file](http://www.naturalearthdata.com/downloads/10m-cultural-vectors/10m-admin-0-details/).
	# This is version 4.1.0.

	mkdir -p /var/tmp/import
	cd /var/tmp/import
	curl -LSs --remote-name --continue-at - --fail http://download.gbif.org/MapDataMirror/2019/04/ne_10m_admin_0_countries.zip || \
		curl -LSs --remote-name --continue-at - --fail https://www.naturalearthdata.com/http//www.naturalearthdata.com/download/10m/cultural/ne_10m_admin_0_countries.zip
	curl -LSs --remote-name --continue-at - --fail http://download.gbif.org/MapDataMirror/2019/04/ne_10m_admin_0_map_units.zip || \
		curl -LSs --remote-name --continue-at - --fail https://www.naturalearthdata.com/http//www.naturalearthdata.com/download/10m/cultural/ne_10m_admin_0_map_units.zip
	curl -LSs --remote-name --continue-at - --fail http://download.gbif.org/MapDataMirror/2019/04/ne_10m_admin_0_map_subunits.zip || \
		curl -LSs --remote-name --continue-at - --fail https://www.naturalearthdata.com/http//www.naturalearthdata.com/download/10m/cultural/ne_10m_admin_0_map_subunits.zip
	mkdir -p ne
	unzip -oj ne_10m_admin_0_countries.zip -d ne/
	unzip -oj ne_10m_admin_0_map_units.zip -d ne/
	unzip -oj ne_10m_admin_0_map_subunits.zip -d ne/
	#unzip -oj ne_10m_admin_1_states_provinces.zip -d ne/

	echo "Creating PostGIS extension"
	echo "CREATE EXTENSION IF NOT EXISTS postgis;" | exec_psql
	echo "CREATE EXTENSION IF NOT EXISTS postgis_topology;" | exec_psql

	shp2pgsql -d -D -s 4326 -i -I -W UTF-8 ne/ne_10m_admin_0_countries.shp public.political | wrap_drop_geometry_commands > ne/political.sql
	shp2pgsql -d -D -s 4326 -i -I -W UTF-8 ne/ne_10m_admin_0_map_units.shp public.political_map_units | wrap_drop_geometry_commands > ne/political_map_units.sql
	shp2pgsql -d -D -s 4326 -i -I -W UTF-8 ne/ne_10m_admin_0_map_subunits.shp public.political_map_subunits | wrap_drop_geometry_commands > ne/political_map_subunits.sql
	#shp2pgsql -d -D -s 4326 -i -I -W UTF-8 ne/ne_10m_admin_1_states_provinces.shp public.political_states_provinces | wrap_drop_geometry_commands > ne/political_states_provinces.sql

	echo "Dropping old tables"
	echo "DROP TABLE IF EXISTS political;" | exec_psql
	echo "DROP TABLE IF EXISTS political_map_units;" | exec_psql
	echo "DROP TABLE IF EXISTS political_map_subunits;" | exec_psql
	echo "DROP TABLE IF EXISTS political_states_provinces;" | exec_psql

	echo "Importing Natural Earth to PostGIS"
	exec_psql_file ne/political.sql
	exec_psql_file ne/political_map_units.sql
	exec_psql_file ne/political_map_subunits.sql
	#exec_psql_file ne/political_states_provinces.sql

	rm ne_10m_admin_0_countries.zip ne_10m_admin_0_map_units.zip ne_10m_admin_0_map_subunits.zip ne/ -Rf

	echo "SELECT AddGeometryColumn('political', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
	echo "UPDATE political SET centroid_geom = ST_Centroid(geom);" | exec_psql

	echo "CREATE INDEX political_iso_a3 ON political (iso_a3);" | exec_psql

	echo "Natural Earth import complete"
	echo
}

# Note: Marine Regions replaced with the Marine Regions Countries Union polygons.
function import_marine_regions() {
	echo "Downloading Marine Regions dataset"

	# EEZ (we're currently on version 10):
	# Download the Low res version from here: http://www.marineregions.org/downloads.php

	mkdir -p /var/tmp/import
	cd /var/tmp/import
	curl -LSs --remote-name --continue-at - --fail http://download.gbif.org/MapDataMirror/2019/04/World_EEZ_v10_20180221.zip
	mkdir -p eez
	unzip -oj World_EEZ_v10_20180221.zip -d eez/

	shp2pgsql -d -D -s 4326 -i -I -W UTF-8 eez/eez_v10.shp public.eez | wrap_drop_geometry_commands > eez/eez.sql

	echo "Dropping old tables"
	echo "DROP TABLE IF EXISTS eez;" | exec_psql

	echo "Importing Marine Regions to PostGIS"
	exec_psql_file eez/eez.sql

	rm World_EEZ_v10_20180221.zip eez/ -Rf

	echo "SELECT AddGeometryColumn('eez', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
	echo "UPDATE eez SET centroid_geom = ST_Centroid(geom);" | exec_psql

	echo "CREATE INDEX eez_iso_3digit ON eez (iso_ter1);" | exec_psql

	echo "Marine Regions import complete"
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
		"UPDATE political_eez SET geom = ST_Multi(ST_Difference(ST_UNION(political_eez.geom, mru.geom), mrd.geom)) FROM mru, mrd WHERE mrgid_eez = $mr1;" | exec_psql

	echo "WITH bbox AS (SELECT ST_GeomFromEWKT('SRID=4326;$bbox') AS geom)," \
		"mru AS (SELECT ST_Intersection(osm.geom, bbox.geom) AS geom FROM bbox, osm WHERE osm_id = '$osmd')," \
		"mrd AS (SELECT ST_Intersection(osm.geom, bbox.geom) AS geom FROM bbox, osm WHERE osm_id = '$osmu')" \
		"UPDATE political_eez SET geom = ST_Multi(ST_Difference(ST_UNION(political_eez.geom, mru.geom), mrd.geom)) FROM mru, mrd WHERE mrgid_eez = $mr2;" | exec_psql
}

function import_marine_regions_union() {
	echo "Downloading Marine Regions Land Union dataset"

	# EEZ + land union (we're currently on version 3, which uses EEZ version 11):
	# Download from here: http://www.marineregions.org/downloads.php

	mkdir -p /var/tmp/import
	cd /var/tmp/import
	curl -LSs --remote-name --continue-at - --fail https://download.gbif.org/MapDataMirror/2022/05/EEZ_land_union_v3_202003.zip
	mkdir -p political_eez
	unzip -oj EEZ_land_union_v3_202003.zip -d political_eez/

	shp2pgsql -d -D -s 4326 -i -I -W UTF-8 political_eez/EEZ_Land_v3_202030.shp public.political_eez | wrap_drop_geometry_commands > political_eez/political_eez.sql

	echo "Dropping old tables"
	echo "DROP TABLE IF EXISTS political_eez;" | exec_psql

	echo "Importing Marine Regions to PostGIS"
	exec_psql_file political_eez/political_eez.sql

	rm EEZ_land_union_v3_202003.zip political_eez/ -Rf

	echo "DROP TABLE IF EXISTS osm;" | exec_psql

	cd $START_DIR/osm
	for id in 13407035 192756 304751 2108121 2425963 3777250 2088990 52411 913110 1867188; do
		[[ -f $id.xml ]] || curl -Ssfo $id.xml https://www.openstreetmap.org/api/0.6/relation/$id/full
		ogr2ogr -append -f PostgreSQL "$PGCONN dbname=dev_eez" $id.xml -nln osm -lco GEOMETRY_NAME=geom multipolygons
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
		"UPDATE political_eez SET geom = osm.geom FROM osm WHERE iso_ter1 = 'BVT';" | exec_psql

	# China / Taiwan: remove Fukien Province from China and add to Taiwan
	# 8486 / 8321 / https://www.openstreetmap.org/relation/3777250
	# https://github.com/gbif/geocode/issues/2
	echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '3777250')" \
		"UPDATE political_eez SET geom = ST_Difference(political_eez.geom, osm.geom) FROM osm WHERE mrgid_eez = '8486';" | exec_psql
	echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '3777250')" \
		"UPDATE political_eez SET geom = ST_Union(political_eez.geom, osm.geom) FROM osm WHERE mrgid_eez = '8321';" | exec_psql

	# Serbia / Kosovo
	# SRB / XKX / https://www.openstreetmap.org/relation/2088990
	echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '2088990')," \
		"xkx AS (SELECT ST_Intersection(osm.geom, political_eez.geom) AS geom FROM political_eez, osm WHERE iso_ter1 = 'SRB')" \
		"INSERT INTO political_eez (\"union\", territory1, iso_ter1, iso_sov1, geom) SELECT 'Kosovo', 'Kosovo', 'XKX', 'XKX', ST_Multi(geom) FROM xkx;" | exec_psql
	echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '2088990')" \
		"UPDATE political_eez SET geom = ST_Difference(political_eez.geom, osm.geom) FROM osm WHERE iso_ter1 = 'SRB';" | exec_psql

	# China / Hong Kong
	# CHN / HKG / https://www.openstreetmap.org/relation/913110
	echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '913110')," \
		"hkg AS (SELECT ST_Intersection(osm.geom, political_eez.geom) AS geom FROM political_eez, osm WHERE iso_ter1 = 'CHN')" \
		"INSERT INTO political_eez (\"union\", territory1, iso_ter1, iso_sov1, geom) SELECT 'Hong Kong', 'Hong Kong', 'HKG', 'HKG', ST_Multi(geom) FROM hkg;" | exec_psql
	echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '913110')" \
		"UPDATE political_eez SET geom = ST_Multi(ST_Difference(political_eez.geom, osm.geom)) FROM osm WHERE iso_ter1 = 'CHN';" | exec_psql
	echo "DELETE FROM political_eez WHERE iso_ter1 = 'HKG' AND ST_NPoints(geom) = 0;" | exec_psql

	# China / Macao
	# CHN / MAC / https://www.openstreetmap.org/relation/1867188
	echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '1867188')," \
		"mac AS (SELECT ST_Intersection(osm.geom, political_eez.geom) AS geom FROM political_eez, osm WHERE iso_ter1 = 'CHN')" \
		"INSERT INTO political_eez (\"union\", territory1, iso_ter1, iso_sov1, geom) SELECT 'Macao', 'Macao', 'MAC', 'MAC', ST_Multi(geom) FROM mac;" | exec_psql
	echo "WITH osm AS (SELECT osm.geom FROM osm WHERE osm_id = '1867188')" \
		"UPDATE political_eez SET geom = ST_Multi(ST_Difference(political_eez.geom, osm.geom)) FROM osm WHERE iso_ter1 = 'CHN';" | exec_psql
	echo "DELETE FROM political_eez WHERE iso_ter1 = 'MAC' AND ST_NPoints(geom) = 0;" | exec_psql

	# Netherlands / Belgium water bit
	# NLD / BEL / https://www.openstreetmap.org/relation/52411
	echo "WITH bbox AS (SELECT ST_GeomFromEWKT('SRID=4326;POLYGON((4.15 51.008,4.15 51.408,4.439 51.408,4.439 51.008,4.15 51.008))') AS geom)," \
		"osm AS (SELECT ST_Intersection(osm.geom, bbox.geom) AS geom FROM bbox, osm WHERE osm_id = '52411')" \
		"UPDATE political_eez SET geom = ST_Union(political_eez.geom, osm.geom) FROM osm WHERE iso_ter1 = 'BEL';" | exec_psql
	echo "WITH bbox AS (SELECT ST_GeomFromEWKT('SRID=4326;POLYGON((4.15 51.008,4.15 51.408,4.439 51.408,4.439 51.008,4.15 51.008))') AS geom)," \
		"osm AS (SELECT ST_Intersection(osm.geom, bbox.geom) AS geom FROM bbox, osm WHERE osm_id = '52411')" \
		"UPDATE political_eez SET geom = ST_Multi(ST_Difference(political_eez.geom, osm.geom)) FROM osm WHERE iso_ter1 = 'NLD';" | exec_psql

	# Remove Antarctica EEZ, based on advice from VLIZ etc (emails with Tim Hirsch, 2019-02-19).
	echo "UPDATE political_eez SET geom = ST_Multi(ST_Difference(political_eez.geom, iho.geom)) FROM iho WHERE iso_ter1 = 'ATA' AND name = 'South Atlantic Ocean';" | exec_psql
	echo "UPDATE political_eez SET geom = ST_Multi(ST_Difference(political_eez.geom, iho.geom)) FROM iho WHERE iso_ter1 = 'ATA' AND name = 'Southern Ocean';" | exec_psql
	echo "UPDATE political_eez SET geom = ST_Multi(ST_Difference(political_eez.geom, iho.geom)) FROM iho WHERE iso_ter1 = 'ATA' AND name = 'South Pacific Ocean';" | exec_psql
	echo "UPDATE political_eez SET geom = ST_Multi(ST_Difference(political_eez.geom, iho.geom)) FROM iho WHERE iso_ter1 = 'ATA' AND name = 'Indian Ocean';" | exec_psql
	echo "UPDATE political_eez SET geom = ST_Multi(ST_Difference(geom, ST_MakeEnvelope(-67.0, -60.1, -35.0, -59.9, 4326))) WHERE iso_ter1 = 'ATA';" | exec_psql

	# Remove Overlapping Claim South China Sea, MR DB doesn't mark a sovereign claim here (only case of this).
	echo "DELETE from political_eez WHERE mrgid_eez = 49003;" | exec_psql

	# Delete the Joint Regime Areas that completely overlap an ordinary EEZ.
	echo "DELETE FROM political_eez WHERE mrgid_eez IN(48961, 21795, 48970, 48968, 50167, 48974, 48969, 48973, 48975, 48976, 48962, 48964, 48966, 48967);" | exec_psql

	# Add ISO codes where they are missing
	echo "UPDATE political_eez SET iso_ter1 = iso_sov1 WHERE iso_ter1 IS NULL;" | exec_psql
	echo "UPDATE political_eez SET iso_ter2 = iso_sov2 WHERE iso_ter2 IS NULL;" | exec_psql
	echo "UPDATE political_eez SET iso_ter3 = iso_sov3 WHERE iso_ter3 IS NULL;" | exec_psql

	# Use sovereign codes rather than territory codes for disputed territories
	echo "UPDATE political_eez SET iso_ter2 = iso_sov2 WHERE iso_ter1 IN ('FLK', 'SGS', 'MYT', 'ESH');" | exec_psql

	# France considers the Tromelin Island part of the French Southern and Antarctic Lands
	echo "UPDATE political_eez SET iso_ter1 = 'ATF' WHERE mrgid_eez = 48946;" | exec_psql
	# And the Matthew and Hunter Islands part of New Caledonia
	echo "UPDATE political_eez SET iso_ter1 = 'ATF' WHERE mrgid_eez = 48948;" | exec_psql

	echo "SELECT AddGeometryColumn('political_eez', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
	echo "UPDATE political_eez SET centroid_geom = ST_Centroid(geom);" | exec_psql

	echo "CREATE INDEX political_eez_iso_3digit ON political_eez (iso_ter1);" | exec_psql

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
	echo "UPDATE gadm SET name_0 = 'Chinese Taipei' WHERE gid_0 = 'TWN';" | exec_psql
	echo "UPDATE gadm SET name_0 = 'Falkland Islands (Malvinas)' WHERE gid_0 = 'FLK';" | exec_psql
	echo "UPDATE gadm SET gid_1 = 'UKR.11_1', name_1 = 'Kiev City', varname_1 = 'Kyiv', nl_name_1 = 'Київ', hasc_1 = 'UA.KC', type_1 = 'Independent City', engtype_1 = 'Independent City', validfr_1 = '~1955', gid_2 = 'UKR.11.1_1', name_2 = 'Darnyts''kyi', varname_2 = 'Darnytskyi', hasc_2 = 'UA.KC.DA', type_2 = 'Raion', engtype_2 = 'Raion', validfr_2 = 'Unknown' WHERE fid = 328778;" | exec_psql

	for i in \
		gid_0 name_0 varname_0 \
		gid_1 name_1 varname_1 nl_name_1 iso_1 hasc_1 cc_1 type_1 engtype_1 validfr_1 \
		gid_2 name_2 varname_2 nl_name_2 hasc_2 cc_2 type_2 engtype_2 validfr_2 \
		gid_3 name_3 varname_3 nl_name_3 hasc_3 cc_3 type_3 engtype_3 validfr_3 \
		gid_4 name_4 varname_4 cc_4 type_4 engtype_4 validfr_4 \
		gid_5 name_5 cc_5 type_5 engtype_5 \
		governedby sovereign disputedby region varregion country continent subcont
	do
		echo "Setting '' to NULL for $i column"
		echo "UPDATE gadm SET $i = NULL WHERE $i = '';" | exec_psql
	done

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
	echo "INSERT INTO continent_step1 (continent, gid_0, geom) SELECT 'ANTARCTICA', 'ANT-89-90', ST_SetSRID(ST_MakeBox2D(ST_Point(-180, -90), ST_Point(180, -88)), 4326);" | exec_psql

	echo "Deleting empty geometries"
	echo "DELETE FROM continent_step1 WHERE ST_IsEmpty(geom) OR ST_Area(geom) = 0;" | exec_psql

	echo "Calculating union of continent parts"
	# https://gis.stackexchange.com/questions/431664/deleting-small-holes-in-polygons-specifying-the-size-with-postgis
	echo "
		CREATE OR REPLACE FUNCTION ST_RemoveHolesInPolygonsByArea(geom GEOMETRY, area real)
		RETURNS GEOMETRY AS
		$BODY$
		WITH tbla AS (SELECT ST_Dump(geom))
        SELECT ST_Collect(ARRAY(SELECT ST_MakePolygon(ST_ExteriorRing(geom),
               ARRAY(SELECT ST_ExteriorRing(rings.geom) FROM ST_DumpRings(geom) AS rings
               WHERE rings.path[1]>0 AND ST_Area(rings.geom)>=area))
               FROM ST_Dump(geom))) AS geom FROM tbla;
		$BODY$
		LANGUAGE SQL;" | exec_psql
	echo "DROP TABLE IF EXISTS continent;" | exec_psql
	echo "CREATE TABLE continent AS SELECT continent, ST_RemoveHolesInPolygonsByArea(ST_Union(geom), 0.0001) AS geom FROM continent_step1 GROUP BY continent;" | exec_psql

	echo "Creating index"
	echo "CREATE INDEX continent_geom_geom_idx ON continent USING GIST (geom);" | exec_psql
	echo "SELECT AddGeometryColumn('continent', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
	echo "UPDATE continent SET centroid_geom = ST_Centroid(geom);" | exec_psql

	echo "Continents import complete"
	echo
}

function align_natural_earth() {
	# Move Crimea to Ukraine
	# 1. Add Crimea to Ukraine
	echo "UPDATE political SET geom = ST_Union(geom, (SELECT geom FROM political_map_subunits WHERE subunit = 'Crimea')) WHERE sovereignt = 'Ukraine';" | exec_psql;
	# 2. Assemble a Russia without Crimea from the three other parts
	echo "UPDATE political SET geom =                (SELECT geom FROM political_map_subunits WHERE gid =  52)  WHERE sovereignt = 'Russia';" | exec_psql;
	echo "UPDATE political SET geom = ST_Union(geom, (SELECT geom FROM political_map_subunits WHERE gid =  57)) WHERE sovereignt = 'Russia';" | exec_psql;
	echo "UPDATE political SET geom = ST_Union(geom, (SELECT geom FROM political_map_subunits WHERE gid = 156)) WHERE sovereignt = 'Russia';" | exec_psql;

	# Import Svalbard, Jan Mayen and Bouvet Island, which are missing from a supposedly-with-territories Norway.
	#
	# Also import Australian territories as pieces, otherwise Christmas Island is joined to the Cocos & Keeling Islands without either having an ISO code.
	#
	# And the Netherlands, for Aruba (AW), Curaçao (CW), Bonaire, Sint Eustatius, and Saba (BQ), Sint Maarten (SX).
	#
	# Import France, so the overseas territories retain their ISO codes.
	echo "DROP TABLE IF EXISTS splitup;" | exec_psql
	echo "DELETE FROM political WHERE sovereignt IN('Norway', 'Australia', 'Netherlands', 'France', 'New Zealand');" | exec_psql
	echo "CREATE TABLE splitup AS SELECT * FROM political_map_units WHERE sovereignt IN('Norway', 'Australia', 'Netherlands', 'France', 'New Zealand');" | exec_psql
	echo "UPDATE splitup SET gid = gid + (SELECT MAX(gid) FROM political);" | exec_psql
	echo "UPDATE splitup SET iso_a2 = 'SJ', iso_a3 = 'SJM' WHERE geounit = 'Jan Mayen';" | exec_psql
	echo "UPDATE splitup SET iso_a2 = 'NL', iso_a3 = 'NLD' WHERE geounit = 'Netherlands';" | exec_psql
	echo "UPDATE splitup SET iso_a2 = 'FR', iso_a3 = 'FRA' WHERE geounit = 'Clipperton Island';" | exec_psql
	echo "SELECT sovereignt, admin, geounit, iso_a2 FROM splitup ORDER BY sovereignt, admin, geounit;" | exec_psql
	echo "ALTER TABLE political ALTER COLUMN featurecla TYPE CHARACTER VARYING(16);" | exec_psql
	echo "INSERT INTO political (SELECT * FROM splitup);" | exec_psql
	echo "DROP TABLE splitup;" | exec_psql

	# Change Guantanamo, Northern Cyprus, Baikonur, Somaliland, Kosovo:
	# SELECT * FROM political WHERE adm0_a3 IN('USG','CNM','CYN','ESB','WSB','FRA','KAB','SAH','SOL');
	echo "UPDATE political SET iso_a2 = 'CU', name = 'Cuba' WHERE adm0_a3 = 'USG';" | exec_psql
	echo "UPDATE political SET iso_a2 = 'CY', name = 'Cyprus' WHERE adm0_a3 IN('CNM','CYN','ESB','WSB');" | exec_psql
	echo "UPDATE political SET iso_a2 = 'KZ', name = 'Kazakhstan' WHERE adm0_a3 = 'KAB';" | exec_psql
	echo "UPDATE political SET iso_a2 = 'SO', name = 'Somalia' WHERE adm0_a3 = 'SOL';" | exec_psql
	echo "UPDATE political SET iso_a2 = 'ZZ' WHERE iso_a2 = '-99';" | exec_psql

	echo "DROP TABLE IF EXISTS iso_map;" | exec_psql
	echo "CREATE TABLE iso_map AS (SELECT iso_a2 AS iso2, iso_a3 AS iso3, STRING_AGG(name, ',') FROM political WHERE iso_a3 != '-99' GROUP BY iso_a2, iso_a3);" | exec_psql
}

function align_marine_regions() {
	# Ascension Island no longer has its own code
	echo "UPDATE eez SET iso_ter1 = 'SHN' WHERE iso_ter1 = 'ASC';" | exec_psql
	# Clipperton Island doesn't have its own code
	echo "UPDATE eez SET iso_ter1 = 'FRA' WHERE iso_ter1 = 'CPT';" | exec_psql
	# Tristan da Cunha no longer has its own code
	echo "UPDATE eez SET iso_ter1 = 'SHN' WHERE iso_ter1 = 'TAA';" | exec_psql
	# Consistent with land in Natural Earth, except for a tiny sliver
	# TODO: Is this reasonable, or should we change the Natural Earth data instead?
	#       It's not clear.  Leaving as is for the moment, although note that ISO-3166
	#       defines region codes MA-10, MA-11 and MA-12, provinces in Western Sahara.
	#echo "UPDATE eez SET iso_ter1 = 'MAR' WHERE iso_ter1 = 'ESH';" | exec_psql

	# Remove Antarctica EEZ, based on advice from VLIZ etc.
	echo "DELETE FROM eez WHERE iso_ter1 = 'ATA';" | exec_psql

	# Remove Overlapping Claim South China Sea, MR DB doesn't mark a sovereign claim here (only case of this).
	echo "DELETE from eez WHERE mrgid = 49003;" | exec_psql

	# Delete the Joint Regime Areas that completely overlap an ordinary EEZ.
	echo "DELETE FROM eez WHERE mrgid IN(48961, 21795, 48970, 48968, 50167, 48974, 48969, 48973, 48975, 48976, 48962, 48964, 48966, 48967);" | exec_psql

	# France considers the Glorioso Islands part of the French Southern and Antarctic Lands
	# http://www.outre-mer.gouv.fr/terres-australes-et-antarctiques-francaises-les-taaf
	echo "UPDATE eez SET iso_ter2 = 'ATF' WHERE geoname = 'Overlapping claim Glorioso Islands: France / Madagascar';" | exec_psql

	# Other observations:
	# - MRGID 50167 has the name "Joint regime area Croatia / Slovenia", but only Croatia is in the record, "ter2" is empty.
	#   http://www.marineregions.org/eezdetails.php?mrgid=50167&zone=eez
	#   In any case, it was deleted above.

	# - MRGID 5692 has the name "Slovenian Exclusive Economic Zone", but has a duplicate "ter2" of Slovenia.
	#   http://www.marineregions.org/eezdetails.php?mrgid=5692&zone=eez
	#   This doesn't matter for our processing.
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


if [[ -e import-complete ]]; then
	echo "Data already imported"
else
	echo "Importing data"
	create_cache
	import_centroids
	import_natural_earth
	align_natural_earth
	import_marine_regions
	align_marine_regions
	import_iho
	import_marine_regions_union
	import_gadm
	import_wgsrpd
	import_continents
	create_combined_function
	touch import-complete
fi
