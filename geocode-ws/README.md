# geocode-ws

RESTful service that provides the reverse geocode functionality. The REST resource should be accessible at this URL: `http://{server}:{httpPort}/reverse`.

## How to build and run this project

The database url and credentials are required to start the application. You can provide them in different ways.

### Using maven
You can put the DB credentials in a maven profile:

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

Then start the application with maven and your profile: ` mvn -Pgeocode-ws spring-boot:run`

### Using a java command

You can create an [application.yml](src/resources/application.yml) and a [bootstrap.yml](src/resources/bootstrap.yml) files
and pass them to the application:

`java -jar geocode-ws-{your-version}-exec.jar.jar --spring.config.location=your_path/application.yml --spring.cloud.bootstrap.location=your_path/bootstrap.yml`

If you want to run the application with a java command this is the only way to pass the properties since the [application.yml](src/resources/application.yml) and a [bootstrap.yml](src/resources/bootstrap.yml)
files provided in the project are not included in the packaged jar.

### Using your IDE
For development, it might be easier to run the [GeocodeWsApplication](src/main/java/org/gbif/geocode/ws/GeocodeWsApplication.java) class
from your IDE and choose the right maven profile or override the properties in the [application.yml](src/resources/application.yml) file provided in the project.

## Testing

First, run the project in one of the ways mentioned above.

Then go to: http://localhost:8080/geocode/reverse?lat=55.68&lng=12.00

Response should be:

```json
[
  {
    "title": "PA:6",
    "id": "6"
  }
]
```

## Create Database

At the moment we have two sources of data: Natural Earth and EEZ.

See [../database/scripts/import.sh](../database/scripts/import.sh) for a script to import the database. With appropriate environment variables, it can be used against non-Docker databases.

## Map image for faster lookups

There is a PNG image used to speed up queries — after an initial query, roughly 95% of the world's area can be determined without referring to the database at all.

![PNG map cache](src/main/resources/org/gbif/geocode/ws/resource/cache-bitmap.png)

See [Map Image Lookup](./MapImageLookup.adoc) for how the image is created.

The layers GADM0, GADM1 and GADM2 are queried at the same time as GADM3.  Since GBIF usage is for GADM3, efficient caching of the lower layers isn't implemented

