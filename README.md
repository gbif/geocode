# Geocode

Provide a reverse geocoding service, which is the process of back (reverse) coding of a point location (latitude, longitude) to a know location. This service relies on a [PostGIS](http://postgis.refractions.net/) database to recognize a coordinate in the boundaries of a political area and inside [exclusive economic zones](https://en.wikipedia.org/wiki/Exclusive_economic_zone). The service exposed in this project is not intended to be used publicly, this service is used internally in GBIF services to interpret countries boundaries from geographic coordinates. This project contains 3 modules:
  * geocode-api: contains the GeocodeService.get(lat,lon) service and the Location instance returned by it.
  * geocode-ws: RESTful API implementation of the GeocodeService.get(lat,lon) service.
    This service is accessible at the URL `http://{server}:{httpPort}/reverse`.
  * geocode-ws-client: Java client to access the RESTful service.
   
##How to build this project

Execute the Maven command:

```
mvn clean package verify install -P{geocode}
```

A Maven profile containing the following settings is required:

```
<profile>
  <id>geocode</id>
  <properties>    
    <geocode-ws.url>jdbc:postgresql://server/postgis_db</geocode-ws.url>
    <geocode-ws.username>eez</geocode-ws.username>
    <geocode-ws.password>password</geocode-ws.password>
  </properties>
</profile>
```
