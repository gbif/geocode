FROM openmaptiles/postgis:2.9

RUN apt-get -qq -y update \
    && apt-get -qq -y --no-install-recommends install \
        wget \
        unzip \
    && rm -rf /var/lib/apt/lists/*

COPY import.sh /docker-entrypoint-initdb.d/90_import_boundary_data.sh
