package org.gbif.geocode.ws.resource;

import org.gbif.geocode.api.cache.HbaseCache;
import org.gbif.geocode.api.model.HbaseProperties;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.ws.service.impl.MyBatisGeocoder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HbaseCacheTest {

  private static HBaseTestingUtility utility;
  private static HbaseProperties properties;

  @BeforeClass
  public static void setup() throws Exception {
    utility = new HBaseTestingUtility();
    utility.startMiniCluster();
    properties = new HbaseProperties(utility.getConfiguration(), "geo", "locations", 1000);
  }

  /**
   * Test get request. Ensure that get comes from database once.
   */
  @Test
  public void getTest() throws IOException {
    MyBatisGeocoder dbGeocoder = mock(MyBatisGeocoder.class);

    HbaseCache geocoder = new HbaseCache(dbGeocoder, properties);
    geocoder.initialize();
    // In the Pacific within French Polynesia's EEZ.
    Location locationTest = new Location("test", "political", "source", "French Polynesia", "PF");
    Location locationTest2 = new Location("test", "political", "source", "French Polynesia", "PF");

    when(dbGeocoder.get(-21.0d, -147.0d, null)).thenReturn(Arrays.asList(locationTest));

    Collection<Location> locations = geocoder.get(-21.0d, -147.0d, null);
    Collection<Location> locations2 = geocoder.get(-21.0d, -147.0d, null);

    verify(dbGeocoder, times(1)).get(-21.0d, -147.0d, null);

    assertEquals(1, locations.size());
    assertEquals(1, locations2.size());
    assertTrue(locations.contains(locationTest2));
    assertTrue(locations2.contains(locationTest2));
  }

  @Test
  public void nullTest() throws IOException {
    MyBatisGeocoder dbGeocoder = mock(MyBatisGeocoder.class);

    HbaseCache geocoder = new HbaseCache(dbGeocoder, properties);
    geocoder.initialize();
    // In the Pacific within French Polynesia's EEZ.
    Location locationTest = new Location("test", "political", "source", "French Polynesia", "PF");
    Location locationTest2 = new Location("test", "political", "source", "French Polynesia", "PF");

    when(dbGeocoder.get(-21.0d, -147.0d, null)).thenReturn(Arrays.asList(locationTest));

    Collection<Location> locations = geocoder.get(0.0d, 0.0d, null);
    Collection<Location> locations1 = geocoder.get(0.0d, 0.0d, null);

    Assert.assertEquals( 0,locations.size());
    Assert.assertEquals( 0,locations1.size());
  }
}
