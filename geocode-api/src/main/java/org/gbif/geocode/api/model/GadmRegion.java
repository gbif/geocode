package org.gbif.geocode.api.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GadmRegion {

  public static class Region {

    private String id;
    private String name;

    public Region(String id, String name) {
      this.id = id;
      this.name = name;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  private String id;

  private String name;

  private Integer gadmLevel;

  private List<String> variantName;

  private List<String> nonLatinName;

  private List<String> type;

  private List<String> englishType;

  private Map<String, String> higherRegions;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getGadmLevel() {
    return gadmLevel;
  }

  public void setGadmLevel(Integer gadmLevel) {
    this.gadmLevel = gadmLevel;
  }

  public List<String> getVariantName() {
    return variantName;
  }

  public void setVariantName(List<String> variantName) {
    this.variantName = variantName;
  }

  public List<String> getNonLatinName() {
    return nonLatinName;
  }

  public void setNonLatinName(List<String> nonLatinName) {
    this.nonLatinName = nonLatinName;
  }

  public List<String> getType() {
    return type;
  }

  public void setType(List<String> type) {
    this.type = type;
  }

  public List<String> getEnglishType() {
    return englishType;
  }

  public void setEnglishType(List<String> englishType) {
    this.englishType = englishType;
  }

  @JsonIgnore
  public Map<String, String> getHigherRegions() {
    return higherRegions;
  }

  public void setHigherRegions(Map<String, String> higherRegions) {
    this.higherRegions = higherRegions;
  }

  @JsonProperty("higherRegions")
  public List<Region> getHigherRegionsList() {
    if(higherRegions != null) {
      return higherRegions.entrySet().stream().map(e -> new Region(e.getKey(), e.getValue())).collect(Collectors.toList());
    }
    return null;
  }
}
