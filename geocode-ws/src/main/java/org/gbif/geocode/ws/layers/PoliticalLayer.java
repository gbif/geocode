package org.gbif.geocode.ws.layers;

import org.springframework.stereotype.Component;

import au.org.ala.layers.intersect.SimpleShapeFile;

@Component
public class PoliticalLayer extends AbstractShapefileLayer {
  public PoliticalLayer() {
    super(PoliticalLayer.class.getResourceAsStream("political.png"));
  }

  public PoliticalLayer(SimpleShapeFile simpleShapeFile) {
    super(simpleShapeFile, PoliticalLayer.class.getResourceAsStream("political.png"));
  }

  @Override
  public String name() {
    return "Political";
  }

  @Override
  public String source() {
    return "http://www.naturalearthdata.com/";
  }
}
