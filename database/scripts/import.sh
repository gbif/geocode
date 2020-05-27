#!/bin/bash
set -o errexit
set -o pipefail
set -o nounset

readonly PGCONN="dbname=$POSTGRES_DB user=$POSTGRES_USER host=$POSTGRES_HOST password=$POSTGRES_PASSWORD port=$POSTGRES_PORT"

function exec_psql() {
	echo psql -v ON_ERROR_STOP=1 --host="$POSTGRES_HOST" --port="$POSTGRES_PORT" --dbname="$POSTGRES_DB" --username="$POSTGRES_USER"
	PGPASSWORD=$POSTGRES_PASSWORD psql -v ON_ERROR_STOP=1 --host="$POSTGRES_HOST" --port="$POSTGRES_PORT" --dbname="$POSTGRES_DB" --username="$POSTGRES_USER"
}

function exec_psql_file() {
	PGPASSWORD=$POSTGRES_PASSWORD psql -v ON_ERROR_STOP=1 --host="$POSTGRES_HOST" --port="$POSTGRES_PORT" --dbname="$POSTGRES_DB" --username="$POSTGRES_USER" -f $1
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
}

function import_marine_regions() {
	echo "Downloading Marine Regions dataset"

	# EEZ (we're currently on version 10):
	# Download the Low res version from here: http://vliz.be/vmdcdata/marbound/download.php

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
	#echo "ALTER TABLE eez RENAME TO eez_original;" | exec_psql
	echo "UPDATE eez SET geom = ST_Multi(ST_SimplifyPreserveTopology(geom, 0.0025));" | exec_psql

	echo "SELECT AddGeometryColumn('political', 'centroid_geom', 4326, 'POINT', 2);" | exec_psql
	echo "UPDATE political SET centroid_geom = ST_Centroid(geom);" | exec_psql

	echo "CREATE INDEX eez_iso_3digit ON eez (iso_ter1);" | exec_psql
	echo "CREATE INDEX eez_iso_3digit ON eez (iso_ter1);" | exec_psql
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
	echo "CREATE TABLE iso_map AS (SELECT iso_a2 AS iso2, iso_a3 AS iso3, name FROM political WHERE iso_a3 != '-99');" | exec_psql
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

import_natural_earth
align_natural_earth
import_marine_regions
align_marine_regions
create_cache
