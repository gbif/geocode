package org.gbif.geocode.ws.layers;

import com.google.inject.Singleton;
import org.gbif.geocode.ws.service.impl.MyBatisGeocoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class Gadm1Layer extends AbstractBitmapCachedLayer {
  public static Logger LOG = LoggerFactory.getLogger(MyBatisGeocoder.class);

  public Gadm1Layer() {
    super(Gadm1Layer.class.getResourceAsStream("gadm1.png"));
  }

  @Override
  public String name() {
    return "GADM1";
  }
}
