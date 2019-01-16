package org.gbif.geocode.api.model;

import java.util.Objects;

/**
 * composite data model of latitude, longitude and uncertainity.
 */
public class GeoCacheKey {

  private final Double lat;
  private final Double lng;
  private final Double uncertainity;

  public GeoCacheKey(Double lat, Double lng, Double uncertainity) {
    this.lat = lat;
    this.lng = lng;
    this.uncertainity = uncertainity;
  }

  public Double getLat() {
    return lat;
  }

  public Double getLng() {
    return lng;
  }

  public Double getUncertainity() {
    return uncertainity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GeoCacheKey)) return false;
    GeoCacheKey that = (GeoCacheKey) o;
    return Double.compare(that.getLat(), getLat()) == 0
           && Double.compare(that.getLng(), getLng()) == 0
           && Double.compare(that.getUncertainity(), getUncertainity()) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLat(), getLng(), getUncertainity());
  }

  @Override
  public String toString() {
    return "{" + "lat=" + lat + ", lng=" + lng + ", uncertainity=" + uncertainity + '}';
  }
}
