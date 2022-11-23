package org.gbif.geocode.ws.layers.postgis;

import org.gbif.geocode.ws.layers.Bitmap;
import org.gbif.geocode.ws.persistence.mapper.LocationMapper;

public class PGContinentLayer extends AbstractPostGISLayer {
  public PGContinentLayer(LocationMapper locationMapper) {
    super(locationMapper, Bitmap.class.getResourceAsStream("continent.png"), 1);
  }

  @Override
  public String name() {
    return "PG_Continent";
  }

  @Override
  public String source() {
    return "https://github.com/gbif/continents";
  }
}
