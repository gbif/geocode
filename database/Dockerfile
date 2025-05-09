# gbif/geocode-ws
#
# Image contains PostgreSQL, PostGIS, and the GBIF reverse geocoding borders
# from Marine Regions, OpenStreetMap, GADM, IHO, WGSRPD.

FROM postgis/postgis:14-master

ARG version

RUN apt-get -qq -y update \
    && apt-get -qq -y --no-install-recommends install \
        curl \
        jq \
        openjdk-11-jdk-headless \
        supervisor \
        unzip \
    && rm -rf /var/lib/apt/lists/*

# Set data here, as the postgres base-image has the default /var/lib/postgres as a volume, so it's not retained.
ENV PGDATA=/data

ENV POSTGRES_DB=eez
ENV POSTGRES_USER=eez
ENV POSTGRES_HOST_AUTH_METHOD=trust

# Scripts to load the database
COPY all_layer_function.sql coordinatecleaner_centroids.sql geolocate_centroids.sql subdivide_layers.sql /docker-entrypoint-initdb.d/

# The PostgreSQL Docker startup will load these.
COPY import.sh /docker-entrypoint-initdb.d/90_import_boundary_data.sh
COPY export-shapefiles.sh /docker-entrypoint-initdb.d/91_export_shapefile_data.sh
COPY osm /osm
# This shuts down PostgreSQL and exits with an error, which is a small hack to end the following RUN.
COPY docker_exit.sh /docker-entrypoint-initdb.d/99_exit.sh

RUN /usr/local/bin/docker-entrypoint.sh postgres || \
    rm -f /docker-entrypoint-initdb.d/90_import_boundary_data.sh /docker-entrypoint-initdb.d/91_export_shapefile_data.sh /docker-entrypoint-initdb.d/99_exit.sh

#################################
# The database is now prepared. #
#################################

# Set up the webservice

RUN mkdir -p /usr/src/app/conf
WORKDIR /usr/src/app

COPY docker_application.yml /usr/src/app/application.yml
COPY docker_bootstrap.yml /usr/src/app/bootstrap.yml
COPY docker_logback.xml /usr/src/app/conf/logback.xml
COPY docker_supervisord.conf /etc/supervisord.conf

RUN echo Installing Geocode-WS version ${version}

RUN curl --fail --progress-bar --output /usr/src/app/geocode-ws.jar https://repository.gbif.org/repository/releases/org/gbif/geocode/geocode-ws/${version}/geocode-ws-"${version}".jar

CMD ["/usr/bin/supervisord"]
