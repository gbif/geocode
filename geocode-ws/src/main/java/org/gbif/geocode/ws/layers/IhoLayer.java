package org.gbif.geocode.ws.layers;

import org.springframework.stereotype.Component;

import au.org.ala.layers.intersect.SimpleShapeFile;

@Component
public class IhoLayer extends AbstractShapefileLayer {
  public IhoLayer() {
    super(IhoLayer.class.getResourceAsStream("iho.png"));
  }

  public IhoLayer(SimpleShapeFile simpleShapeFile) {
    super(simpleShapeFile, IhoLayer.class.getResourceAsStream("iho.png"));
  }

  public IhoLayer(String root) {
    this(new SimpleShapeFile(root + "iho_subdivided", new String[]{"id", "name", "isoCountry"}));
  }

  @Override
  public String name() {
    return "IHO";
  }

  @Override
  public String source() {
    return "http://marineregions.org/";
  }
}
