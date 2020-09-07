package org.gbif.geocode.api.model;

import java.util.List;

public class GadmRegion {

  private String id;

  private String name;

  private Integer gadmLevel;

  private List<String> variantName;

  private List<String> nonLatinName;

  private List<String> type;

  private List<String> englishType;

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
}
