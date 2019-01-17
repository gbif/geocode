package org.gbif.geocode.api.model;

import static java.lang.Double.doubleToLongBits;

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
    return arraysHashCode(getLat(), getLng(), getUncertainity());
  }

  // This utility implementation is to ensure that hashCode implementation remains same, across all implementations and different versions of jvm
  private int doublesHashCode(Double value) {
    long bits = doubleToLongBits(value);
    return (int) (bits ^ (bits >>> 32));
  }

  private int arraysHashCode(Double... a) {
    if (a == null) return 0;

    int result = 1;

    for (Double element : a) {
      result = 31 * result + (element == null ? 0 : doublesHashCode(element));
    }

    return result;
  }

  @Override
  public String toString() {
    return "{" + "lat=" + lat + ", lng=" + lng + ", uncertainity=" + uncertainity + '}';
  }
}
