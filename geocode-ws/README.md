# geocode-ws

RESTful service that provides the reverse geocode functionality. The REST resource should be accessible at this URL: `http://{server}:{httpPort}/reverse`.

## How to build this project

No credentials are hardcoded. You can provide credentials in one of multiple ways:

For these two remember not to check your changes in:
 - Change the src/resources/mybatis.properties file
 - Change the src/resources/mybatis-config.xml file

 - Provide the connection details on the command line while running Maven:
    ```-Dgeocode-ws.url=<url> -Dgeocode-ws.username=<user> -Dgeocode-ws.password=<password>```

 - Provide the connection details in a profile in your settings.xml file:

```xml
<profile>
  <id>geocode-ws</id>
  <properties>
    <geocode-ws.url></geocode-ws.url>
    <geocode-ws.username></geocode-ws.username>
    <geocode-ws.password></geocode-ws.password>
  </properties>
</profile>
```

Then start Maven with this profile: `-Pgeocode-ws`

### Building

To build the WebService just execute Maven this way:

```mvn clean package```

And make sure to provide database credentials in one of the ways mentioned above.

## Testing

To run the WebService in a local Jetty instance run

```mvn jetty:run```

And make sure to provide database credentials in one of the ways mentioned above.

Then go to: http://localhost:8080/geocode-ws/reverse?lat=55.68&lng=12.00

Response should be:

```json
[
  {
    "title": "PA:6"
    "id": "6"
  }
]
```

## Create Database

At the moment we have two sources of data: Natural Earth and EEZ.

Natural Earth (we're currently on version 1.4.0):
Download the 1:10m Cultural Vectors, Admin 0 - Countries file from here http://www.naturalearthdata.com/downloads/10m-cultural-vectors/

```
shp2pgsql -d -D -s 4326 -i -I -W WINDOWS-1252 ne_10m_admin_0_countries.shp public.political > political.sql
psql -h <host> -U <user> -d <database> -f political.sql
```

EEZ (we're currently on version 6.1):
Download the Low res version from here: http://vliz.be/vmdcdata/marbound/download.php

```
shp2pgsql -d -D -s 4326 -i -I -W WINDOWS-1252 World_EEZ_v6_1_simpliefiedcoastlines_20110512.shp public.eez > eez.sql
psql -h <host> -U <user> -d <database> -f political.sql
```

Add the indexes:

```sql
CREATE INDEX political_iso_a3
  ON political
  (iso_a3);

CREATE INDEX eez_iso_3digit
  ON eez
  (iso_3digit);
```

## Map image for faster lookups

There is a PNG image used to speed up queries — roughly half the world's area can be determined without referring to the database at all.

![PNG map cache](./src/main/resources/org/gbif/geocode/ws/service/impl/world.png)

See [Map Image Lookup](./MapImageLookup.md) for how the image is created.

## How to run this service

This service is based on the [gbif-microservice](https://github.com/gbif/gbif-microservice) project which means that the
jar file can be executed using the parameters described in the [gbif-microservice](https://github.com/gbif/gbif-microservice)
project; additionally, this service requires a logback.xml file in the `conf` directory located at same level where the
jar file (such directory is added to the application classpath).
