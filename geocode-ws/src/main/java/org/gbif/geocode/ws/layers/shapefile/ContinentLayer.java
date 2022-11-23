package org.gbif.geocode.ws.layers.shapefile;

import org.gbif.geocode.ws.layers.Bitmap;

import au.org.ala.layers.intersect.SimpleShapeFile;

public class ContinentLayer extends AbstractShapefileLayer {
  public ContinentLayer(String root) {
    super(new SimpleShapeFile(root + "continents_subdivided", new String[]{"id", "name", "isoCountry"}),
      Bitmap.class.getResourceAsStream("continent.png"),
      1);
  }

  @Override
  public String name() {
    return "Continent";
  }

  @Override
  public String source() {
    return "https://github.com/gbif/continents";
  }
}
