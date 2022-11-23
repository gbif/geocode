package org.gbif.geocode.ws.layers.postgis;

import org.gbif.geocode.ws.layers.Bitmap;
import org.gbif.geocode.ws.persistence.mapper.LocationMapper;

public class PGGadmLayer extends AbstractPostGISLayer {
  public PGGadmLayer(LocationMapper locationMapper) {
    super(locationMapper, Bitmap.class.getResourceAsStream("gadm3210.png"), 4);
  }

  @Override
  public String name() {
    return "PG_GADM";
  }

  @Override
  public String source() {
    return "http://gadm.org/";
  }
}
