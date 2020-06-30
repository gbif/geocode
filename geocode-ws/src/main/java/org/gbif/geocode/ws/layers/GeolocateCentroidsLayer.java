package org.gbif.geocode.ws.layers;

import com.google.inject.Singleton;
import org.gbif.geocode.ws.service.impl.MyBatisGeocoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class GeolocateCentroidsLayer extends AbstractBitmapCachedLayer {
  public static Logger LOG = LoggerFactory.getLogger(MyBatisGeocoder.class);

  public GeolocateCentroidsLayer() {
    super(GeolocateCentroidsLayer.class.getResourceAsStream("geolocate_centroids.png"));
  }

  @Override
  public String name() {
    return "GeolocateCentroids";
  }
}
