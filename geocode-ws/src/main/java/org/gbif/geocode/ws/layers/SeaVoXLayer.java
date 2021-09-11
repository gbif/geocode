package org.gbif.geocode.ws.layers;

import org.springframework.stereotype.Component;

import au.org.ala.layers.intersect.SimpleShapeFile;

@Component
public class SeaVoXLayer extends AbstractShapefileLayer {
  public SeaVoXLayer() {
    super(SeaVoXLayer.class.getResourceAsStream("seavox.png"));
  }

  public SeaVoXLayer(SimpleShapeFile simpleShapeFile) {
    super(simpleShapeFile, SeaVoXLayer.class.getResourceAsStream("seavox.png"));
  }

  @Override
  public String name() {
    return "SeaVoX";
  }

  @Override
  public String source() {
    return "http://marineregions.org/";
  }
}
