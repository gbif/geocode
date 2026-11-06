package org.gbif.geocode.api.cache;

import org.gbif.geocode.api.model.Location;

import java.util.List;

public interface Layer {
  /**
   * Layer name, used in API responses and logging.
   */
  String name();

  /**
   * Layer source (e.g. a URL), used in API responses.
   */
  String source();

  /**
   * Query the layer.
   */
  List<Location> query(double latitude, double longitude, double uncertaintyDegrees);
}
