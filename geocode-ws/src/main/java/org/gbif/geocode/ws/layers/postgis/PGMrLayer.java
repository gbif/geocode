package org.gbif.geocode.ws.layers.postgis;

import org.gbif.geocode.ws.layers.Bitmap;
import org.gbif.geocode.ws.persistence.mapper.LocationMapper;

public class PGMrLayer extends AbstractPostGISLayer {
  public PGMrLayer(LocationMapper locationMapper) {
    super(locationMapper, Bitmap.class.getResourceAsStream("mr.png"), 1);
  }

  @Override
  public String name() {
    return "PG_MR";
  }

  @Override
  public String source() {
    return "http://marineregions.org/";
  }
}
