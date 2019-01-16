package org.gbif.geocode.api.model;

import java.io.Serializable;
import java.util.Objects;

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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Location)) return false;
    Location location = (Location) o;
    return getId().equals(location.getId())
           && getType().equals(location.getType())
           && getSource().equals(location.getSource())
           && getTitle().equals(location.getTitle())
           && getIsoCountryCode2Digit().equals(location.getIsoCountryCode2Digit());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getType(), getSource(), getTitle(), getIsoCountryCode2Digit());
  }

  @Override
  public String toString() {
    return "Location{"
           + "id='"
           + id
           + '\''
           + ", type='"
           + type
           + '\''
           + ", source='"
           + source
           + '\''
           + ", title='"
           + title
           + '\''
           + ", isoCountryCode2Digit='"
           + isoCountryCode2Digit
           + '\''
           + '}';
  }
}
