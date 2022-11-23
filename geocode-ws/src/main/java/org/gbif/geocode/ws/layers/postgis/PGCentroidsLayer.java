package org.gbif.geocode.ws.layers.postgis;

import org.gbif.geocode.ws.layers.Bitmap;
import org.gbif.geocode.ws.persistence.mapper.LocationMapper;

public class PGCentroidsLayer extends AbstractPostGISLayer {
  public PGCentroidsLayer(LocationMapper locationMapper) {
    super(locationMapper, Bitmap.class.getResourceAsStream("centroids.png"), 1);
  }

  @Override
  public String name() {
    return "PG_Centroids";
  }

  @Override
  public String source() {
    return "http://geo-locate.org/webservices/geolocatesvcv2/ / https://github.com/ropensci/CoordinateCleaner";
  }}
