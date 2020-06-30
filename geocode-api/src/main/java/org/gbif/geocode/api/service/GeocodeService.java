package org.gbif.geocode.api.service;

import org.gbif.geocode.api.model.Location;

import java.util.Collection;
import java.util.List;

/**
 * A service for Geocoding operations.
 */
public interface GeocodeService {

  /**
   * Gets a list of possible {@link org.gbif.geocode.api.model.Location}s for coordinates.
   *
   * @return a list of Locations that fit the provided coordinates in no particular order
   */
  public Collection<Location> get(Double latitude, Double longitude, Double uncertaintyDegrees, Double uncertaintyMeters);

  /**
   * Gets a list of possible {@link org.gbif.geocode.api.model.Location}s for coordinates.
   *
   * @return a list of Locations that fit the provided coordinates in no particular order
   */
  public Collection<Location> get(Double latitude, Double longitude, Double uncertaintyDegrees, Double uncertaintyMeters, List<String> layers);

  /**
   * Gets a PNG bitmap suitable for using as a client-side cache/lookup table.
   */
  public byte[] bitmap();
}
