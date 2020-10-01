DROP VIEW IF EXISTS centroids;
CREATE VIEW centroids AS (
  SELECT
    'GeolocateCentroid' AS type,
    gid::text AS id,
    'http://geo-locate.org/webservices/geolocatesvcv2/' AS source,
    iso_a2 AS isoCountryCode2Digit,
    geom AS geom
  FROM geolocate_centroids
) UNION ALL (
  SELECT
    'CoordinateCleanerCentroid' AS type,
    gid::text AS id,
    'https://github.com/ropensci/CoordinateCleaner' AS source,
    iso_a2 AS isoCountryCode2Digit,
    geom AS geom
  FROM coordinatecleaner_centroids
);

DROP FUNCTION IF EXISTS query_layers(q_lng float, q_lat float, q_unc float, q_layers text[]);
CREATE OR REPLACE FUNCTION query_layers(q_lng float, q_lat float, q_unc float, q_layers text[])
RETURNS TABLE(layer text, id text, source text, title text, isoCountryCode2Digit character, distance float) AS $$
    (
      SELECT
        'Political' AS type,
        iso_a2 AS id,
        'http://www.naturalearthdata.com/' AS source,
        name AS title,
        iso_a2 AS isoCountryCode2Digit,
        ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) AS distance
      FROM political
      WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
        AND 'Political' = ANY(q_layers)
      ORDER BY distance, id
    )
  UNION ALL
    (
      /* EEZ: The WITH query is used to create up to three rows for polygons containing multiple
       *      overlapping claims or JRAs (joint regime areas).
       *
       *      The ordering (ordinal column) is to preserve the order of claims in the Marine Regions database.
       */
      WITH eez_expanded AS (
        SELECT DISTINCT
          mrgid,
          mrgid_ter1, mrgid_ter2, mrgid_ter3,
          geoname,
          im1.iso2 AS iso2_ter1, im2.iso2 AS iso2_ter2, im3.iso2 AS iso2_ter3,
          ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) AS distance
        FROM eez
        LEFT OUTER JOIN iso_map im1 ON eez.iso_ter1 = im1.iso3
        LEFT OUTER JOIN iso_map im2 ON eez.iso_ter2 = im2.iso3
        LEFT OUTER JOIN iso_map im3 ON eez.iso_ter3 = im3.iso3
        WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
          AND 'EEZ' = ANY(q_layers)
      )
      SELECT DISTINCT
        'EEZ' AS type,
        id,
        'http://vliz.be/vmdcdata/marbound/' AS source,
        title,
        isoCountryCode2Digit,
        distance
      FROM (
        SELECT
          'http://marineregions.org/mrgid/' || mrgid AS id,
          geoname AS title,
          iso2_ter1 AS isoCountryCode2Digit,
          distance,
          1 AS ordinal
        FROM eez_expanded
        WHERE iso2_ter1 IS NOT NULL
        UNION ALL
        SELECT
          'http://marineregions.org/mrgid/' || mrgid AS id,
          geoname AS title,
          iso2_ter2 AS isoCountryCode2Digit,
          distance,
          2 AS ordinal
        FROM eez_expanded
        WHERE iso2_ter2 IS NOT NULL
        UNION ALL
        SELECT
          'http://marineregions.org/mrgid/' || mrgid AS id,
          geoname AS title,
          iso2_ter3 AS isoCountryCode2Digit,
          distance,
          3 AS ordinal
        FROM eez_expanded
        WHERE iso2_ter3 IS NOT NULL
        ORDER BY distance, ordinal
      ) e
    )
  UNION ALL
    (
      /* GADM: The WITH query is used to optimize the query, by querying all three layers at once. */
      WITH gadm AS (
        SELECT
          gid_0, gid_1, gid_2, gid_3,
          name_0, name_1, name_2, name_3,
          iso_map.iso2 AS isoCountryCode2Digit,
          ST_Distance(gadm3.geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) AS distance
        FROM gadm3 LEFT OUTER JOIN iso_map ON gadm3.gid_0 = iso_map.iso3
        WHERE ST_DWithin(gadm3.geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
      )
      SELECT
        'GADM0' AS type,
        gid_0 AS id,
        'http://gadm.org/' AS source,
        name_0 AS title,
        isoCountryCode2Digit,
        MIN(distance) AS distance
      FROM gadm
      WHERE 'GADM0' = ANY(q_layers) AND gid_0 IS NOT NULL
      GROUP BY gid_0, name_0, isoCountryCode2Digit
      UNION ALL
      SELECT
        'GADM1' AS type,
        gid_1 AS id,
        'http://gadm.org/' AS source,
        name_1 AS title,
        isoCountryCode2Digit,
        MIN(distance) AS distance
      FROM gadm
      WHERE 'GADM1' = ANY(q_layers) AND gid_1 IS NOT NULL
      GROUP BY gid_1, name_1, isoCountryCode2Digit
      UNION ALL
      SELECT
        'GADM2' AS type,
        gid_2 AS id,
        'http://gadm.org/' AS source,
        name_2 AS title,
        isoCountryCode2Digit,
        MIN(distance) AS distance
      FROM gadm
      WHERE 'GADM2' = ANY(q_layers) AND gid_2 IS NOT NULL
      GROUP BY gid_2, name_2, isoCountryCode2Digit
      UNION ALL
      SELECT
        'GADM3' AS type,
        gid_3 AS id,
        'http://gadm.org/' AS source,
        name_3 AS title,
        isoCountryCode2Digit,
        MIN(distance) AS distance
      FROM gadm
      WHERE 'GADM3' = ANY(q_layers) AND gid_3 IS NOT NULL
      GROUP BY gid_3, name_3, isoCountryCode2Digit
      ORDER BY distance, id
    )
  UNION ALL
    (
      SELECT
        'IHO' AS type,
        'http://marineregions.org/mrgid/' || mrgid AS id,
        'http://marineregions.org/' AS source,
        name AS title,
        NULL,
        ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) AS distance
      FROM iho
      WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
        AND 'IHO' = ANY(q_layers)
      ORDER BY distance, id
    )
  UNION ALL
    (
      SELECT
        'SeaVoX' AS type,
        skos_url AS id,
        'http://marineregions.org/' AS source,
        sub_region AS title,
        NULL,
        ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) AS distance
      FROM seavox
      WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
        AND 'SeaVoX' = ANY(q_layers)
      ORDER BY distance, id
    )
  UNION ALL
    (
      SELECT
        'WGSRPD' AS type,
        'WGSRPD:' || level4_cod AS id,
        'http://www.tdwg.org/standards/109' AS source,
        level_4_na AS title,
        iso_code,
        ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) AS distance
      FROM wgsrpd_level4
      WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
        AND 'WGSRPD' = ANY(q_layers)
      ORDER BY distance, id
    )
  UNION ALL
    (
      SELECT
        type,
        id,
        source,
        isoCountryCode2Digit AS title,
        isoCountryCode2Digit,
        ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) AS distance
      FROM centroids
      WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
        AND 'Centroids' = ANY(q_layers)
      ORDER BY distance, id
    )
$$ LANGUAGE SQL IMMUTABLE;

SELECT
  'SeaVoX' AS type,
  skos_url AS id,
  'http://marineregions.org/' AS source,
  sub_region AS title,
  NULL,
  ST_Distance(geom, ST_SetSRID(ST_Point(4.02, 50.02), 4326))
FROM seavox
WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(4.02, 50.02), 4326), 0.05)
ORDER BY ST_Distance(geom, ST_SetSRID(ST_Point(4.02, 50.02), 4326)) ASC;

SELECT *
FROM gadm3 LEFT OUTER JOIN iso_map ON gadm3.gid_0 = iso_map.iso3
WHERE ST_DWithin(gadm3.geom, ST_SetSRID(ST_Point(4.02, 50.02), 4326), 0.05)
ORDER BY ST_Distance(gadm3.geom, ST_SetSRID(ST_Point(4.02, 50.02), 4326)) ASC;

SELECT * FROM query_layers(4.02, 50.02, 0.05, ARRAY['SeaVoX', 'IHO', 'EEZ', 'Political', 'GADM1', 'GADM2', 'GADM3', 'Centroids', 'WGSRPD']);


CREATE EXTENSION unaccent;

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



CREATE MATERIALIZED VIEW gadm_region AS
SELECT DISTINCT id_0 AS id, gid_0 AS gid, name_0 AS name, NULL::text[] AS variant_name, NULL::text[] AS non_latin_name, ARRAY['Country'] AS type, ARRAY['Country']  AS english_type, 0 AS gadm_level, NULL::text[] AS top_levels, NULL AS parent_gid, fulltext_search_0 AS fulltext_search FROM gadm3
UNION ALL
SELECT DISTINCT id_1 AS id, gid_1 AS gid, name_1 AS name, string_to_array(varname_1, '|') AS variant_name, string_to_array(nl_name_1, '|') AS non_latin_name, string_to_array(type_1, '|') AS type, string_to_array(engtype_1, '|') AS english_type, 1 AS gadm_level, ARRAY[gid_0] AS top_levels, gid_0 AS parent_gid, fulltext_search_1 AS fulltext_search FROM gadm3
UNION ALL
SELECT DISTINCT id_2 AS id, gid_2 AS gid, name_2 AS name, string_to_array(varname_2, '|') AS variant_name, string_to_array(nl_name_2, '|') AS non_latin_name, string_to_array(type_2, '|') AS type, string_to_array(engtype_2, '|') AS english_type, 2 AS gadm_level, ARRAY[gid_0, gid_1] AS top_levels, gid_1 AS parent_gid, fulltext_search_2 AS fulltext_search FROM gadm3
UNION ALL
SELECT DISTINCT id_3 AS id, gid_3 AS gid, name_3 AS name, string_to_array(varname_3, '|') AS variant_name, string_to_array(nl_name_3, '|') AS non_latin_name, string_to_array(type_3, '|') AS type, string_to_array(engtype_3, '|') AS english_type, 3 AS gadm_level, ARRAY[gid_0, gid_1, gid_2]  AS top_levels, gid_2 AS parent_gid, fulltext_search_3 AS fulltext_search FROM gadm3;
