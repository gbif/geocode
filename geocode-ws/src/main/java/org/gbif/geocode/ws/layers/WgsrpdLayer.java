package org.gbif.geocode.ws.layers;

import org.springframework.stereotype.Component;

import au.org.ala.layers.intersect.SimpleShapeFile;

@Component
public class WgsrpdLayer extends AbstractShapefileLayer {
  public WgsrpdLayer() {
    super(WgsrpdLayer.class.getResourceAsStream("wgsrpd.png"));
  }

  public WgsrpdLayer(SimpleShapeFile simpleShapeFile) {
    super(simpleShapeFile, WgsrpdLayer.class.getResourceAsStream("wgsrpd.png"));
  }

  public WgsrpdLayer(String root) {
    this(new SimpleShapeFile(root + "wgsrpd_subdivided", new String[]{"id", "name", "isoCountry"}));
  }

  @Override
  public String name() {
    return "WGSRPD";
  }

  @Override
  public String source() {
    return "http://www.tdwg.org/standards/109";
  }
}
