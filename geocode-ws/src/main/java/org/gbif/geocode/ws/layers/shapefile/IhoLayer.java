package org.gbif.geocode.ws.layers.shapefile;

import org.gbif.geocode.ws.layers.Bitmap;

import au.org.ala.layers.intersect.SimpleShapeFile;

public class IhoLayer extends AbstractShapefileLayer {
  public IhoLayer(String root) {
    super(new SimpleShapeFile(root + "iho_subdivided", new String[]{"id", "name", "isoCountry"}),
      Bitmap.class.getResourceAsStream("iho.png"),
      1);
  }

  @Override
  public double adjustUncertainty(double uncertaintyDegrees, double latitude) {
    return uncertaintyDegrees;
  }

  @Override
  public String name() {
    return "IHO";
  }

  @Override
  public String source() {
    return "http://marineregions.org/";
  }
}
