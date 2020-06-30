package org.gbif.geocode;

import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.ws.monitoring.GeocodeWsStatistics;
import org.gbif.geocode.ws.resource.GeocodeResource;
import org.gbif.geocode.ws.resource.OffWorldException;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocationResourceTest {

  @Test(expected = OffWorldException.class)
  public void testMissingParameters() {
    GeocodeService geocodeService = new GeocodeResource(null, null);
    geocodeService.get(null, null, null, null);
  }

  @Test(expected = OffWorldException.class)
  public void testMissingParameter() {
    GeocodeService geocodeService = new GeocodeResource(null, null);
    geocodeService.get(10.0d, null, null, null);
  }

  @Test(expected = OffWorldException.class)
  public void testOffworldParameter() {
    GeocodeService geocodeService = new GeocodeResource(null, null);
    geocodeService.get(95.0d, 0.0d, null, null);
  }

  @Test
  public void testGoodRequest() {
    GeocodeWsStatistics statistics = mock(GeocodeWsStatistics.class);
    GeocodeService geocoder = mock(GeocodeService.class);
    GeocodeService geocodeService = new GeocodeResource(geocoder, statistics);

    Location locationTest = new Location("test", "political", "source", "Germany", "DE");
    Location locationTest2 = new Location("test", "political", "source", "Germany", "DE");
    when(geocoder.get(10.0d, 53.0d, null, null)).thenReturn(Arrays.asList(locationTest));
    Collection<Location> locations = geocodeService.get(10.0d, 53.0d, null, null);
    verify(statistics).goodRequest();
    verify(geocoder).get(10.0d, 53.0d, null, null);
    assertEquals(1, locations.size());
    assertTrue(locations.contains(locationTest2));
  }
}
