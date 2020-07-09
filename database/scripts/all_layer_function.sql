DROP FUNCTION IF EXISTS query_layers(q_lng float, q_lat float, q_unc float, q_layers text[]);
CREATE OR REPLACE FUNCTION query_layers(q_lng float, q_lat float, q_unc float, q_layers text[])
RETURNS TABLE(layer text, id text, source text, title text, isoCountryCode2Digit character, distance float) AS $$
    (SELECT
      'Political',
      iso_a2,
      'http://www.naturalearthdata.com/',
      name,
      iso_a2,
      ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326))
    FROM political
    WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
      AND 'Political' = ANY(q_layers)
    ORDER BY ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) ASC)
  UNION ALL
    (SELECT
      'EEZ' as TYPE,
      'http://marineregions.org/mrgid/' || eez.mrgid AS id,
      'http://vliz.be/vmdcdata/marbound/' as SOURCE,
      eez.geoname AS title,
      iso_map.iso2 AS isoCountryCode2Digit,
      ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326))
    FROM eez LEFT OUTER JOIN iso_map ON eez.iso_ter1 = iso_map.iso3
    WHERE ST_DWithin(eez.geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
      AND 'EEZ' = ANY(q_layers)
    ORDER BY ST_Distance(eez.geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) ASC)
  UNION ALL
    (SELECT
      'GADM' as TYPE,
      COALESCE(gid_5, gid_4, gid_3, gid_2, gid_1, gid_0) AS id,
      'http://gadm.org/' as SOURCE,
      COALESCE(name_5, name_4, name_3, name_2, name_1, name_0) AS title,
      iso_map.iso2 AS isoCountryCode2Digit,
      ST_Distance(gadm.geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326))
    FROM gadm LEFT OUTER JOIN iso_map ON gadm.gid_0 = iso_map.iso3
    WHERE ST_DWithin(gadm.geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
      AND 'GADM' = ANY(q_layers)
    ORDER BY ST_Distance(gadm.geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) ASC)
  UNION ALL
    (SELECT
      'IHO' as TYPE,
      'http://marineregions.org/mrgid/' || mrgid AS id,
      'http://marineregions.org/' as SOURCE,
      name AS title,
      NULL,
      ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326))
    FROM iho
    WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
      AND 'IHO' = ANY(q_layers)
    ORDER BY ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) ASC)
  UNION ALL
    (SELECT
      'SeaVoX' as TYPE,
      skos_url AS id,
      'http://marineregions.org/' as SOURCE,
      sub_region AS title,
      NULL,
      ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326))
    FROM seavox
    WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
      AND 'SeaVoX' = ANY(q_layers)
    ORDER BY ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) ASC)
  UNION ALL
    (SELECT
      'WGSRPD' as TYPE,
      'WGSRPD:' || level4_cod AS id,
      'http://www.tdwg.org/standards/109' as SOURCE,
      level_4_na AS title,
      iso_code,
      ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326))
    FROM wgsrpd_level4
    WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
      AND 'WGSRPD' = ANY(q_layers)
    ORDER BY ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) ASC)
  UNION ALL
    (SELECT
      'GeolocateCentroids' as TYPE,
      gid::text AS id,
      'http://geo-locate.org/webservices/geolocatesvcv2/' as SOURCE,
      NULL,
      iso_a2 AS isoCountryCode2Digit,
      ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326))
    FROM geolocate_centroids
    WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326), q_unc)
      AND 'GeolocateCentroids' = ANY(q_layers)
    ORDER BY ST_Distance(geom, ST_SetSRID(ST_Point(q_lng, q_lat), 4326)) ASC)
$$ LANGUAGE SQL IMMUTABLE;

SELECT
  'SeaVoX' as TYPE,
  skos_url AS id,
  'http://marineregions.org/' as SOURCE,
  sub_region AS title,
  NULL,
  ST_Distance(geom, ST_SetSRID(ST_Point(4.02, 50.02), 4326))
FROM seavox
WHERE ST_DWithin(geom, ST_SetSRID(ST_Point(4.02, 50.02), 4326), 0.05)
ORDER BY ST_Distance(geom, ST_SetSRID(ST_Point(4.02, 50.02), 4326)) ASC;

SELECT *
FROM gadm LEFT OUTER JOIN iso_map ON gadm.gid_0 = iso_map.iso3
WHERE ST_DWithin(gadm.geom, ST_SetSRID(ST_Point(4.02, 50.02), 4326), 0.05)
ORDER BY ST_Distance(gadm.geom, ST_SetSRID(ST_Point(4.02, 50.02), 4326)) ASC;

SELECT * FROM query_layers(4.02, 50.02, 0.05, ARRAY['SeaVoX', 'IHO', 'EEZ', 'Political']);
