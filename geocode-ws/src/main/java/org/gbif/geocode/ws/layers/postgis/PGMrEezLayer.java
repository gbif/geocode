package org.gbif.geocode.ws.layers.postgis;

import org.gbif.geocode.ws.layers.Bitmap;
import org.gbif.geocode.ws.persistence.mapper.LocationMapper;

public class PGMrEezLayer extends AbstractPostGISLayer {
  public PGMrEezLayer(LocationMapper locationMapper) {
    super(locationMapper, Bitmap.class.getResourceAsStream("mr_eez.png"), 1);
  }

  @Override
  public String name() {
    return "PG_MR_EEZ";
  }

  @Override
  public String source() {
    return "https://www.marineregions.org/sources.php#marbound";
  }
}
