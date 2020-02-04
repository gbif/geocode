# gbif/geocode-ws (Docker image)

## Build:
```
docker build --build-arg version=0.15 -t gbif/geocode-ws .
```

## Distribute to Docker Hub:
```
docker push gbif/geocode-ws
```

## Usage:
```
docker run --rm --publish 8080:8080 gbif/geocode-ws
sleep 5
curl 'http://127.0.0.1:8080/geocode/reverse?lat=50&lng=0'
```
