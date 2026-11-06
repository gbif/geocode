-- WGSRPD
DROP TABLE IF EXISTS wgsrpd_level4_subdivided;
CREATE TABLE wgsrpd_level4_subdivided AS
    SELECT level4_cod, level_4_na, iso_code, geom
    FROM wgsrpd_level4
    WHERE ST_NPoints(geom) <= 1024;

INSERT INTO wgsrpd_level4_subdivided
    SELECT level4_cod, level_4_na, iso_code, ST_Multi(ST_Subdivide(geom, 1024)) AS geom
    FROM wgsrpd_level4
    WHERE ST_NPoints(geom) > 1024 AND ST_NPoints(geom) <= 10000;

DO
$do$
DECLARE
   k   record;
BEGIN
   FOR k IN
      SELECT gid, level4_cod FROM wgsrpd_level4 WHERE ST_NPoints(geom) > 10000
   LOOP
      RAISE NOTICE 'Processing % (%)', k.gid, k.level4_cod;
      INSERT INTO wgsrpd_level4_subdivided
        SELECT level4_cod, level_4_na, iso_code, ST_Multi(ST_Subdivide(geom, 1024)) AS geom
        FROM wgsrpd_level4
        WHERE gid = k.gid;
      RAISE NOTICE 'Completed % (%)', k.gid, k.level4_cod;
   END LOOP;
END
$do$;
