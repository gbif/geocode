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

### Read-write mode

This includes authentication functionality.
There are two ways: use simple user basic authentication or GBIF app authentication.

#### Using simple user basic authentication

```java
// set this to the web service URL.  It might be localhost:8080 for local development
String wsUrl = "http://api.gbif.org/v1/";
String password = "password";
String username = "username";
ClientBuilder clientBuilder = new ClientBuilder();
clientBuilder
  .withUrl(wsUrl)
  .withCredentials(username, password);
GeocodeService geocodeClient = clientBuilder.build(GeocodeWsClient.class);
```

Make sure you are using right properties `wsUrl`, `username` and `passowrd`.

#### Using GBIF app authentication

```java
// set this to the web service URL.  It might be localhost:8080 for local development
String wsUrl = "http://api.gbif.org/v1/";
String appKey = "app.key";
String secretKey = "secret-key";
String username = "username";
ClientBuilder clientBuilder = new ClientBuilder();
clientBuilder
  .withUrl(wsUrl)
  .withAppKeyCredentials(username, appKey, secretKey);
GeocodeService geocodeClient = clientBuilder.build(GeocodeWsClient.class);
```

Make sure you are using right properties `wsUrl`, `username`, `appKey` and `secretKey`.
