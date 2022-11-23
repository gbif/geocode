package org.gbif.geocode.ws.layers.postgis;

import org.gbif.geocode.ws.layers.Bitmap;
import org.gbif.geocode.ws.persistence.mapper.LocationMapper;

public class PGPoliticalLayer extends AbstractPostGISLayer {
  public PGPoliticalLayer(LocationMapper locationMapper) {
    super(locationMapper, Bitmap.class.getResourceAsStream("political.png"), 3);
  }

  @Override
  public String name() {
    return "PG_Political";
  }

  @Override
  public String source() {
    return "https://www.marineregions.org/";
  }
}
