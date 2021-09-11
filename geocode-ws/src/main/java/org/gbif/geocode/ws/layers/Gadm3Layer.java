package org.gbif.geocode.ws.layers;

import org.springframework.stereotype.Component;

@Component
public class Gadm3Layer extends AbstractShapefileLayer {
  public Gadm3Layer() {
    super(Gadm3Layer.class.getResourceAsStream("gadm3.png"));
  }

  @Override
  public String name() {
    return "GADM3";
  }

  @Override
  public String source() {
    return "http://gadm.org/";
  }
}
