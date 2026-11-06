-- MR LME
DROP TABLE IF EXISTS mr_lme_subdivided;
CREATE TABLE mr_lme_subdivided AS
    SELECT *
    FROM mr_lme
    WHERE ST_NPoints(geom) <= 1024;

INSERT INTO mr_lme_subdivided
    SELECT gid, objectid, lme_name, grouping, arctic, uslmes, shape_leng, shape_area, sum_gis_km, mrgid, minlat, minlong, maxlat, maxlong, lat, lon, lme_number, ST_Multi(ST_Subdivide(ST_MakeValid(geom), 1024)) AS geom
    FROM mr_lme
    WHERE ST_NPoints(geom) > 1024 AND ST_NPoints(geom) <= 10000;

DO
$do$
DECLARE
   k   record;
BEGIN
   FOR k IN
      SELECT mrgid, lme_name FROM mr_lme WHERE ST_NPoints(geom) > 10000
   LOOP
      RAISE NOTICE 'Processing % (%)', k.mrgid, k.lme_name;
      INSERT INTO mr_lme_subdivided
        SELECT gid, objectid, lme_name, grouping, arctic, uslmes, shape_leng, shape_area, sum_gis_km, mrgid, minlat, minlong, maxlat, maxlong, lat, lon, lme_number, ST_Multi(ST_Subdivide(ST_MakeValid(geom), 1024)) AS geom
        FROM mr_lme
        WHERE mrgid = k.mrgid;
      RAISE NOTICE 'Completed % (%)', k.mrgid, k.lme_name;
   END LOOP;
END
$do$;
