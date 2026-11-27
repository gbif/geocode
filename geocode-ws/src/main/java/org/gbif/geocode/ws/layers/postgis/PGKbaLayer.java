package org.gbif.geocode.ws.layers.postgis;

import org.gbif.geocode.ws.layers.Bitmap;
import org.gbif.geocode.ws.persistence.mapper.LocationMapper;

public class PGKbaLayer extends AbstractPostGISLayer {
  /*
   * Verify maximum of two shapes at a single location:
   * <pre>
     SELECT o, count(*), count(DISTINCT id) FROM (
       SELECT 0 AS o, sitrecid::text AS id FROM kba
       UNION ALL
       SELECT 1 AS o, 'BLACK' AS id
         FROM (
           SELECT ST_CollectionExtract(ST_Intersection(a.geom, b.geom), 3) AS overlap
           FROM kba a JOIN kba b ON a.gid < b.gid AND ST_Intersects(a.geom, b.geom) AND NOT ST_Touches(a.geom, b.geom)
         ) kba_overlaps
       WHERE NOT ST_IsEmpty(overlap)
     ) shapes GROUP BY o ORDER BY o;"
      o │ count │ count
     ───┼───────┼───────
      0 │ 16540 │ 16540
      1 │  3077 │     1
     </pre>
   */
  public PGKbaLayer(LocationMapper locationMapper) {
    super(locationMapper, Bitmap.class.getResourceAsStream("kba.png"), 2);
  }

  @Override
  public String name() {
    return "PG_KBA";
  }

  @Override
  public String source() {
    return "https://www.keybiodiversityareas.org/";
  }
}
