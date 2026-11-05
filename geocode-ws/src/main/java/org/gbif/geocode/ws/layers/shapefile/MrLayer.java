package org.gbif.geocode.ws.layers.shapefile;

import org.gbif.geocode.ws.layers.Bitmap;

import au.org.ala.layers.intersect.SimpleShapeFile;

public class MrLayer extends AbstractShapefileLayer {
  public MrLayer(String root) {
    super(new SimpleShapeFile(root + "mr_subdivided", new String[]{"id", "name", "isoCountry"}),
      Bitmap.class.getResourceAsStream("mr.png"),
      100);
  }

  @Override
  public double adjustUncertainty(double uncertaintyDegrees, double latitude) {
    return uncertaintyDegrees;
  }

  @Override
  public String name() {
    return "MR";
  }

  @Override
  public String source() {
    return "https://marineregions.org/";
  }
}
