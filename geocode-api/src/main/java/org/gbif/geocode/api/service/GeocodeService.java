package org.gbif.geocode.api.service;

import org.gbif.geocode.api.model.Location;

import java.util.Collection;

/**
 * A service for Geocoding operations.
 */
public interface GeocodeService {

  /**
   * Gets a list of possible {@link org.gbif.geocode.api.model.Location}s for coordinates.
   *
   * @param latitude    to check
   * @param longitude   to check
   * @param uncertainty to use
   *
   * @return a list of Locations that fit the provided coordinates in no particular order
   */
  Collection<Location> get(Double latitude, Double longitude, Double uncertainty);

  /**
   * Gets a PNG bitmap suitable for using as a client-side cache/lookup table.
   */
  byte[] bitmap();
}
