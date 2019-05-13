# Development

To run the debug/development database and interface:

```
docker-compose up -d postgres
docker-compose exec postgres /scripts/import.sh
docker-compose run --publish 3000:8080 postserve
```

Then http://localhost:3000/index.html

Run a geocode-ws on port 8080 if you wish to click on the map to use the service.

(This is very rough-and-ready.  Based on components of OpenMapTiles, see https://openmaptiles.org/.)

# Updating the database

Once the `import.sh` script is ready, you can update the GBIF databases:

```
source .env
./scripts/import.sh
```

Then update the static debug interface tiles, see the other directory.
