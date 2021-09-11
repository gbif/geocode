package org.gbif.geocode.ws.layers;

import org.springframework.stereotype.Component;

@Component
public class Gadm2Layer extends AbstractShapefileLayer {
  public Gadm2Layer() {
    super(Gadm2Layer.class.getResourceAsStream("gadm2.png"));
  }

  @Override
  public String name() {
    return "GADM2";
  }

  @Override
  public String source() {
    return "http://gadm.org/";
  }
}
