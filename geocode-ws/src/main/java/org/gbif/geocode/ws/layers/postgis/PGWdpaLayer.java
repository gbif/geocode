package org.gbif.geocode.ws.layers.postgis;

import org.gbif.geocode.ws.layers.Bitmap;
import org.gbif.geocode.ws.persistence.mapper.LocationMapper;

public class PGWdpaLayer extends AbstractPostGISLayer {
  public PGWdpaLayer(LocationMapper locationMapper) {
    super(locationMapper, Bitmap.class.getResourceAsStream("wdpa.png"), 100);
  }

  @Override
  public String name() {
    return "PG_WDPA";
  }

  @Override
  public String source() {
    return "https://www.protectedplanet.net/";
  }
}
