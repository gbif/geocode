package org.gbif.geocode;

import org.gbif.geocode.api.model.HbaseProperties;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.ws.monitoring.GeocodeWsStatistics;
import org.gbif.geocode.ws.resource.GeocodeResource;
import org.gbif.geocode.ws.resource.OffWorldException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocationResourceTest {

  private static HBaseTestingUtility utility;
  private static HbaseProperties properties;

  @BeforeClass
  public static void setup() throws Exception {
    utility = new HBaseTestingUtility();
    utility.startMiniCluster();
    properties = new HbaseProperties(utility.getConfiguration(), "geo", "locations", 1000);
  }

  @Test(expected = OffWorldException.class)
  public void testMissingParameters() {
    GeocodeService geocodeService = new GeocodeResource(null, null, properties);
    geocodeService.get(null, null, null);
  }

  @Test(expected = OffWorldException.class)
  public void testMissingParameter() {
    GeocodeService geocodeService = new GeocodeResource(null, null, properties);
    geocodeService.get(10.0d, null, null);
  }

  @Test(expected = OffWorldException.class)
  public void testOffworldParameter() {
    GeocodeService geocodeService = new GeocodeResource(null, null, properties);
    geocodeService.get(95.0d, 0.0d, null);
  }

  @Test
  public void testGoodRequest() throws IOException {
    GeocodeService geocoder = mock(GeocodeService.class);
    GeocodeWsStatistics statistics = mock(GeocodeWsStatistics.class);

    GeocodeService geocodeService = new GeocodeResource(geocoder, statistics, properties);

    Location locationTest = new Location("test", "political", "source", "Germany", "DE");
    Location locationTest2 = new Location("test", "political", "source", "Germany", "DE");
    when(geocoder.get(10.0d, 53.0d, null)).thenReturn(Arrays.asList(locationTest));
    Collection<Location> locations = geocodeService.get(10.0d, 53.0d, null);

    verify(geocoder).get(10.0d, 53.0d, null);
    assertEquals(1, locations.size());
    assertTrue(locations.contains(locationTest2));
  }
}
