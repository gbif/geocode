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
