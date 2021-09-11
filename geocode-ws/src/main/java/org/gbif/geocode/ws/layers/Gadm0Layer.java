package org.gbif.geocode.ws.layers;

import org.springframework.stereotype.Component;

@Component
public class Gadm0Layer extends AbstractShapefileLayer {
  public Gadm0Layer() {
    super(Gadm0Layer.class.getResourceAsStream("gadm0.png"));
  }

  @Override
  public String name() {
    return "GADM0";
  }

  @Override
  public String source() {
    return "http://gadm.org/";
  }
}
