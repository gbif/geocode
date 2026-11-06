-- GADM3
DROP TABLE IF EXISTS gadm3_subdivided;
CREATE TABLE gadm3_subdivided AS
    SELECT *
    FROM gadm3
    WHERE ST_NPoints(geom) <= 1024;

INSERT INTO gadm3_subdivided
    SELECT fid, uid,
           gid_0, name_0, varname_0,
           gid_1, name_1, varname_1, nl_name_1, hasc_1, cc_1, type_1, engtype_1, validfr_1,
           gid_2, name_2, varname_2, nl_name_2, hasc_2, cc_2, type_2, engtype_2, validfr_2,
           gid_3, name_3, varname_3, nl_name_3, hasc_3, cc_3, type_3, engtype_3, validfr_3, ST_Subdivide(geom, 1024) AS geom
      FROM gadm3
     WHERE ST_NPoints(geom) > 1024 AND ST_NPoints(geom) <= 10000;

DO
$do$
DECLARE
   k   record;
BEGIN
   FOR k IN
      SELECT fid FROM gadm3 WHERE ST_NPoints(geom) > 10000
   LOOP
      RAISE NOTICE 'Processing %', k.fid;
      INSERT INTO gadm3_subdivided
        SELECT fid, uid,
               gid_0, name_0, varname_0,
               gid_1, name_1, varname_1, nl_name_1, hasc_1, cc_1, type_1, engtype_1, validfr_1,
               gid_2, name_2, varname_2, nl_name_2, hasc_2, cc_2, type_2, engtype_2, validfr_2,
               gid_3, name_3, varname_3, nl_name_3, hasc_3, cc_3, type_3, engtype_3, validfr_3, ST_Subdivide(geom, 1024) AS geom
        FROM gadm3
        WHERE fid = k.fid;
      RAISE NOTICE 'Completed %', k.fid;
   END LOOP;
END
$do$;
