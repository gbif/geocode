package org.gbif.geocode.ws.layers.combined;

import org.gbif.geocode.api.cache.Layer;
import org.gbif.geocode.api.model.Location;

import java.util.ArrayList;
import java.util.List;

public class PGMrLayer implements Layer {
  final Layer[] layers;

  public PGMrLayer(Layer... pgMrLayers) {
    this.layers = pgMrLayers;
  }

  /**
   * Query the underlying layers.
   */
  public final List<Location> query(double latitude, double longitude, double uncertaintyDegrees) {
    List<Location> found = new ArrayList<>();

    for (Layer layer : layers) {
      found.addAll(layer.query(latitude, longitude, uncertaintyDegrees));
    }

    return found;
  }

  /**
   * Layer name, used in API responses and logging.
   */
  public String name() {
    return "PG_MR";
  }

  /**
   * Layer source (e.g. a URL), used in API responses.
   */
  public String source() {
    return "https://www.marineregions.org/sources.php";
  }
}
