-- Continent
DROP TABLE IF EXISTS continent_subdivided;
CREATE TABLE continent_subdivided AS
    SELECT continent, ST_Subdivide(geom, 1024) AS geom, centroid_geom
    FROM continent
    WHERE continent = 'ANTARCTICA';
TRUNCATE continent_subdivided;

DO
$do$
DECLARE
   k   record;
BEGIN
   FOR k IN
      SELECT continent FROM continent
   LOOP
      RAISE NOTICE 'Processing %', k.continent;
      INSERT INTO continent_subdivided
        SELECT continent, ST_Subdivide(geom, 1024) AS geom, centroid_geom
        FROM continent
        WHERE continent = k.continent;
      RAISE NOTICE 'Completed %', k.continent;
   END LOOP;
END
$do$;
