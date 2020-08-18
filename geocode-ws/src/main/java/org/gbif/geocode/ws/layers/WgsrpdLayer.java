package org.gbif.geocode.ws.layers;

import org.gbif.geocode.ws.service.impl.MyBatisGeocoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WgsrpdLayer extends AbstractBitmapCachedLayer {
  public static Logger LOG = LoggerFactory.getLogger(MyBatisGeocoder.class);

  public WgsrpdLayer() {
    super(WgsrpdLayer.class.getResourceAsStream("wgsrpd.png"));
  }

  @Override
  public String name() {
    return "WGSRPD";
  }
}
