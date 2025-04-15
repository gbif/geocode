package org.gbif.geocode.ws.layers.jts;

import org.gbif.geocode.ws.layers.Bitmap;
import org.gbif.geocode.ws.persistence.mapper.LocationMapper;

public class CentroidsLayer extends AbstractJTSLayer {
  public CentroidsLayer(LocationMapper locationMapper) {
    super(locationMapper, Bitmap.class.getResourceAsStream("centroids.png"), 1, 5_050d);
  }

  @Override
  public double adjustUncertainty(double uncertaintyDegrees, double latitude) {
    return uncertaintyDegrees;
  }

  @Override
  public String name() {
    return "Centroids";
  }

  @Override
  public String source() {
    return "https://github.com/jhnwllr/catalogue-of-centroids";
  }
}
