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

function import_shp() {
	local shp_file=$1
	local table_name=$2
	shp2pgsql -s 4326 -I -g geometry "$shp_file" "$table_name" | exec_psql | hide_inserts
}

function hide_inserts() {
	grep -v "INSERT 0 1"
}

function import_centroids() {
	echo "Importing Centroids dataset"

	echo "Dropping old tables"
	# Generated (for the moment) with GeoLocateCentroids.java "test" in the geocode-ws module.
	echo "DROP TABLE IF EXISTS geolocate_centroids;" | exec_psql
	# Generated from the R script in the comment in this file.
	echo "DROP TABLE IF EXISTS coordinatecleaner_centroids;" | exec_psql

	echo "Importing Centroids to PostGIS"
	exec_psql_file $SCRIPT_DIR/geolocate_centroids.sql
	exec_psql_file $SCRIPT_DIR/coordinatecleaner_centroids.sql

	echo "Centroids import complete"
	echo
}

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

	echo "Simplifying Marine Regions EEZs"

	echo "SELECT AddGeometryColumn('eez', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
	echo "UPDATE eez SET centroid_geom = ST_Centroid(geom);" | exec_psql

	echo "CREATE INDEX eez_iso_3digit ON eez (iso_ter1);" | exec_psql

	echo "Marine Regions import complete"
	echo
}

function import_gadm() {
	echo "Downloading GADM dataset"

	# GADM, version 3.6: https://gadm.org/download_world.html

	mkdir -p /var/tmp/import
	cd /var/tmp/import
	curl -LSs --remote-name --continue-at - --fail http://download.gbif.org/MapDataMirror/2020/05/gadm36_gpkg.zip || \
		curl -LSs --remote-name --continue-at - --fail https://biogeo.ucdavis.edu/data/gadm3.6/gadm36_gpkg.zip
	mkdir -p gadm
	unzip -oj gadm36_gpkg.zip -d gadm/

	echo "Dropping old tables"
	for i in 1 2 3 4 ''; do echo "DROP TABLE IF EXISTS gadm$i;" | exec_psql; done

	echo "Importing GADM to PostGIS"
	ogr2ogr -lco GEOMETRY_NAME=geom -f PostgreSQL "$PGCONN" gadm/gadm36.gpkg

	rm gadm36_gpkg.zip gadm/ -Rf

	echo "SELECT AddGeometryColumn('gadm', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
	echo "UPDATE gadm SET centroid_geom = ST_Centroid(geom);" | exec_psql
	echo "UPDATE gadm SET name_0 = 'Chinese Taipei' WHERE gid_0 = 'TWN';" | exec_psql

	echo "Creating gadm4 table"
	echo "
		CREATE TABLE gadm4 AS SELECT
			MIN(fid) AS fid,MIN(uid) AS uid,
			gid_0,id_0,name_0,
			gid_1,id_1,name_1,varname_1,nl_name_1,hasc_1,cc_1,type_1,engtype_1,validfr_1,validto_1,remarks_1,
			gid_2,id_2,name_2,varname_2,nl_name_2,hasc_2,cc_2,type_2,engtype_2,validfr_2,validto_2,remarks_2,
			gid_3,id_3,name_3,varname_3,nl_name_3,hasc_3,cc_3,type_3,engtype_3,validfr_3,validto_3,remarks_3,
			gid_4,id_4,name_4,varname_4,                 cc_4,type_4,engtype_4,validfr_4,validto_4,remarks_4,
			ST_UNION(geom) AS geom
		FROM gadm
		GROUP BY
			gid_0,id_0,name_0,
			gid_1,id_1,name_1,varname_1,nl_name_1,hasc_1,cc_1,type_1,engtype_1,validfr_1,validto_1,remarks_1,
			gid_2,id_2,name_2,varname_2,nl_name_2,hasc_2,cc_2,type_2,engtype_2,validfr_2,validto_2,remarks_2,
			gid_3,id_3,name_3,varname_3,nl_name_3,hasc_3,cc_3,type_3,engtype_3,validfr_3,validto_3,remarks_3,
			gid_4,id_4,name_4,varname_4,                 cc_4,type_4,engtype_4,validfr_4,validto_4,remarks_4;" | exec_psql
	echo "CREATE INDEX gadm4_geom_geom_idx ON gadm4 USING GIST (geom);" | exec_psql
	echo "SELECT AddGeometryColumn('gadm4', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
	echo "UPDATE gadm4 SET centroid_geom = ST_Centroid(geom);" | exec_psql

	echo "Creating gadm3 table"
	echo "
		CREATE TABLE gadm3 AS SELECT
			MIN(fid) AS fid,MIN(uid) AS uid,
			gid_0,id_0,name_0,
			gid_1,id_1,name_1,varname_1,nl_name_1,hasc_1,cc_1,type_1,engtype_1,validfr_1,validto_1,remarks_1,
			gid_2,id_2,name_2,varname_2,nl_name_2,hasc_2,cc_2,type_2,engtype_2,validfr_2,validto_2,remarks_2,
			gid_3,id_3,name_3,varname_3,nl_name_3,hasc_3,cc_3,type_3,engtype_3,validfr_3,validto_3,remarks_3,
			ST_UNION(geom) AS geom
		FROM gadm4
		GROUP BY
			gid_0,id_0,name_0,
			gid_1,id_1,name_1,varname_1,nl_name_1,hasc_1,cc_1,type_1,engtype_1,validfr_1,validto_1,remarks_1,
			gid_2,id_2,name_2,varname_2,nl_name_2,hasc_2,cc_2,type_2,engtype_2,validfr_2,validto_2,remarks_2,
			gid_3,id_3,name_3,varname_3,nl_name_3,hasc_3,cc_3,type_3,engtype_3,validfr_3,validto_3,remarks_3;" | exec_psql
	echo "CREATE INDEX gadm3_geom_geom_idx ON gadm3 USING GIST (geom);" | exec_psql
	echo "SELECT AddGeometryColumn('gadm3', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
	echo "UPDATE gadm3 SET centroid_geom = ST_Centroid(geom);" | exec_psql

	echo "Creating gadm2 table"
	echo "
		CREATE TABLE gadm2 AS SELECT
			MIN(fid) AS fid,MIN(uid) AS uid,
			gid_0,id_0,name_0,
			gid_1,id_1,name_1,varname_1,nl_name_1,hasc_1,cc_1,type_1,engtype_1,validfr_1,validto_1,remarks_1,
			gid_2,id_2,name_2,varname_2,nl_name_2,hasc_2,cc_2,type_2,engtype_2,validfr_2,validto_2,remarks_2,
			ST_UNION(geom) AS geom
		FROM gadm3
		GROUP BY
			gid_0,id_0,name_0,
			gid_1,id_1,name_1,varname_1,nl_name_1,hasc_1,cc_1,type_1,engtype_1,validfr_1,validto_1,remarks_1,
			gid_2,id_2,name_2,varname_2,nl_name_2,hasc_2,cc_2,type_2,engtype_2,validfr_2,validto_2,remarks_2;" | exec_psql
	echo "CREATE INDEX gadm2_geom_geom_idx ON gadm2 USING GIST (geom);" | exec_psql
	echo "SELECT AddGeometryColumn('gadm2', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
	echo "UPDATE gadm2 SET centroid_geom = ST_Centroid(geom);" | exec_psql

	echo "Creating gadm1 table"
	echo "
		CREATE TABLE gadm1 AS SELECT
			MIN(fid) AS fid,MIN(uid) AS uid,
			gid_0,id_0,name_0,
			gid_1,id_1,name_1,varname_1,nl_name_1,hasc_1,cc_1,type_1,engtype_1,validfr_1,validto_1,remarks_1,
			ST_UNION(geom) AS geom
		FROM gadm2
		GROUP BY
			gid_0,id_0,name_0,
			gid_1,id_1,name_1,varname_1,nl_name_1,hasc_1,cc_1,type_1,engtype_1,validfr_1,validto_1,remarks_1;" | exec_psql
	echo "CREATE INDEX gadm1_geom_geom_idx ON gadm1 USING GIST (geom);" | exec_psql
	echo "SELECT AddGeometryColumn('gadm1', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
	echo "UPDATE gadm1 SET centroid_geom = ST_Centroid(geom);" | exec_psql

	echo "GADM import complete"
	echo
}

function import_seavox() {
	echo "Downloading Seavox dataset"

	# SeaVoX version 17: http://www.marineregions.org/downloads.php#seavox

	mkdir -p /var/tmp/import
	cd /var/tmp/import
	curl -LSs --remote-name --continue-at - --fail http://download.gbif.org/MapDataMirror/2020/05/SeaVoX_sea_areas_polygons_v17.zip
	mkdir -p seavox
	unzip -oj SeaVoX_sea_areas_polygons_v17.zip -d seavox/

	shp2pgsql -d -D -s 4326 -i -I -W UTF-8 seavox/SeaVoX_sea_areas_polygons_v17_att.shp public.seavox | wrap_drop_geometry_commands > seavox/seavox.sql

	echo "Dropping old tables"
	echo "DROP TABLE IF EXISTS seavox;" | exec_psql

	echo "Importing SeaVoX to PostGIS"
	exec_psql_file seavox/seavox.sql

	rm SeaVoX_sea_areas_polygons_v17.zip seavox/ -Rf

	echo "SELECT AddGeometryColumn('seavox', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
	echo "UPDATE seavox SET centroid_geom = ST_Centroid(geom);" | exec_psql

	echo "Seavox import complete"
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

		echo "SELECT AddGeometryColumn('wgsrpd_level$i', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
		echo "UPDATE wgsrpd_level$i SET centroid_geom = ST_Centroid(geom);" | exec_psql
	done

	echo "WGSRPD import complete"
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


if [[ -e complete ]]; then
	echo "Data already imported"
else
	echo "Importing data"
	create_cache
	import_centroids
	import_natural_earth
	align_natural_earth
	import_marine_regions
	align_marine_regions
	import_gadm
	import_iho
	import_seavox
	import_wgsrpd
	create_combined_function
	touch complete
fi
