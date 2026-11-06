package org.gbif.geocode.ws.layers.shapefile;

import org.gbif.geocode.ws.layers.Bitmap;

import au.org.ala.layers.intersect.SimpleShapeFile;

public class MrIhoLayer extends AbstractShapefileLayer {
  public MrIhoLayer(String root) {
    super(new SimpleShapeFile(root + "mr_iho_subdivided", new String[]{"id", "name", "isoCountry"}),
      Bitmap.class.getResourceAsStream("mr_iho.png"),
      1);
  }

  @Override
  public double adjustUncertainty(double uncertaintyDegrees, double latitude) {
    return uncertaintyDegrees;
  }

  @Override
  public String name() {
    return "MR_IHO";
  }

  @Override
  public String source() {
    return "https://www.marineregions.org/sources.php#iho";
  }
}
