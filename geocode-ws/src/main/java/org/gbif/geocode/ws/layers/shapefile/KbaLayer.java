package org.gbif.geocode.ws.layers.shapefile;

import org.gbif.geocode.ws.layers.Bitmap;

import au.org.ala.layers.intersect.SimpleShapeFile;

public class KbaLayer extends AbstractShapefileLayer {
  /*
   * See PGKbaLayer for SQL query verifying maximum of 2 shapes for a location.
   */
  public KbaLayer(String root) {
    super(new SimpleShapeFile(root + "kba_subdivided", new String[]{"id", "name", "isoCountry"}),
      Bitmap.class.getResourceAsStream("kba.png"),
      2);
  }

  @Override
  public double adjustUncertainty(double uncertaintyDegrees, double latitude) {
    return uncertaintyDegrees;
  }

  @Override
  public String name() {
    return "KBA";
  }

  @Override
  public String source() {
    return "https://www.keybiodiversityareas.org/";
  }
}
