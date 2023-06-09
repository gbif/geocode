/* Using ::geography to calculate the distance in metres is *very* slow, but is included anyway.
 * (The PostGIS layers are only for verification.)
 */
DROP FUNCTION IF EXISTS query_layers(q_lng float, q_lat float, q_unc float, q_layers text[]);
CREATE OR REPLACE FUNCTION query_layers(q_lng float, q_lat float, q_unc float, q_layers text[])
RETURNS TABLE(layer text, id text, source text, title text, isoCountryCode2Digit character, distance float, distanceMeters float) AS $$
    (
      SELECT DISTINCT
        'Continent' AS type,
        continent AS id,
        'https://github.com/gbif/continents' AS source,
        continent AS title,
        NULL AS isoCountryCode2Digit,
        MIN(ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326))) AS distance,
        MIN(ST_Distance(geom::geography, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)::geography)) AS distanceMeters
      FROM continent
      WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
        AND 'Continent' = ANY(q_layers)
      GROUP BY continent
      ORDER BY distance, id
    )
  UNION ALL
    (
      /* Political: The WITH query is used to create up to three rows for polygons containing multiple
       *            overlapping claims or JRAs (joint regime areas).
       *
       *            The ordering (ordinal column) is to preserve the order of claims in the Marine Regions database.
       */
      WITH political_expanded AS (
        SELECT DISTINCT
          COALESCE(mrgid_eez, mrgid_ter1) AS mrgid,
          mrgid_ter1, mrgid_ter2, mrgid_ter3,
          "union" AS geoname,
          im1.iso2 AS iso2_ter1, im2.iso2 AS iso2_ter2, im3.iso2 AS iso2_ter3,
          ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) AS distance,
          ST_Distance(geom::geography, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)::geography) AS distanceMeters
        FROM political
        LEFT OUTER JOIN iso_map im1 ON political.iso_ter1 = im1.iso3
        LEFT OUTER JOIN iso_map im2 ON political.iso_ter2 = im2.iso3
        LEFT OUTER JOIN iso_map im3 ON political.iso_ter3 = im3.iso3
        WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
          AND 'Political' = ANY(q_layers)
      )
      SELECT
        'Political' AS type,
        id,
        'https://www.marineregions.org/' AS source,
        title,
        isoCountryCode2Digit,
        distance,
        distanceMeters
      FROM (
        SELECT
          'http://marineregions.org/mrgid/' || mrgid AS id,
          geoname AS title,
          iso2_ter1 AS isoCountryCode2Digit,
          distance,
          distanceMeters,
          1 AS ordinal
        FROM political_expanded
        WHERE iso2_ter1 IS NOT NULL
        UNION ALL
        SELECT
          'http://marineregions.org/mrgid/' || mrgid AS id,
          geoname AS title,
          iso2_ter2 AS isoCountryCode2Digit,
          distance,
          distanceMeters,
          2 AS ordinal
        FROM political_expanded
        WHERE iso2_ter2 IS NOT NULL
        UNION ALL
        SELECT
          'http://marineregions.org/mrgid/' || mrgid AS id,
          geoname AS title,
          iso2_ter3 AS isoCountryCode2Digit,
          distance,
          distanceMeters,
          3 AS ordinal
        FROM political_expanded
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
          ST_Distance(gadm3.geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) AS distance,
          ST_Distance(geom::geography, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)::geography) AS distanceMeters
        FROM gadm3 LEFT OUTER JOIN iso_map ON gadm3.gid_0 = iso_map.iso3
        WHERE ST_DWithin(gadm3.geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
      )
      SELECT
        'GADM0' AS type,
        gid_0 AS id,
        'http://gadm.org/' AS source,
        name_0 AS title,
        isoCountryCode2Digit,
        MIN(distance) AS distance,
        MIN(distanceMeters) AS distanceMeters
      FROM gadm
      WHERE 'GADM' = ANY(q_layers) AND gid_0 IS NOT NULL
      GROUP BY gid_0, name_0, isoCountryCode2Digit
      UNION ALL
      SELECT
        'GADM1' AS type,
        gid_1 AS id,
        'http://gadm.org/' AS source,
        name_1 AS title,
        isoCountryCode2Digit,
        MIN(distance) AS distance,
        MIN(distanceMeters) AS distanceMeters
      FROM gadm
      WHERE 'GADM' = ANY(q_layers) AND gid_1 IS NOT NULL
      GROUP BY gid_1, name_1, isoCountryCode2Digit
      UNION ALL
      SELECT
        'GADM2' AS type,
        gid_2 AS id,
        'http://gadm.org/' AS source,
        name_2 AS title,
        isoCountryCode2Digit,
        MIN(distance) AS distance,
        MIN(distanceMeters) AS distanceMeters
      FROM gadm
      WHERE 'GADM' = ANY(q_layers) AND gid_2 IS NOT NULL
      GROUP BY gid_2, name_2, isoCountryCode2Digit
      UNION ALL
      SELECT
        'GADM3' AS type,
        gid_3 AS id,
        'http://gadm.org/' AS source,
        name_3 AS title,
        isoCountryCode2Digit,
        MIN(distance) AS distance,
        MIN(distanceMeters) AS distanceMeters
      FROM gadm
      WHERE 'GADM' = ANY(q_layers) AND gid_3 IS NOT NULL
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
        ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) AS distance,
        ST_Distance(geom::geography, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)::geography) AS distanceMeters
      FROM iho
      WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
        AND 'IHO' = ANY(q_layers)
      ORDER BY distance, id
    )
  UNION ALL
    (
      SELECT
        'IUCN' AS type,
        id_no::text AS id,
        'https://iucnredlist.org/' AS source,
        CONCAT_WS(' ', sci_name, subspecies, subpop, island) AS title,
        NULL,
        ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) AS distance,
        ST_Distance(geom::geography, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)::geography) AS distanceMeters
      FROM iucn
      WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
        AND 'IUCN' = ANY(q_layers)
      ORDER BY distance, id
    )
  UNION ALL
    (
      SELECT
        'WDPA' AS type,
        "wdpaId"::text AS id,
        'https://www.protectedplanet.net/' AS source,
        name AS title,
        NULL,
        ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) AS distance,
        ST_Distance(geom::geography, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)::geography) AS distanceMeters
      FROM wdpa
      WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
        AND 'WDPA' = ANY(q_layers)
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
        ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) AS distance,
        ST_Distance(geom::geography, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)::geography) AS distanceMeters
      FROM wgsrpd_level4
      WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
        AND 'WGSRPD' = ANY(q_layers)
      ORDER BY distance, id
    )
  UNION ALL
    (
      SELECT
        'Centroids' AS type,
        id,
        source,
        isoCountryCode2Digit AS title,
        isoCountryCode2Digit,
        ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) AS distance,
        ST_Distance(geom::geography, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)::geography) AS distanceMeters
      FROM centroids
      WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
        AND 'Centroids' = ANY(q_layers)
      ORDER BY distance, id
    )
$$ LANGUAGE SQL IMMUTABLE;

-- Examples / tests
SELECT
  'IHO' AS type,
  'http://marineregions.org/mrgid/' || mrgid AS id,
  'http://marineregions.org/' AS source,
  name AS title,
  NULL,
  ST_Distance(geom, ST_SetSRID(ST_Point(4.02, 50.02), 4326))
FROM iho
WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(4.02, 50.02), 4326), 0.05)
ORDER BY ST_Distance(geom, ST_SetSRID(ST_Point(4.02, 50.02), 4326)) ASC;

SELECT * FROM query_layers(4.02, 50.02, 0.05, ARRAY['IHO', 'Political', 'Continent', 'GADM', 'Centroids', 'WGSRPD', 'IUCN', 'WDPA']);

SELECT * FROM query_layers(-34.2, -53.1, 0.05, ARRAY['Political']);
