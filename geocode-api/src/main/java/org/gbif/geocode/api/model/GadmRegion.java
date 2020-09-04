package org.gbif.geocode.api.model;

public class GadmRegion {

  private final String id;

  private final String name;

  public GadmRegion(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
