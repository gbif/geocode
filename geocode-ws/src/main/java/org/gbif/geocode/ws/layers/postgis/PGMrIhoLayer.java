package org.gbif.geocode.ws.layers.postgis;

import org.gbif.geocode.ws.layers.Bitmap;
import org.gbif.geocode.ws.persistence.mapper.LocationMapper;

public class PGMrIhoLayer extends AbstractPostGISLayer {
  public PGMrIhoLayer(LocationMapper locationMapper) {
    super(locationMapper, Bitmap.class.getResourceAsStream("mr_iho.png"), 1);
  }

  @Override
  public String name() {
    return "PG_MR_IHO";
  }

  @Override
  public String source() {
    return "https://www.marineregions.org/sources.php#iho";
  }
}
