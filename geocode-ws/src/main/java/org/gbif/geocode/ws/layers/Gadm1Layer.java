package org.gbif.geocode.ws.layers;

import org.springframework.stereotype.Component;

@Component
public class Gadm1Layer extends AbstractShapefileLayer {
  public Gadm1Layer() {
    super(Gadm1Layer.class.getResourceAsStream("gadm1.png"));
  }

  @Override
  public String name() {
    return "GADM1";
  }

  @Override
  public String source() {
    return "http://gadm.org/";
  }
}
