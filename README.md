# GBIF Geocoder

Provides a reverse geocoding service, which is the process of back-coding of a point location (latitude,
longitude) to a known location. This service relies on either a [PostGIS](https://postgis.net/) database
or shapefiles to recognize a coordinate in the boundaries of a several layers, including political divisions
and subdivisions, continents and seas, etc.

The service exposed in this project is *not intended to be used publicly*, this service is used internally in GBIF
services to interpret countries boundaries from geographic coordinates.

This project contains 3 modules:
  * geocode-api: contains the GeocodeService.get(lat,lon) service and the Location instance returned by it.
  * geocode-ws: RESTful API implementation of the GeocodeService.get(lat,lon) service.
    This service is accessible at the URL `http://{server}:{httpPort}/geocode/reverse`, and a debug interface is present at
    `http://{server}:{httpPort}/geocode/debug/map.html`.
  * geocode-ws-client: Java client to access the RESTful service.

There is a supporting module:
  * database: shell scripts to construct the PostGIS database and shapefiles, optionally run using Docker

## How to build this project

Execute the Maven command:
```
mvn clean package verify install -P{geocode}
```
