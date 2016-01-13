package org.gbif.geocode;

import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.ws.monitoring.GeocodeWsStatistics;
import org.gbif.geocode.ws.resource.GeocodeResource;
import org.gbif.geocode.ws.resource.OffWorldException;
import org.gbif.geocode.ws.service.Geocoder;

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
    geocodeService.get(null, null);
  }

  @Test(expected = OffWorldException.class)
  public void testMissingParameter() {
    GeocodeService geocodeService = new GeocodeResource(null, null);
    geocodeService.get(10.0d, null);
  }

  @Test(expected = OffWorldException.class)
  public void testOffworldParameter() {
    GeocodeService geocodeService = new GeocodeResource(null, null);
    geocodeService.get(95.0d, 0.0d);
  }

  @Test
  public void testGoodRequest() {
    GeocodeWsStatistics statistics = mock(GeocodeWsStatistics.class);
    Geocoder geocoder = mock(Geocoder.class);
    GeocodeService geocodeService = new GeocodeResource(geocoder, statistics);

    Location locationTest = new Location("test", "political", "source", "Germany", "DE");
    Location locationTest2 = new Location("test", "political", "source", "Germany", "DE");
    when(geocoder.get(10.0d, 53.0d)).thenReturn(Arrays.asList(locationTest));
    Collection<Location> locations = geocodeService.get(10.0d, 53.0d);
    verify(statistics).goodRequest();
    verify(geocoder).get(10.0d, 53.0d);
    assertEquals(1, locations.size());
    assertTrue(locations.contains(locationTest2));
  }
}
