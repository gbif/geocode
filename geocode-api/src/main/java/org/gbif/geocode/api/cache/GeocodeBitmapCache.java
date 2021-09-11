package org.gbif.geocode.api.cache;

import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;

import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A cache which uses a bitmap to cache coordinate lookups.
 */
public class GeocodeBitmapCache extends AbstractBitmapCachedLayer implements GeocodeService {
  public static Logger LOG = LoggerFactory.getLogger(GeocodeBitmapCache.class);

  private final GeocodeService geocodeService;

  public GeocodeBitmapCache(GeocodeService geocodeService, InputStream bitmap) {
    super(bitmap, 500);
    this.geocodeService = geocodeService;
  }

  public GeocodeBitmapCache(InputStream bitmap) {
    this(bitmap, 500);
  }

  public GeocodeBitmapCache(InputStream bitmap, int maxLocations) {
    super(bitmap, maxLocations);
    throw new RuntimeException("geocodeService is required");
  }

  @Override
  public String name() {
    return "Service-backed cache";
  }

  @Override
  public String source() {
    return "Service-backed cache";
  }

  /**
   * Simple get candidates by point.  No cache if layers are specified.
   */
  @Override
  public List<Location> get(Double lat, Double lng, Double uncertaintyDegrees, Double uncertaintyMeters, List<String> layers) {
    if (layers == null || layers.isEmpty()) {
      // No layers, use the method below
      return get(lat, lng, uncertaintyDegrees, uncertaintyMeters);
    } else {
      // Layers, skip the cache and go straight to the database.
      return geocodeService.get(lat, lng, uncertaintyDegrees, uncertaintyMeters, layers);
    }
  }

  /**
   * Check the colour of a pixel from the map image to determine the country.
   * <br/>
   * Other than the special cases, the colours are looked up using the web service the first
   * time they are found.
   * @return Locations or null if the bitmap can't answer.
   */
  @Override
  public List<Location> get(Double lat, Double lng, Double uncertaintyDegrees, Double uncertaintyMeters) {
    List<Location> locations = null;

    // Convert uncertainty in metres to degrees, approximating the Earth as a sphere.
    if (uncertaintyMeters != null) {
      uncertaintyDegrees = uncertaintyMeters / (111_319.491 * Math.cos(Math.toRadians(lat)));
      LOG.debug("{}m uncertainty converted to {}°", uncertaintyMeters, uncertaintyDegrees);
    }

    // Check the image map for a sure location.
    if (uncertaintyDegrees == null || uncertaintyDegrees <= 0.05d) {
      locations = checkBitmap(lat, lng);
    }

    // If that doesn't help, use the database.
    if (locations == null) {
      locations = geocodeService.get(lat, lng, uncertaintyDegrees, uncertaintyMeters);
      putBitmap(lat, lng, locations);
    }

    return locations;
  }

  @Override
  public byte[] bitmap() {
    return new byte[0];
  }
}
