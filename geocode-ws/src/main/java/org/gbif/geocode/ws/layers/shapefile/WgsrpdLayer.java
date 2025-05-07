package org.gbif.geocode.ws.layers.shapefile;

import org.gbif.geocode.ws.layers.Bitmap;

import au.org.ala.layers.intersect.SimpleShapeFile;

public class WgsrpdLayer extends AbstractShapefileLayer {
  public WgsrpdLayer(String root) {
    super(new SimpleShapeFile(root + "wgsrpd_subdivided", new String[]{"id", "name", "isoCountry"}),
      Bitmap.class.getResourceAsStream("wgsrpd.png"),
      1);
  }

  @Override
  public double adjustUncertainty(double uncertaintyDegrees, double latitude) {
    return uncertaintyDegrees;
  }

  @Override
  public String name() {
    return "WGSRPD";
  }

  @Override
  public String source() {
    return "http://www.tdwg.org/standards/109";
  }
}
