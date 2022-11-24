package org.gbif.geocode.ws.service.impl;

import org.gbif.geocode.api.service.GeocodeService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.base.Stopwatch;

/**
 * Speed comparison/test for PostGIS and ShapeFile geocoders.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = GeocoderIntegrationTestsConfiguration.class)
@TestPropertySource(value = "classpath:application-test.properties")
@Disabled
public class GeocoderSpeedIT {
  final GeocodeService geocoder;

  // Set to "" to test shapefiles, "PG_" to test PostGIS
  final String p = "";

  Stopwatch sf = Stopwatch.createUnstarted();
  List<String> layers = new ArrayList<>();

  @Autowired
  public GeocoderSpeedIT(GeocodeServiceImpl geocoderService) {
    this.geocoder = geocoderService;
  }

  @Test
  @Disabled
  public void speedTestRandomQueries() {
    // PostGIS speeds (localhost PostGIS, Matt's desktop, no geagraphy distance calculation)
    // Political:   10000 queries in   8 seconds;   1250 per second (obsolete layer)
    // EEZ:         10000 queries in  34 seconds;    294 per second (obsolete layer)
    // GADM:       100000 queries in 112 seconds;    892 per second
    // Continent:  100000 queries in  10 seconds;  10000 per second
    // IHO:        100000 queries in  74 seconds;   1351 per second
    // WGSRPD:     100000 queries in  23 seconds;   4347 per second

    int count = 10_000_000;
    List<String> testLayers = Arrays.asList(
                      // Speeds on Matt's desktop.
      "Political",    // Political:    10000000 queries in 13 seconds; 769230.7 per second
      "GADM",         // GADM:         10000000 queries in 48 seconds; 208333.3 per second
      "Continent",    // Continent:    10000000 queries in 15 seconds; 666666.6 per second
      "IHO",          // IHO:          10000000 queries in 15 seconds; 666666.6 per second
      "WGSRPD",       // WGSRPD:       10000000 queries in 14 seconds; 714285.7 per second
      "Centroids");   // Centroids:    10000000 queries in 14 seconds; 714285.7 per second

    for (String l : testLayers) {
      System.out.println("Testing layer "+l);

      layers.clear();
      layers.add(l);
      sf.reset();
      sf.start();
      for (int i = 0; i < count; i++) {
        double latitude = Math.random() * 180 - 90;
        double longitude = Math.random() * 360 - 180;

        geocoder.get(latitude, longitude, 0.05, null, layers).size();
      }
      sf.stop();

      System.out.println(l + ": " + count + " queries in " + sf.elapsed(TimeUnit.SECONDS) + " seconds; "
        + ((double)count) / sf.elapsed(TimeUnit.SECONDS) + " per second");
    }
  }

  @Test
  @Disabled
  public void speedTestRandomGadmQueries() {
    int count = 10_000_000;
    // SF GADM3210: 10000000 queries in 45 seconds; 222222.2 per second
    List<String> testSfLayers = Arrays.asList("GADM");
    System.out.println("Testing GADM with Shapefile backend");

    sf.reset();
    sf.start();
    for (int i = 0; i < count; i++) {
      double latitude = Math.random() * 180 - 90;
      double longitude = Math.random() * 360 - 180;
      geocoder.get(latitude, longitude, 0.05, null, testSfLayers).size();
    }
    sf.stop();
    System.out.println("SF " + "GADM3210: " + count + " queries in " + sf.elapsed(TimeUnit.SECONDS) + " seconds; "
      + ((double)count) / sf.elapsed(TimeUnit.SECONDS) + " per second");

    count = 100;
    // PG GADM3210 (without geography distance calculation): 100000 queries in 128 seconds; 781.25 per second
    List<String> testMBLayers = Arrays.asList("PG_GADM");
    System.out.println("Testing GADM with MyBatis backend");

    sf.reset();
    sf.start();
    for (int i = 0; i < count; i++) {
      double latitude = Math.random() * 180 - 90;
      double longitude = Math.random() * 360 - 180;
      geocoder.get(latitude, longitude, 0.05, null, testMBLayers).size();
    }
    sf.stop();
    System.out.println("PG " + "GADM3210: " + count + " queries in " + sf.elapsed(TimeUnit.SECONDS) + " seconds; "
      + ((double)count) / sf.elapsed(TimeUnit.SECONDS) + " per second");
  }

  @Test
  @Disabled
  public void speedTestAllLayerQueries() {

    int count = 10_000_000;
    // SF all layers: 10000000 queries in 137 seconds; 72992.7 per second

    System.out.println("Testing all layers with Shapefile backend");
    sf.reset();
    sf.start();
    for (int i = 0; i < count; i++) {
      double latitude = Math.random() * 180 - 90;
      double longitude = Math.random() * 360 - 180;
      geocoder.get(latitude, longitude, 0.05, null).size();
    }
    sf.stop();
    System.out.println("SF all layers: " + count + " queries in " + sf.elapsed(TimeUnit.SECONDS) + " seconds; "
      + ((double)count) / sf.elapsed(TimeUnit.SECONDS) + " per second");

    count = 100;
    // PG all layers (without geography distance calculation): 10000 queries in 87 seconds; 114.9 per second
    System.out.println("Testing all layers with PostGIS backend");
    List<String> defaultPGLayers = new ArrayList<>();
    defaultPGLayers.add("PG_Centroids");
    defaultPGLayers.add("PG_Continent");
    defaultPGLayers.add("PG_GADM3210");
    defaultPGLayers.add("PG_IHO");
    defaultPGLayers.add("PG_Political");
    defaultPGLayers.add("PG_WGSRPD");
    sf.reset();
    sf.start();
    for (int i = 0; i < count; i++) {
      double latitude = Math.random() * 180 - 90;
      double longitude = Math.random() * 360 - 180;
      geocoder.get(latitude, longitude, 0.05, null, defaultPGLayers).size();
    }
    sf.stop();
    System.out.println("PG all layers: " + count + " queries in " + sf.elapsed(TimeUnit.SECONDS) + " seconds; "
      + ((double)count) / sf.elapsed(TimeUnit.SECONDS) + " per second");
  }
}
