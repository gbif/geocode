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

-- IHO
DROP TABLE IF EXISTS iho_subdivided;
CREATE TABLE iho_subdivided AS
    SELECT *
    FROM iho
    WHERE ST_NPoints(geom) <= 1024;

INSERT INTO iho_subdivided
    SELECT gid, name, id, longitude, latitude, min_x, min_y, max_x, max_y, area, mrgid, ST_Multi(ST_Subdivide(ST_MakeValid(geom), 1024)) AS geom
    FROM iho
    WHERE ST_NPoints(geom) > 1024 AND ST_NPoints(geom) <= 10000;

DO
$do$
DECLARE
   k   record;
BEGIN
   FOR k IN
      SELECT gid, name FROM iho WHERE ST_NPoints(geom) > 10000
   LOOP
      RAISE NOTICE 'Processing % (%)', k.gid, k.name;
      INSERT INTO iho_subdivided
        SELECT gid, name, id, longitude, latitude, min_x, min_y, max_x, max_y, area, mrgid, ST_Multi(ST_Subdivide(ST_MakeValid(geom), 1024)) AS geom
        FROM iho
        WHERE gid = k.gid;
      RAISE NOTICE 'Completed % (%)', k.gid, k.name;
   END LOOP;
END
$do$;

-- IUCN
DROP TABLE IF EXISTS iucn_subdivided;
CREATE TABLE iucn_subdivided AS
    SELECT gid, id_no, sci_name, presence, origin, seasonal, compiler, yrcompiled, citation, subspecies, subpop, source, island, tax_comm,
           dist_comm, generalisd, legend, kingdom, phylum, class, order_, family, genus, category, marine, terrestial, freshwater,
           geom, centroid_geom
    FROM iucn
    WHERE ST_NPoints(geom) <= 1024;

INSERT INTO iucn_subdivided
    SELECT gid, id_no, sci_name, presence, origin, seasonal, compiler, yrcompiled, citation, subspecies, subpop, source, island, tax_comm,
           dist_comm, generalisd, legend, kingdom, phylum, class, order_, family, genus, category, marine, terrestial, freshwater,
           ST_Multi(ST_Subdivide(ST_MakeValid(geom), 1024)) AS geom, centroid_geom
    FROM iucn
    WHERE ST_NPoints(geom) > 1024 AND ST_NPoints(geom) <= 10000;

DO
$do$
DECLARE
   k   record;
BEGIN
   FOR k IN
      SELECT gid, sci_name FROM iucn WHERE ST_NPoints(geom) > 10000
   LOOP
      RAISE NOTICE 'Processing % (%)', k.gid, k.sci_name;
      INSERT INTO iucn_subdivided
        SELECT gid, id_no, sci_name, presence, origin, seasonal, compiler, yrcompiled, citation, subspecies, subpop, source, island, tax_comm,
               dist_comm, generalisd, legend, kingdom, phylum, class, order_, family, genus, category, marine, terrestial, freshwater,
               ST_Multi(ST_Subdivide(ST_MakeValid(geom), 1024)) AS geom, centroid_geom
        FROM iucn
        WHERE gid = k.gid;
      RAISE NOTICE 'Completed % (%)', k.gid, k.sci_name;
   END LOOP;
END
$do$;

-- WDPA
DROP TABLE IF EXISTS wdpa_subdivided;
CREATE TABLE wdpa_subdivided AS
    SELECT "wdpaId", "wdpaParcelId", name, "iso3Code", geom
    FROM wdpa
    WHERE ST_NPoints(geom) <= 1024;

INSERT INTO wdpa_subdivided
    SELECT "wdpaId", "wdpaParcelId", name, "iso3Code", ST_Multi(ST_Subdivide(geom, 1024)) AS geom
    FROM wdpa
    WHERE ST_NPoints(geom) > 1024 AND ST_NPoints(geom) <= 10000;

DO
$do$
DECLARE
   k   record;
BEGIN
   FOR k IN
      SELECT "wdpaParcelId" AS id, name FROM wdpa WHERE ST_NPoints(geom) > 10000
   LOOP
      RAISE NOTICE 'Processing % (%)', k.id, k.name;
      INSERT INTO wdpa_subdivided
        SELECT "wdpaId", "wdpaParcelId", name, "iso3Code", ST_Multi(ST_Subdivide(geom, 1024)) AS geom
        FROM wdpa
        WHERE "wdpaParcelId" = k.id;
      RAISE NOTICE 'Completed % (%)', k.id, k.name;
   END LOOP;
END
$do$;

-- WGSPRD
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
