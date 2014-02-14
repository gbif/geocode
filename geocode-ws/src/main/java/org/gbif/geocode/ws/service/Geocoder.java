package org.gbif.geocode.ws.service;

import org.gbif.geocode.api.model.Location;

import java.util.Collection;

/**
 * A simple interface to implement Geocode operations.
 * <p/>
 * Provides a way to translate coordinates in to {@link Location}s.
 */
public interface Geocoder {

  /**
   * Returns a collection of locations for the provided coordinates.
   * <p/>
   * No validation is performed.
   *
   * @param lat latitude
   * @param lng longitude
   *
   * @return collection of Locations or empty collection if no result.
   */
  Collection<Location> get(double lat, double lng);
}
