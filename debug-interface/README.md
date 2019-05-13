# Geocoder -- Debug interface

This is deployed here: https://labs.gbif.org/geocoder/

## Building

The tiles in ./tiles will need recreating if the database is updated.  Start the `postserve` docker task from the database module, then use `wget` to generate the tiles:

```
for z in 6; for x in `seq 0 127`; for y in `seq 0 63`; wget -O tiles/$z-$x-$y.pbf --tries=1 --no-clobber -nv http://localhost:3000/tiles/$z/$x/$y.pbf
for z in 2; for x in `seq 0 7`; for y in `seq 0 3`; wget -O tiles/$z-$x-$y.pbf --tries=1 --no-clobber -nv http://localhost:3000/tiles/$z/$x/$y.pbf
```
