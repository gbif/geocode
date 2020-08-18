#geocode-ws-client

Java client to access the RESTful services defined in the geocode-ws module.
It is an [OpenFeign](https://github.com/OpenFeign/feign) client.

##How to build this project
Execute the Maven command:

```
mvn clean package install
```

## How to create a client

### Read-only mode

Example:

```java
// set this to the web service URL.  It might be localhost:8080 for local development
String wsUrl = "http://api.gbif.org/v1/";
ClientBuilder clientBuilder = new ClientBuilder();
clientBuilder.withUrl(wsUrl);
GeocodeService geocodeClient = clientBuilder.build(GeocodeWsClient.class);
```

Make sure you are using right property `wsUrl` and includes the port.
