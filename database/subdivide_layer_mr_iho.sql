-- MR IHO
DROP TABLE IF EXISTS mr_iho_subdivided;
CREATE TABLE mr_iho_subdivided AS
    SELECT *
    FROM mr_iho
    WHERE ST_NPoints(geom) <= 1024;

INSERT INTO mr_iho_subdivided
    SELECT gid, name, id, longitude, latitude, min_x, min_y, max_x, max_y, area, mrgid, ST_Multi(ST_Subdivide(ST_MakeValid(geom), 1024)) AS geom
    FROM mr_iho
    WHERE ST_NPoints(geom) > 1024 AND ST_NPoints(geom) <= 10000;

DO
$do$
DECLARE
   k   record;
BEGIN
   FOR k IN
      SELECT mrgid, name FROM mr_iho WHERE ST_NPoints(geom) > 10000
   LOOP
      RAISE NOTICE 'Processing % (%)', k.mrgid, k.name;
      INSERT INTO mr_iho_subdivided
        SELECT gid, name, id, longitude, latitude, min_x, min_y, max_x, max_y, area, mrgid, ST_Multi(ST_Subdivide(ST_MakeValid(geom), 1024)) AS geom
        FROM mr_iho
        WHERE mrgid = k.mrgid;
      RAISE NOTICE 'Completed % (%)', k.mrgid, k.name;
   END LOOP;
END
$do$;
