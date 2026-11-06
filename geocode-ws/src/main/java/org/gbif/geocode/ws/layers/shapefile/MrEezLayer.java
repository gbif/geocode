package org.gbif.geocode.ws.layers.shapefile;

import org.gbif.geocode.ws.layers.Bitmap;

import au.org.ala.layers.intersect.SimpleShapeFile;

public class MrEezLayer extends AbstractShapefileLayer {
  public MrEezLayer(String root) {
    super(new SimpleShapeFile(root + "mr_eez_subdivided", new String[]{"id", "name", "isoCountry"}),
      Bitmap.class.getResourceAsStream("mr_eez.png"),
      1);
  }

  @Override
  public double adjustUncertainty(double uncertaintyDegrees, double latitude) {
    return uncertaintyDegrees;
  }

  @Override
  public String name() {
    return "MR_EEZ";
  }

  @Override
  public String source() {
    return "https://www.marineregions.org/sources.php#marbound";
  }
}
