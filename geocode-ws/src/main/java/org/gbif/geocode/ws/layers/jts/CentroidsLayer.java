package org.gbif.geocode.ws.layers.jts;

import org.gbif.geocode.ws.layers.Bitmap;
import org.gbif.geocode.ws.persistence.mapper.LocationMapper;

public class CentroidsLayer extends AbstractJTSLayer {
  public CentroidsLayer(LocationMapper locationMapper) {
    super(locationMapper, Bitmap.class.getResourceAsStream("centroids.png"), 1, 5_050d);
  }

  @Override
  public String name() {
    return "Centroids";
  }

  @Override
  public String source() {
    return "http://geo-locate.org/webservices/geolocatesvcv2/ / https://github.com/ropensci/CoordinateCleaner";
  }
}
