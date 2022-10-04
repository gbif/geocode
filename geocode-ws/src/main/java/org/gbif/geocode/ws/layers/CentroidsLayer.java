package org.gbif.geocode.ws.layers;

import org.springframework.stereotype.Component;

import au.org.ala.layers.intersect.SimpleShapeFile;

@Component
public class CentroidsLayer extends AbstractShapefileLayer {
  public CentroidsLayer() {
    super(CentroidsLayer.class.getResourceAsStream("centroids.png"));
  }

  public CentroidsLayer(SimpleShapeFile simpleShapeFile) {
    super(simpleShapeFile, CentroidsLayer.class.getResourceAsStream("centroids.png"));
  }

  public CentroidsLayer(String root) {
    this(new SimpleShapeFile(root + "centroids", new String[]{"id", "name", "isoCountry"}));
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
