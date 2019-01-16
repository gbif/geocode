package org.gbif.geocode.ws.resource;

import org.gbif.geocode.api.cache.GeocodeBitmapCache;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.ws.service.impl.MyBatisGeocoder;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BitmapFirstGeocoderTest {

  /**
   * An initial request looks up the colour from the bitmap using the database,
   * subsequent requests to the same country are cached.
   */
  @Test
  public void testGoodRequest() {
    MyBatisGeocoder dbGeocoder = mock(MyBatisGeocoder.class);

    GeocodeBitmapCache geocoder = new GeocodeBitmapCache(dbGeocoder, this.getClass().getResourceAsStream("world.png"));

    Location locationTest = new Location("test", "political", "source", "Greenland", "GD");
    Location locationTest2 = new Location("test", "political", "source", "Greenland", "GD");

    when(dbGeocoder.get(75.0, -40.0, 0.0)).thenReturn(Arrays.asList(locationTest));

    Collection<Location> locations = geocoder.get(75.0, -40.0, null);
    Collection<Location> locations2 = geocoder.get(75.1, -40.1, null);

    verify(dbGeocoder, times(1)).get(75.0, -40.0, 0.0);
    verify(dbGeocoder, never()).get(75.1, -40.1, 0.0);

    assertEquals(1, locations.size());
    assertEquals(1, locations2.size());
    assertTrue(locations.contains(locationTest2));
    assertTrue(locations2.contains(locationTest2));
  }

  /**
   * Test that borders are read from the database every time.
   */
  @Test
  public void testBorderRequest() {
    MyBatisGeocoder dbGeocoder = mock(MyBatisGeocoder.class);

    GeocodeBitmapCache geocoder = new GeocodeBitmapCache(dbGeocoder, this.getClass().getResourceAsStream("world.png"));

    // All of Sri Lanka is covered with black borders in the bitmap image.
    Location locationTest = new Location("test", "political", "source", "Sri Lanka", "LK");
    Location locationTest2 = new Location("test", "political", "source", "Sri Lanka", "LK");

    when(dbGeocoder.get(7.0d, 81.0d, null)).thenReturn(Arrays.asList(locationTest));

    Collection<Location> locations = geocoder.get(7.0d, 81.0d, null);
    Collection<Location> locations2 = geocoder.get(7.0d, 81.0d, null);

    verify(dbGeocoder, times(2)).get(7.0d, 81.0d, null);

    assertEquals(1, locations.size());
    assertEquals(1, locations2.size());
    assertTrue(locations.contains(locationTest2));
    assertTrue(locations2.contains(locationTest2));
  }

  /**
   * Test that EEZ areas are read from the database every time.
   */
  @Test
  public void testEezRequest() {
    MyBatisGeocoder dbGeocoder = mock(MyBatisGeocoder.class);

    GeocodeBitmapCache geocoder = new GeocodeBitmapCache(dbGeocoder, this.getClass().getResourceAsStream("world.png"));

    // In the Pacific within French Polynesia's EEZ.
    Location locationTest = new Location("test", "political", "source", "French Polynesia", "PF");
    Location locationTest2 = new Location("test", "political", "source", "French Polynesia", "PF");

    when(dbGeocoder.get(-21.0d, -147.0d, null)).thenReturn(Arrays.asList(locationTest));

    Collection<Location> locations = geocoder.get(-21.0d, -147.0d, null);
    Collection<Location> locations2 = geocoder.get(-21.0d, -147.0d, null);

    verify(dbGeocoder, times(2)).get(-21.0d, -147.0d, null);

    assertEquals(1, locations.size());
    assertEquals(1, locations2.size());
    assertTrue(locations.contains(locationTest2));
    assertTrue(locations2.contains(locationTest2));
  }

  /**
   * Test that international water doesn't go to the database at all.
   */
  @Test
  public void testInternationalWaterRequest() {
    MyBatisGeocoder dbGeocoder = mock(MyBatisGeocoder.class);

    GeocodeBitmapCache geocoder = new GeocodeBitmapCache(dbGeocoder, this.getClass().getResourceAsStream("world.png"));

    Collection<Location> locations = geocoder.get(0d, 0d, null);

    verify(dbGeocoder, never()).get(0d, 0d, null);

    assertEquals(0, locations.size());
  }

  /**
   * Test that exceptional areas aren't cached for the whole country.
   */
  @Test
  public void testExceptionalAreasRequest() {
    MyBatisGeocoder dbGeocoder = mock(MyBatisGeocoder.class);

    GeocodeBitmapCache geocoder = new GeocodeBitmapCache(dbGeocoder, this.getClass().getResourceAsStream("world.png"));

    Location locationCosmodrome = new Location("test", "political", "source", "Baikonur Cosmodrome", "-99");
    Location locationKazakhstan = new Location("test", "political", "source", "Kazakhstan", "KZ");
    Location locationKazakhstan2 = new Location("test", "political", "source", "Kazakhstan", "KZ");

    when(dbGeocoder.get(45.965, 63.305, 0.0)).thenReturn(Arrays.asList(locationCosmodrome));
    when(dbGeocoder.get(47.0, 69.0, 0.0)).thenReturn(Arrays.asList(locationKazakhstan));

    Collection<Location> locations = geocoder.get(45.965, 63.305, null);
    Collection<Location> locations2 = geocoder.get(47.0, 69.0, null);

    verify(dbGeocoder, times(1)).get(45.965, 63.305, 0.0);
    verify(dbGeocoder, times(1)).get(47.0, 69.0, 0.0);

    assertEquals(1, locations.size());
    assertEquals(1, locations2.size());
    assertTrue(locations.contains(locationCosmodrome));
    assertTrue(locations2.contains(locationKazakhstan2));
  }
}
