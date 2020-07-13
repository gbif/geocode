package org.gbif.geocode.ws.layers;

import com.google.inject.Singleton;
import org.gbif.geocode.ws.service.impl.MyBatisGeocoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class Gadm0Layer extends AbstractBitmapCachedLayer {
  public static Logger LOG = LoggerFactory.getLogger(MyBatisGeocoder.class);

  public Gadm0Layer() {
    super(Gadm0Layer.class.getResourceAsStream("gadm0.png"));
  }

  @Override
  public String name() {
    return "GADM0";
  }
}
