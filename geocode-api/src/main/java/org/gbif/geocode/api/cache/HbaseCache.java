package org.gbif.geocode.api.cache;

import org.gbif.geocode.api.model.GeoCacheKey;
import org.gbif.geocode.api.model.GeoInfo;
import org.gbif.geocode.api.model.HbaseProperties;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HBase backed cache for geo lookup queries.
 */
public class HbaseCache implements GeocodeService {

  public static final String LOCATIONS = "locations";
  private final String tableName;
  private final String columnFamily;
  private final Configuration configuration;
  private int modulus = 10;
  private Connection connection;
  private TableName tbName;
  private ObjectMapper mapper = new ObjectMapper();
  private GeocodeService databaseService;

  private static final Logger LOG = LoggerFactory.getLogger(HbaseCache.class);

  /**
   * salted key generator
   */
  private Function<GeoCacheKey, String> rowKeyGenerator =
    key -> String.format("%s|%s", Math.abs(key.hashCode() % modulus), key);

  public HbaseCache(
    GeocodeService service, HbaseProperties properties) {
    this.databaseService = service;
    this.tableName = properties.getTableName();
    this.columnFamily = properties.getColumnFamily();
    this.configuration = properties.getConfiguration();
    this.modulus = properties.getModulus();
  }

  /**
   * Initializes hbase connection and creates table if not available.
   * @throws IOException
   */
  public void initialize() throws IOException {
    connection = ConnectionFactory.createConnection(configuration);
    Admin admin = connection.getAdmin();

    tbName = TableName.valueOf(tableName);
    LOG.info("Looking for table in hbase {}.", tableName);
    if (!admin.tableExists(tbName)) {
      LOG.info("Could not find table in hbase {}. Creating one.", tableName);
      HTableDescriptor desc = new HTableDescriptor(tbName);
      desc.addFamily(new HColumnDescriptor(columnFamily));
      admin.createTable(desc);
      LOG.info("Table {} created.", tableName);
    }
  }

  /**
   * fetches the desired key from hbase cache or get it from lower level attached service.
   * @param key geo key
   * @return locations
   */
  public Collection<Location> get(GeoCacheKey key) {
    return Optional.ofNullable(bulkGet(Arrays.asList(key)))
      .filter(result -> !result.isEmpty())
      .map(result -> result.get(0).getLocations())
      .orElse(Arrays.asList());
  }

  public List<GeoInfo> bulkGet(List<GeoCacheKey> keys) {
    try (Table table = connection.getTable(tbName)) {

      List<Get> gets = new ArrayList();

      for (GeoCacheKey key : keys) {
        String rowKey = rowKeyGenerator.apply(key);
        Get get = new Get(Bytes.toBytes(rowKey));
        get.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(LOCATIONS));
        gets.add(get);
      }
      Result[] results = table.get(gets);
      List<GeoInfo> geoInfos = new ArrayList();

      for (int i = 0; i < results.length; i++) {
        byte[] bytes = results[i].getValue(Bytes.toBytes(columnFamily), Bytes.toBytes(LOCATIONS));
        // fail entire batch if not found
        if (bytes == null) return null;
        List<Location> locations = mapper.readValue(bytes, new TypeReference<List<Location>>() {});
        GeoCacheKey key = keys.get(i);
        geoInfos.add(new GeoInfo(key, locations));
      }
      return geoInfos;
    } catch (IOException e) {
      LOG.error("Error reading values from {}.", tableName, e);
    }
    return null;
  }

  /**
   * Puts the missed entry to hbase cache.
   * @param info
   */
  public void put(GeoInfo info) {
    bulkPut(Arrays.asList(info));
  }

  /**
   * Does batch puts to hbase cache.
   * @param geoInfos information of key with locations.
   */
  public void bulkPut(List<GeoInfo> geoInfos) {
    try (Table table = connection.getTable(tbName)) {

      List<Put> puts = new ArrayList();

      for (GeoInfo geoInfo : geoInfos) {
        GeoCacheKey key = geoInfo.getKey();
        Collection<Location> locations = geoInfo.getLocations();
        String rowKey = rowKeyGenerator.apply(key);
        Put put = new Put(Bytes.toBytes(rowKey));

        byte[] serializedLocations = new byte[0];
        try {
          serializedLocations = mapper.writeValueAsBytes(locations);
        } catch (JsonProcessingException e) {
          LOG.error("Could not serialize {}.", locations, e);
        }
        put.addImmutable(Bytes.toBytes(columnFamily), Bytes.toBytes(LOCATIONS), serializedLocations);
        puts.add(put);
      }
      LOG.debug("Writing put to table {}, size: {}", tableName, puts.size());
      table.put(puts);
      LOG.debug("Updated table {}, with size: {}", tableName, puts.size());
    } catch (IOException e) {
      LOG.error("Could not write to the hbase table {}", tableName, e);
    }
  }

  /**
   * closes the connection once done.
   * @throws Exception
   */
  public void cleanUp() throws Exception {
    if (connection != null) {
      LOG.debug("Closing connection for {}", HbaseCache.class.getName());
      connection.close();
    }
  }

  @Override
  public Collection<Location> get(Double latitude, Double longitude, Double uncertainty) {
    GeoCacheKey key = new GeoCacheKey(latitude, longitude, uncertainty);
    LOG.info("Received request: {}", key);
    Collection<Location> result = get(key);

    if (result.isEmpty()) {
      LOG.info("HBase cache missed for: {}", key);
      Collection<Location> locations = databaseService.get(latitude, longitude, uncertainty);
      LOG.info("SRS identified locations for: {} as {}", key, locations);
      put(new GeoInfo(key, locations));
      LOG.info("HBase saved for: {} as {}", key, locations);
      return locations;
    }
    LOG.info("result: {} as {}", key, result);
    return result;
  }

  @Override
  public byte[] bitmap() {
    return new byte[0];
  }
}
