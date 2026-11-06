-- MR EEZ
DROP TABLE IF EXISTS mr_eez_subdivided;
CREATE TABLE mr_eez_subdivided AS
    SELECT *
    FROM mr_eez
    WHERE ST_NPoints(geom) <= 1024;

INSERT INTO mr_eez_subdivided
    SELECT fid, mrgid, geoname, mrgid_ter1, pol_type, mrgid_sov1, territory1, iso_ter1, sovereign1, mrgid_ter2, mrgid_sov2, territory2, iso_ter2, sovereign2, mrgid_ter3, mrgid_sov3, territory3, iso_ter3, sovereign3, x_1, y_1, mrgid_eez,  area_km2, iso_sov1, iso_sov2, iso_sov3, un_sov1, un_sov2, un_sov3, un_ter1, un_ter2, un_ter3, ST_Multi(ST_Subdivide(ST_MakeValid(geom), 1024)) AS geom
    FROM mr_eez
    WHERE ST_NPoints(geom) > 1024 AND ST_NPoints(geom) <= 10000;

DO
$do$
DECLARE
   k   record;
BEGIN
   FOR k IN
      SELECT mrgid, geoname FROM mr_eez WHERE ST_NPoints(geom) > 10000
   LOOP
      RAISE NOTICE 'Processing % (%)', k.mrgid, k.geoname;
      INSERT INTO mr_eez_subdivided
        SELECT fid, mrgid, geoname, mrgid_ter1, pol_type, mrgid_sov1, territory1, iso_ter1, sovereign1, mrgid_ter2, mrgid_sov2, territory2, iso_ter2, sovereign2, mrgid_ter3, mrgid_sov3, territory3, iso_ter3, sovereign3, x_1, y_1, mrgid_eez,  area_km2, iso_sov1, iso_sov2, iso_sov3, un_sov1, un_sov2, un_sov3, un_ter1, un_ter2, un_ter3, ST_Multi(ST_Subdivide(ST_MakeValid(geom), 1024)) AS geom
        FROM mr_eez
        WHERE mrgid = k.mrgid;
      RAISE NOTICE 'Completed % (%)', k.mrgid, k.geoname;
   END LOOP;
END
$do$;
