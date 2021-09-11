package org.gbif.geocode.ws.layers;

import org.springframework.stereotype.Component;

import au.org.ala.layers.intersect.SimpleShapeFile;

@Component
public class ContinentLayer extends AbstractShapefileLayer {
  public ContinentLayer() {
    super(ContinentLayer.class.getResourceAsStream("continent.png"));
  }

  public ContinentLayer(SimpleShapeFile simpleShapeFile) {
    super(simpleShapeFile, ContinentLayer.class.getResourceAsStream("continent.png"));
  }

  @Override
  public String name() {
    return "Continent";
  }

  @Override
  public String source() {
    return "https://github.com/gbif/continents";
  }
}
