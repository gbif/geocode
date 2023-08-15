CREATE EXTENSION IF NOT EXISTS unaccent;

ALTER TABLE gadm3 ADD COLUMN fulltext_search_0 tsvector;
CREATE INDEX gadm3_fulltext_search_0_idx ON gadm3 USING gin(fulltext_search_0);
UPDATE gadm3 SET fulltext_search_0 =
  to_tsvector(coalesce(gid_0,'')) || to_tsvector(unaccent(coalesce(name_0,'')));

ALTER TABLE gadm3 ADD COLUMN fulltext_search_1 tsvector;
CREATE INDEX gadm3_fulltext_search_1_idx ON gadm3 USING gin(fulltext_search_1);
UPDATE gadm3 SET fulltext_search_1 =
  to_tsvector(coalesce(gid_1,'')) || to_tsvector(unaccent(coalesce(name_1,'')))
  || array_to_tsvector(string_to_array(unaccent(coalesce(varname_1, '')), '|'))
  || array_to_tsvector(string_to_array(unaccent(coalesce(nl_name_1, '')), '|'));

ALTER TABLE gadm3 ADD COLUMN fulltext_search_2 tsvector;
CREATE INDEX gadm3_fulltext_search_2_idx ON gadm3 USING gin(fulltext_search_2);
UPDATE gadm3 SET fulltext_search_2 =
  to_tsvector(coalesce(gid_2,'')) || to_tsvector(unaccent(coalesce(name_2,'')))
  || array_to_tsvector(string_to_array(unaccent(coalesce(varname_2, '')), '|'))
  || array_to_tsvector(string_to_array(unaccent(coalesce(nl_name_2, '')), '|'));

ALTER TABLE gadm3 ADD COLUMN fulltext_search_3 tsvector;
CREATE INDEX gadm3_fulltext_search_3_idx ON gadm3 USING gin(fulltext_search_3);
UPDATE gadm3 SET fulltext_search_3 =
  to_tsvector(coalesce(gid_3,'')) || to_tsvector(unaccent(coalesce(name_3,'')))
  || array_to_tsvector(string_to_array(unaccent(coalesce(varname_3, '')), '|'))
  || array_to_tsvector(string_to_array(unaccent(coalesce(nl_name_3, '')), '|'));

CREATE EXTENSION IF NOT EXISTS hstore;

-- DISTINCT ON to avoid duplicate entries
CREATE MATERIALIZED VIEW gadm_region AS
SELECT DISTINCT ON(gid_0)
  gid_0 AS gid, name_0 AS name, string_to_array(varname_0, '|') AS variant_name, NULL::text[] AS non_latin_name, ARRAY['Country or area'] AS type, ARRAY['Country or area']  AS english_type,
  0 AS gadm_level, NULL::text[] AS top_levels, NULL::hstore AS top_levels_map, NULL AS parent_gid, fulltext_search_0 AS fulltext_search
  FROM gadm3
  WHERE gid_0 IS NOT NULL
UNION ALL
SELECT DISTINCT ON(gid_1)
  gid_1 AS gid, name_1 AS name, string_to_array(varname_1, '|') AS variant_name, string_to_array(nl_name_1, '|') AS non_latin_name, string_to_array(type_1, '|') AS type, string_to_array(engtype_1, '|') AS english_type,
  1 AS gadm_level, ARRAY[gid_0] AS top_levels, hstore(gid_0, name_0) AS top_levels_map, gid_0 AS parent_gid, fulltext_search_1 AS fulltext_search
  FROM gadm3
  WHERE gid_1 IS NOT NULL
UNION ALL
SELECT DISTINCT ON(gid_2)
  gid_2 AS gid, name_2 AS name, string_to_array(varname_2, '|') AS variant_name, string_to_array(nl_name_2, '|') AS non_latin_name, string_to_array(type_2, '|') AS type, string_to_array(engtype_2, '|') AS english_type,
  2 AS gadm_level, ARRAY[gid_0, gid_1] AS top_levels, hstore(gid_0,name_0) || hstore(gid_1, name_1) AS top_levels_map, gid_1 AS parent_gid, fulltext_search_2 AS fulltext_search
  FROM gadm3
  WHERE gid_2 IS NOT NULL
UNION ALL
SELECT DISTINCT ON(gid_3)
  gid_3 AS gid, name_3 AS name, string_to_array(varname_3, '|') AS variant_name, string_to_array(nl_name_3, '|') AS non_latin_name, string_to_array(type_3, '|') AS type, string_to_array(engtype_3, '|') AS english_type,
  3 AS gadm_level, ARRAY[gid_0, gid_1, gid_2]  AS top_levels, hstore(gid_0,name_0) || hstore(gid_1, name_1) || hstore(gid_2, name_2) AS top_levels_map, gid_2 AS parent_gid, fulltext_search_3 AS fulltext_search
  FROM gadm3
  WHERE gid_3 IS NOT NULL;
