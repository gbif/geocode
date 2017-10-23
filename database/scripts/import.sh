#!/bin/bash
set -o errexit
set -o pipefail
set -o nounset

readonly PGCONN="dbname=$POSTGRES_DB user=$POSTGRES_USER host=$POSTGRES_HOST password=$POSTGRES_PASSWORD port=$POSTGRES_PORT"

function exec_psql() {
    PGPASSWORD=$POSTGRES_PASSWORD psql --host="$POSTGRES_HOST" --port="$POSTGRES_PORT" --dbname="$POSTGRES_DB" --username="$POSTGRES_USER"
}

function exec_psql_file() {
    PGPASSWORD=$POSTGRES_PASSWORD psql --host="$POSTGRES_HOST" --port="$POSTGRES_PORT" --dbname="$POSTGRES_DB" --username="$POSTGRES_USER" -f $1
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
	# This is version 3.1.0.

	mkdir -p /var/tmp/import
	cd /var/tmp/import
	wget --quiet http://download.gbif.org/2017/08/ne_10m_admin_0_countries.zip http://download.gbif.org/2017/08/ne_10m_admin_0_map_units.zip
	#wget --quiet http://www.naturalearthdata.com/http//www.naturalearthdata.com/download/10m/cultural/ne_10m_admin_0_countries.zip
	#wget --quiet http://www.naturalearthdata.com/http//www.naturalearthdata.com/download/10m/cultural/ne_10m_admin_0_map_units.zip
	mkdir -p ne
	unzip -oj ne_10m_admin_0_countries.zip -d ne/
	unzip -oj ne_10m_admin_0_map_units.zip -d ne/

	shp2pgsql -d -D -s 4326 -i -I -W UTF-8 ne/ne_10m_admin_0_countries.shp public.political > ne/political.sql
	shp2pgsql -d -D -s 4326 -i -I -W UTF-8 ne/ne_10m_admin_0_map_units.shp public.political_map_units > ne/political_map_units.sql

	echo "Dropping old tables"
	echo "DROP TABLE IF EXISTS political;" | exec_psql
	echo "DROP TABLE IF EXISTS political_map_units;" | exec_psql

    echo "Importing Natural Earth to PostGIS"
	exec_psql_file ne/political.sql
	exec_psql_file ne/political_map_units.sql

	rm ne_10m_admin_0_countries.zip ne_10m_admin_0_map_units.zip ne/ -Rf

	echo "CREATE INDEX political_iso_a3 ON political (iso_a3);" | exec_psql
}

function import_marine_regions() {
	echo "Downloading Marine Regions dataset"

	# EEZ (we're currently on version 8):
	# Download the Low res version from here: http://vliz.be/vmdcdata/marbound/download.php

	mkdir -p /var/tmp/import
    cd /var/tmp/import
	wget --quiet http://download.gbif.org/2017/08/World_EEZ_v9_20161021_LR.zip
	mkdir -p eez
	unzip -oj World_EEZ_v9_20161021_LR.zip -d eez/

	shp2pgsql -d -D -s 4326 -i -I -W UTF-8 eez/eez_lr.shp public.eez > eez/eez.sql

	echo "Dropping old tables"
	echo "DROP TABLE IF EXISTS eez;" | exec_psql

    echo "Importing Marine Regions to PostGIS"
	exec_psql_file eez/eez.sql

	rm World_EEZ_v9_20161021_LR.zip eez/ -Rf

	echo "CREATE INDEX eez_iso_3digit ON eez (iso_ter1);" | exec_psql
}

function align_natural_earth() {
	# Import Svalbard, Jan Mayen and Bouvet Island, which are missing from a supposedly-with-territories Norway.
	#
	# Also import Australian territories as pieces, otherwise Christmas Island is joined to the Cocos & Keeling Islands without either having an ISO code.
	#
	# And the Netherlands, for Aruba (AW), Curaçao (CW), Bonaire, Sint Eustatius, and Saba (BQ), Sint Maarten (SX).
	#
	# Import France, so the overseas territories retain their ISO codes.  Likewise for China, for Hong Kong and Macau.

	echo "DELETE FROM political WHERE sovereignt IN('Norway', 'Australia', 'Netherlands', 'France', 'China');" | exec_psql
	echo "CREATE TABLE splitup AS SELECT * FROM political_map_units WHERE sovereignt IN('Norway', 'Australia', 'Netherlands', 'France', 'China');" | exec_psql
	echo "UPDATE splitup SET gid = gid + (SELECT MAX(gid) FROM political);" | exec_psql
	echo "UPDATE splitup SET iso_a2 = 'SJ', iso_a3 = 'SJM' WHERE geounit = 'Jan Mayen';" | exec_psql
	echo "UPDATE splitup SET iso_a2 = 'NL', iso_a3 = 'NLD' WHERE geounit = 'Netherlands';" | exec_psql
	echo "SELECT sovereignt, admin, geounit, iso_a2 FROM splitup ORDER BY sovereignt, admin, geounit;" | exec_psql
	echo "INSERT INTO political (SELECT * FROM splitup);" | exec_psql
	echo "DROP TABLE splitup;" | exec_psql

	# Change some areas to use different ISO codes:

	# SELECT * FROM political WHERE adm0_a3 IN('USG','CNM','CYN','ESB','WSB','FRA','KAB','SAH','SOL');

	echo "UPDATE political SET iso_a2 = 'CU', name = 'Cuba' WHERE adm0_a3 = 'USG';" | exec_psql
	echo "UPDATE political SET iso_a2 = 'CY', name = 'Cyprus' WHERE adm0_a3 IN('CNM','CYN','ESB','WSB');" | exec_psql
	echo "UPDATE political SET iso_a2 = 'KZ', name = 'Kazakhstan' WHERE adm0_a3 = 'KAB';" | exec_psql
	echo "UPDATE political SET iso_a2 = 'SO', name = 'Somalia' WHERE adm0_a3 = 'SOL';" | exec_psql
	echo "UPDATE political SET iso_a2 = 'XK' WHERE adm0_a3 = 'KOS';" | exec_psql
	echo "UPDATE political SET iso_a2 = 'ZZ' WHERE iso_a2 = '-99';" | exec_psql

	echo "DROP TABLE IF EXISTS iso_map;" | exec_psql
	echo "CREATE TABLE iso_map AS (SELECT iso_a2 AS iso2, iso_a3 AS iso3, name FROM political WHERE iso_a3 != '-99');" | exec_psql
}

function align_marine_regions() {
	echo "UPDATE eez SET iso_ter1 = 'SHN' WHERE iso_ter1 = 'ASC';" | exec_psql # Ascension Island no longer has its own code
	echo "UPDATE eez SET iso_ter1 = 'SHN' WHERE iso_ter1 = 'TAA';" | exec_psql # Tristan da Cunha no longer has its own code
	echo "UPDATE eez SET iso_ter1 = 'MAR' WHERE iso_ter1 = 'ESH';" | exec_psql # Consistent with land in Natural Earth, except for a tiny sliver
}


which wget unzip || (apt update && apt install -y wget unzip)

import_natural_earth
align_natural_earth
import_marine_regions
align_marine_regions
