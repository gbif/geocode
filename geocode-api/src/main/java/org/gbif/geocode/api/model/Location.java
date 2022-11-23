package org.gbif.geocode.api.model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 * Represents a location feature.
 */
public class Location implements Serializable, Comparable<Location> {

  private static final long serialVersionUID = -2085795867974984269L;

  /**
   * This is not well-ordered, so we keep locations in their original order
   * if the distances are identical.
   */
  public static final Comparator<Location> DISTANCE_COMPARATOR = new Comparator<Location>() {
    @Override
    public int compare(Location o1, Location o2) {
      return
        (o1 == null || o1.distance == null) ?
          (o2 == null ? 0 : 1) :
          (o2 == null) ? -1 : o1.distance.compareTo(o2.distance);
    }
  };

  /**
   * Identifier for the location feature, preferably globally unique.
   */
  private String id;

  /**
   * Type; GBIF-assigned identifier for the source.
   */
  private String type;

  /**
   * Source; URL or other reference to the source for the location feature.
   */
  private String source;

  /**
   * Title of the location feature, such as a country name, sea name or administrative area name.
   */
  private String title;

  /**
   * ISO 3166 Alpha 2 country code, if applicable.
   */
  private String isoCountryCode2Digit;

  /**
   * Distance from the location, or 0 if the query was within the boundary of the location.
   * <p>
   * This is approximate, it is not calculated with full geospatial accuracy, but can be compared
   * between locations in the same response.
   */
  private Double distance;

  private Double distanceMeters;

  public Location() {
    // For mybatis & jackson
  }

  public Location(
    String id, String type, String source, String title, String isoCountryCode2Digit, Double distance, Double distanceMeters
  ) {
    this.id = id;
    this.type = type;
    this.source = source;
    this.title = title;
    this.isoCountryCode2Digit = isoCountryCode2Digit;
    this.distance = distance;
    this.distanceMeters = distanceMeters;
  }

  public String getIsoCountryCode2Digit() {
    return isoCountryCode2Digit;
  }

  public void setIsoCountryCode2Digit(String isoCountryCode2Digit) {
    this.isoCountryCode2Digit = isoCountryCode2Digit;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public Double getDistance() {
    return distance;
  }

  public void setDistance(Double distance) {
    this.distance = distance;
  }

  public Double getDistanceMeters() {
    return distanceMeters;
  }

  public void setDistanceMeters(Double distanceMeters) {
    this.distanceMeters = distanceMeters;
  }

  public void calculateDistanceMetersFromLatitude(double latitude) {
    this.distanceMeters = 111_319.491 * distance * Math.cos(Math.toRadians(latitude));
  }

  public void calculateDistanceDegreesFromMeters(double latitude) {
    this.distance = distanceMeters / (111_319.491 * Math.cos(Math.toRadians(latitude)));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Location location = (Location) o;
    return Objects.equals(id, location.id) &&
      Objects.equals(type, location.type) &&
      Objects.equals(source, location.source) &&
      Objects.equals(title, location.title) &&
      Objects.equals(isoCountryCode2Digit, location.isoCountryCode2Digit) &&
      Objects.equals(distance, location.distance) &&
      Objects.equals(distanceMeters, location.distanceMeters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, type, source, title, isoCountryCode2Digit, distance);
  }

  @Override
  public String toString() {
    return "Location{" +
      "id='" + id + '\'' +
      ", type='" + type + '\'' +
      ", source='" + source + '\'' +
      ", title='" + title + '\'' +
      ", isoCountryCode2Digit='" + isoCountryCode2Digit + '\'' +
      ", distance=" + distance +
      ", distanceMeters=" + distanceMeters +
      '}';
  }

  @Override
  public int compareTo(Location o) {
//    int byDist = DISTANCE_COMPARATOR.compare(this, o);
//    if (byDist == 0) {
      return this.id.compareTo(o.id);
//    } else {
//      return byDist;
//    }
  }
}
