package org.gbif.geocode.ws.layers;

import org.gbif.geocode.ws.service.impl.MyBatisGeocoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Gadm3Layer extends AbstractBitmapCachedLayer {
  public static Logger LOG = LoggerFactory.getLogger(MyBatisGeocoder.class);

  public Gadm3Layer() {
    super(Gadm3Layer.class.getResourceAsStream("gadm3.png"));
  }

  @Override
  public String name() {
    return "GADM3";
  }
}
