package org.gbif.geocode.ws.layers.postgis;

import org.gbif.geocode.ws.layers.Bitmap;
import org.gbif.geocode.ws.persistence.mapper.LocationMapper;

public class PGIhoLayer extends AbstractPostGISLayer {
  public PGIhoLayer(LocationMapper locationMapper) {
    super(locationMapper, Bitmap.class.getResourceAsStream("iho.png"), 1);
  }

  @Override
  public String name() {
    return "PG_IHO";
  }

  @Override
  public String source() {
    return "http://marineregions.org/";
  }
}
