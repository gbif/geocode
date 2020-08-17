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
      SELECT DISTINCT
        'EEZ' AS type,
        'http://marineregions.org/mrgid/' || eez.mrgid AS id,
        'http://vliz.be/vmdcdata/marbound/' AS source,
        eez.geoname AS title,
        iso_map.iso2 AS isoCountryCode2Digit,
        ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) AS distance
      FROM eez LEFT OUTER JOIN iso_map ON eez.iso_ter1 = iso_map.iso3
      WHERE ST_DWithin(eez.geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
        AND 'EEZ' = ANY(q_layers)
      ORDER BY distance, id
    )
  UNION ALL
    (
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
        'GeolocateCentroids' AS type,
        gid::text AS id,
        'http://geo-locate.org/webservices/geolocatesvcv2/' AS source,
        NULL,
        iso_a2 AS isoCountryCode2Digit,
        ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) AS distance
      FROM geolocate_centroids
      WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
        AND 'GeolocateCentroids' = ANY(q_layers)
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

SELECT * FROM query_layers(4.02, 50.02, 0.05, ARRAY['SeaVoX', 'IHO', 'EEZ', 'Political', 'GADM2', 'GADM3']);
