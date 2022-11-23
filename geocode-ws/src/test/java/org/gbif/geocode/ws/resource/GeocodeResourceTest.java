package org.gbif.geocode.ws.resource;

import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.ws.resource.exception.OffWorldException;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GeocodeResourceTest {

  @Test
  public void testMissingParameters() {
    GeocodeService geocodeService = new GeocodeResource(null, null);
    assertThrows(OffWorldException.class, () -> geocodeService.get(null, null, null, null));
  }

  @Test
  public void testMissingParameter() {
    GeocodeService geocodeService = new GeocodeResource(null, null);
    assertThrows(OffWorldException.class, () -> geocodeService.get(10.0d, null, null, null));
  }

  @Test
  public void testOffWorldParameter() {
    GeocodeService geocodeService = new GeocodeResource(null, null);
    assertThrows(OffWorldException.class, () -> geocodeService.get(95.0d, 0.0d, null, null));
  }

  @Test
  public void testGoodRequest() {
    GeocodeService geocoder = mock(GeocodeService.class);
    GeocodeService geocodeService = new GeocodeResource(geocoder, null);

    List<String> defaultLayers = Arrays.asList(
      "Political",
      "Centroids",
      "Continent",
      "GADM",
      "GADM1",
      "GADM2",
      "GADM3",
      "IHO",
      "WGSRPD");

    Location locationTest = new Location("test", "political", "source", "Germany", "DE", 0d, 0d);
    Location locationTest2 = new Location("test", "political", "source", "Germany", "DE", 0d, 0d);
    when(geocoder.get(10.0d, 53.0d, null, null, defaultLayers)).thenReturn(Arrays.asList(locationTest));
    Collection<Location> locations = geocodeService.get(10.0d, 53.0d, null, null, null);
    verify(geocoder).get(10.0d, 53.0d, null, null, defaultLayers);
    assertEquals(1, locations.size());
    assertTrue(locations.contains(locationTest2));
  }
}
