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
