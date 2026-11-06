package org.gbif.geocode.ws.layers.shapefile;

import org.gbif.geocode.ws.layers.Bitmap;

import au.org.ala.layers.intersect.SimpleShapeFile;

public class MrLmeLayer extends AbstractShapefileLayer {
  public MrLmeLayer(String root) {
    super(new SimpleShapeFile(root + "mr_lme_subdivided", new String[]{"id", "name", "isoCountry"}),
      Bitmap.class.getResourceAsStream("mr_lme.png"),
      1);
  }

  @Override
  public double adjustUncertainty(double uncertaintyDegrees, double latitude) {
    return uncertaintyDegrees;
  }

  @Override
  public String name() {
    return "MR_LME";
  }

  @Override
  public String source() {
    return "http://geonode.iwlearn.org/";
  }
}
