package org.gbif.geocode.api.model;

import java.util.Collection;

/**
 * Composite data model of geo keys and locations.
 */
public class GeoInfo {

  private GeoCacheKey key;
  private Collection<Location> locations;

  public GeoInfo() {
  }

  public GeoInfo(GeoCacheKey key, Collection<Location> locations) {
    this.key = key;
    this.locations = locations;
  }

  public GeoCacheKey getKey() {
    return key;
  }

  public void setKey(GeoCacheKey key) {
    this.key = key;
  }

  public Collection<Location> getLocations() {
    return locations;
  }

  public void setLocations(Collection<Location> locations) {
    this.locations = locations;
  }

  @Override
  public String toString() {
    return "GeoInfo{" + "key=" + key + ", locations=" + locations + '}';
  }
}
