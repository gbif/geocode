# Geocode API

Service and model classes for the reverse geocode service.

It contains the `GeocodeService.get(latitude, longitude, uncertaintyDegrees, uncertaintyMeters, layers)` service and the Location instance returned by it.

It also contains a bitmap cache implementation for use by clients.

## How to build this project
Execute the Maven command:

```
mvn clean package install
```
