package org.gbif.geocode.ws.layers;

import org.gbif.geocode.ws.service.impl.MyBatisGeocoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Gadm2Layer extends AbstractBitmapCachedLayer {
  public static Logger LOG = LoggerFactory.getLogger(MyBatisGeocoder.class);

  public Gadm2Layer() {
    super(Gadm2Layer.class.getResourceAsStream("gadm2.png"));
  }

  @Override
  public String name() {
    return "GADM2";
  }
}
