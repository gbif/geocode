package org.gbif.geocode.api.model;

import java.io.Serializable;

/**
 * Represents a location.
 *
 * @author tim
 */
public class Location implements Serializable {

  private static final long serialVersionUID = -2085795867974984269L;

  private String id;
  private String type;
  private String source;
  private String title;
  private String isoCountryCode2Digit;

  public Location() {
    // For mybatis & jackson
  }

  public Location(String id, String type, String source, String title, String isoCountryCode2Digit) {
    this.id = id;
    this.type = type;
    this.source = source;
    this.title = title;
    this.isoCountryCode2Digit = isoCountryCode2Digit;
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

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Location)) {
      return false;
    }
    Location other = (Location) obj;

    return (id == null ? other.id == null : id.equals(other.id))
           && (isoCountryCode2Digit == null ? other.isoCountryCode2Digit == null
      : isoCountryCode2Digit.equals(other.isoCountryCode2Digit))
           && (source == null ? other.source == null : source.equals(other.source))
           && (title == null ? other.title == null : title.equals(other.title)) && (type == null ? other.type == null
      : type.equals(other.type));
  }

  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + source.hashCode();
    result = 31 * result + title.hashCode();
    result = 31 * result + (isoCountryCode2Digit != null ? isoCountryCode2Digit.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Location{" + "id='" + id + '\'' + ", type='" + type + '\'' + ", source='" + source + '\'' + ", title='"
           + title + '\'' + ", isoCountryCode2Digit='" + isoCountryCode2Digit + '\'' + '}';
  }
}
