package org.gbif.geocode.api.model;

import java.util.Objects;

import org.apache.hadoop.conf.Configuration;

/**
 * Data model for basic Hbase properties need to start hbase cache.
 */
public class HbaseProperties {

  private final Configuration configuration;
  private final String tableName;
  private final String columnFamily;
  private final int modulus;

  public HbaseProperties(Configuration configuration, String tableName, String columnFamily, int modulus) {
    this.configuration = configuration;
    this.tableName = tableName;
    this.columnFamily = columnFamily;
    this.modulus = modulus;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public String getTableName() {
    return tableName;
  }

  public String getColumnFamily() {
    return columnFamily;
  }

  public int getModulus() {
    return modulus;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof HbaseProperties)) return false;
    HbaseProperties that = (HbaseProperties) o;
    return getModulus() == that.getModulus()
           && getConfiguration().equals(that.getConfiguration())
           && getTableName().equals(that.getTableName())
           && getColumnFamily().equals(that.getColumnFamily());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getConfiguration(), getTableName(), getColumnFamily(), getModulus());
  }

  @Override
  public String toString() {
    return "HbaseProperties{"
           + "configuration="
           + configuration
           + ", tableName='"
           + tableName
           + '\''
           + ", columnFamily='"
           + columnFamily
           + '\''
           + ", modulus="
           + modulus
           + '}';
  }
}
