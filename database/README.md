# Geocode Webservice: Database setup, data import, shapefile export and Docker image

## Standalone database setup
```
export POSTGRES_DB=…
export POSTGRES_USER=…
export POSTGRES_PASSWORD=…
export POSTGRES_HOST=…
export POSTGRES_PORT=5432

./import.sh
```

## Shapefile export

```
./export-shapefiles.sh
```

Update these on NFS if they change. Note the dev NFS location is also used by the Jenkins build:

```
rsync -rptDv --delete layers/ ws.gbif-dev.org:/mnt/auto/maps/geocode/layers/
rsync -rptDv --delete layers/ ws.gbif.org:/mnt/auto/maps/geocode/layers/
```

## Docker build:
```
docker build --build-arg version=0.15 -t gbif/geocode-ws .
```

### Distribute to Docker Hub:
```
docker push gbif/geocode-ws
```

### Docker usage:
```
docker run --rm --publish 8080:8080 gbif/geocode-ws
sleep 5
curl 'http://127.0.0.1:8080/geocode/reverse?lat=50&lng=0'
```
