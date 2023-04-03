package org.gbif.geocode.ws.layers.postgis;

import org.gbif.geocode.ws.layers.Bitmap;
import org.gbif.geocode.ws.persistence.mapper.LocationMapper;

public class PGIucnLayer extends AbstractPostGISLayer {
  public PGIucnLayer(LocationMapper locationMapper) {
    super(locationMapper, Bitmap.class.getResourceAsStream("black.png"), 500_000);
  }

  @Override
  public String name() {
    return "PG_IUCN";
  }

  @Override
  public String source() {
    return "https://www.iucnredlist.org/";
  }
}
