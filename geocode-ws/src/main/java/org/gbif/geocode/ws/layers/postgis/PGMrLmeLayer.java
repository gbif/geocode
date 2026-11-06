package org.gbif.geocode.ws.layers.postgis;

import org.gbif.geocode.ws.layers.Bitmap;
import org.gbif.geocode.ws.persistence.mapper.LocationMapper;

public class PGMrLmeLayer extends AbstractPostGISLayer {
  public PGMrLmeLayer(LocationMapper locationMapper) {
    super(locationMapper, Bitmap.class.getResourceAsStream("mr_lme.png"), 1);
  }

  @Override
  public String name() {
    return "PG_MR_LME";
  }

  @Override
  public String source() {
    return "http://geonode.iwlearn.org/";
  }
}
