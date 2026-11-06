-- Political EEZ Union
DROP TABLE IF EXISTS political_subdivided;
CREATE TABLE political_subdivided AS
    SELECT *
    FROM political
    WHERE ST_NPoints(geom) <= 1024;

INSERT INTO political_subdivided
    SELECT gid,
           "union", mrgid_eez,
           territory1, mrgid_ter1, iso_ter1, un_ter1, sovereign1, mrgid_sov1, iso_sov1, un_sov1,
           territory2, mrgid_ter2, iso_ter2, un_ter2, sovereign2, mrgid_sov2, iso_sov2, un_sov2,
           territory3, mrgid_ter3, iso_ter3, un_ter3, sovereign3, mrgid_sov3, iso_sov3, un_sov3,
           pol_type, y_1, x_1, area_km2, ST_Multi(ST_Subdivide(ST_MakeValid(geom), 1024)) AS geom
    FROM political
    WHERE ST_NPoints(geom) > 1024 AND ST_NPoints(geom) <= 10000;

DO
$do$
DECLARE
   k   record;
BEGIN
   FOR k IN
      SELECT gid, "union" AS geoname FROM political WHERE ST_NPoints(geom) > 10000
   LOOP
      RAISE NOTICE 'Processing % (%)', k.gid, k.geoname;
      INSERT INTO political_subdivided
        SELECT gid,
               "union", mrgid_eez,
               territory1, mrgid_ter1, iso_ter1, un_ter1, sovereign1, mrgid_sov1, iso_sov1, un_sov1,
               territory2, mrgid_ter2, iso_ter2, un_ter2, sovereign2, mrgid_sov2, iso_sov2, un_sov2,
               territory3, mrgid_ter3, iso_ter3, un_ter3, sovereign3, mrgid_sov3, iso_sov3, un_sov3,
               pol_type, y_1, x_1, area_km2, ST_Multi(ST_Subdivide(ST_MakeValid(geom), 1024)) AS geom
        FROM political
        WHERE gid = k.gid;
      RAISE NOTICE 'Completed % (%)', k.gid, k.geoname;
   END LOOP;
END
$do$;
