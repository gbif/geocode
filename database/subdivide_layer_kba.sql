-- KBA
DROP TABLE IF EXISTS kba_subdivided;
CREATE TABLE kba_subdivided AS
    SELECT *
    FROM kba
    WHERE ST_NPoints(geom) <= 1024;

INSERT INTO kba_subdivided
    SELECT gid, sitrecid, region, country, iso3, natname, intname, sitlat, sitlong, sitareakm2, kbastatus, kbaclass, ibastatus, legacykba, azestatus, lastupdate, source, repareakm2, ST_Multi(ST_Subdivide(ST_MakeValid(geom), 1024)) AS geom
    FROM kba
    WHERE ST_NPoints(geom) > 1024 AND ST_NPoints(geom) <= 10000;

DO
$do$
DECLARE
   k   record;
BEGIN
   FOR k IN
      SELECT gid, intname FROM kba WHERE ST_NPoints(geom) > 10000
   LOOP
      RAISE NOTICE 'Processing % (%)', k.gid, k.intname;
      INSERT INTO kba_subdivided
        SELECT gid, sitrecid, region, country, iso3, natname, intname, sitlat, sitlong, sitareakm2, kbastatus, kbaclass, ibastatus, legacykba, azestatus, lastupdate, source, repareakm2, ST_Multi(ST_Subdivide(ST_MakeValid(geom), 1024)) AS geom
        FROM kba
        WHERE gid = k.gid;
      RAISE NOTICE 'Completed % (%)', k.gid, k.intname;
   END LOOP;
END
$do$;
