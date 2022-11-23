package org.gbif.geocode.ws.layers.postgis;

import org.gbif.geocode.ws.layers.Bitmap;
import org.gbif.geocode.ws.persistence.mapper.LocationMapper;

public class PGWgsrpdLayer extends AbstractPostGISLayer {
  public PGWgsrpdLayer(LocationMapper locationMapper) {
    super(locationMapper, Bitmap.class.getResourceAsStream("wgsrpd.png"), 1);
  }

  @Override
  public String name() {
    return "PG_WGSRPD";
  }

  @Override
  public String source() {
    return "http://www.tdwg.org/standards/109";
  }
}
