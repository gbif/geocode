package org.gbif.geocode.api.model;

import java.util.Collection;

/**
 * Composite data model of geo keys and locations.
 */
public class GeoInfo {

  private final GeoCacheKey key;
  private final Collection<Location> locations;

  public GeoInfo(GeoCacheKey key, Collection<Location> locations) {
    this.key = key;
    this.locations = locations;
  }

  public GeoCacheKey getKey() {
    return key;
  }

  public Collection<Location> getLocations() {
    return locations;
  }

  @Override
  public String toString() {
    return "GeoInfo{" + "key=" + key + ", locations=" + locations + '}';
  }
}
