package org.gbif.geocode.ws.layers;

import com.google.inject.Singleton;
import org.gbif.geocode.ws.service.impl.MyBatisGeocoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class GadmLayer extends AbstractBitmapCachedLayer {
  public static Logger LOG = LoggerFactory.getLogger(MyBatisGeocoder.class);

  public GadmLayer() {
    super(GadmLayer.class.getResourceAsStream("gadm.png"));
  }

  @Override
  public String name() {
    return "GADM";
  }
}
