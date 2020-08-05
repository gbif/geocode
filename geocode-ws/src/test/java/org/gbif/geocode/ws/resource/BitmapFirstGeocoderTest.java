//package org.gbif.geocode.ws.resource;
//
//import org.gbif.geocode.api.cache.GeocodeBitmapCache;
//import org.gbif.geocode.api.model.Location;
//
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.List;
//
//import org.gbif.geocode.ws.layers.EezLayer;
//import org.gbif.geocode.ws.service.impl.MyBatisGeocoder;
//import org.junit.Test;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//import static org.mockito.Mockito.times;
//
//public class BitmapFirstGeocoderTest {
//
//  /**
//   * An initial request looks up the colour from the bitmap using the database,
//   * subsequent requests to the same country are cached.
//   */
//  @Test
//  public void testGoodRequest() {
//    MyBatisGeocoder dbGeocoder = mock(MyBatisGeocoder.class);
//
//    GeocodeBitmapCache geocoder = new GeocodeBitmapCache(dbGeocoder, this.getClass().getResourceAsStream("cache-bitmap.png"));
//
//    Location locationTest = new Location("test", "political", "source", "Greenland", "GD", 0d);
//    Location locationTest2 = new Location("test", "political", "source", "Greenland", "GD", 0d);
//
//    when(dbGeocoder.get(75.0, -40.0, null, null)).thenReturn(Arrays.asList(locationTest));
//
//    Collection<Location> locations = geocoder.get(75.0, -40.0, null, null);
//    Collection<Location> locations2 = geocoder.get(75.1, -40.1, null, null);
//
//    verify(dbGeocoder, times(1)).get(75.0, -40.0, null, null);
//    verify(dbGeocoder, never()).get(75.1, -40.1, null, null);
//
//    assertEquals(1, locations.size());
//    assertEquals(1, locations2.size());
//    assertTrue(locations.contains(locationTest2));
//    assertTrue(locations2.contains(locationTest2));
//  }
//
//  /**
//   * Test that borders are read from the database every time.
//   */
//  @Test
//  public void testBorderRequest() {
//    MyBatisGeocoder dbGeocoder = mock(MyBatisGeocoder.class);
//
//    GeocodeBitmapCache geocoder = new GeocodeBitmapCache(dbGeocoder, this.getClass().getResourceAsStream("cache-bitmap.png"));
//
//    // All of Sri Lanka is covered with black borders in the bitmap image.
//    Location locationTest = new Location("test", "political", "source", "Denmark", "DK", 0d);
//    Location locationTest2 = new Location("test", "political", "source", "Denmark", "DK", 0d);
//
//    when(dbGeocoder.get(55.102d, 14.685d, null, null)).thenReturn(Arrays.asList(locationTest));
//
//    Collection<Location> locations = geocoder.get(55.102d, 14.685d, null, null);
//    Collection<Location> locations2 = geocoder.get(55.102d, 14.685d, null, null);
//
//    verify(dbGeocoder, times(2)).get(55.102d, 14.685d, null, null);
//
//    assertEquals(1, locations.size());
//    assertEquals(1, locations2.size());
//    assertTrue(locations.contains(locationTest2));
//    assertTrue(locations2.contains(locationTest2));
//  }
//
//  /**
//   * Test that EEZ areas are cached (if reasonable).
//   */
//  @Test
//  public void testEezRequest() {
//    MyBatisGeocoder dbGeocoder = mock(MyBatisGeocoder.class);
//
//    GeocodeBitmapCache geocoder = new GeocodeBitmapCache(dbGeocoder, EezLayer.class.getResourceAsStream("eez.png"));
//
//    // In the Pacific within French Polynesia's EEZ.
//    Location locationTest = new Location("test", "political", "source", "French Polynesia", "PF", 0d);
//    Location locationTest2 = new Location("test", "political", "source", "French Polynesia", "PF", 0d);
//
//    when(dbGeocoder.get(-21.0d, -147.0d, null, null)).thenReturn(Arrays.asList(locationTest));
//
//    Collection<Location> locations = geocoder.get(-21.0d, -147.0d, null, null);
//    Collection<Location> locations2 = geocoder.get(-21.0d, -147.1d, null, null);
//
//    verify(dbGeocoder, times(1)).get(-21.0d, -147.0d, null, null);
//    verify(dbGeocoder, never()).get(-21.0, -147.1, null, null);
//
//    assertEquals(1, locations.size());
//    assertEquals(1, locations2.size());
//    assertTrue(locations.contains(locationTest2));
//    assertTrue(locations2.contains(locationTest2));
//  }
//
//  /**
//   * Test that international water doesn't go to the database at all.
//   */
//  @Test
//  public void testInternationalWaterRequest() {
//    MyBatisGeocoder dbGeocoder = mock(MyBatisGeocoder.class);
//
//    GeocodeBitmapCache geocoder = new GeocodeBitmapCache(dbGeocoder, EezLayer.class.getResourceAsStream("eez.png"));
//
//    Collection<Location> locations = geocoder.get(0d, 0d, null, null);
//
//    verify(dbGeocoder, never()).get(0d, 0d, null, null);
//
//    assertEquals(0, locations.size());
//  }
//
//  /**
//   * Test that layer requests always go to the database.
//   */
//  @Test
//  public void testLayerRequest() {
//    MyBatisGeocoder dbGeocoder = mock(MyBatisGeocoder.class);
//
//    GeocodeBitmapCache geocoder = new GeocodeBitmapCache(dbGeocoder, EezLayer.class.getResourceAsStream("eez.png"));
//
//    List<String> eezLayer = Arrays.asList(new String[]{"EEZ"});
//
//    Collection<Location> locations = geocoder.get(0d, 0d, null, null, eezLayer);
//
//    verify(dbGeocoder, times(1)).get(0d, 0d, null, null, eezLayer);
//
//    assertEquals(0, locations.size());
//  }
//}
