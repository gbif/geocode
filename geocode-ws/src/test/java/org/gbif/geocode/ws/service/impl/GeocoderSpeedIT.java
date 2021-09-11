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
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.base.Stopwatch;

/**
 * Speed comparisont/test for PostGIS and ShapeFile geocoders.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = GeocoderIntegrationTestsConfiguration.class)
public class GeocoderSpeedIT {
  GeocodeService geocoder;

  final MyBatisGeocoder myBatisGeocoder;
  final ShapefileGeocoder shapefileGeocoder;

  Stopwatch sf = Stopwatch.createUnstarted();
  List<String> layers = new ArrayList<>();

  @Autowired
  public GeocoderSpeedIT(MyBatisGeocoder myBatisGeocoder, ShapefileGeocoder shapefileGeocoder) {
    this.myBatisGeocoder = myBatisGeocoder;
    this.shapefileGeocoder = shapefileGeocoder;

    // Change according to test.
    geocoder = shapefileGeocoder;
  }

  @Test
  @Disabled
  public void compareRandomQueries() {

    int count = 100_000;
    List<String> testLayers = Arrays.asList(
      "Political",  // Political:  100000 queries in   1 seconds; 100000 per second
      "EEZ",        // EEZ:        100000 queries in  16 seconds;   6250 per second
      "GADM3210",   // GADM3210:   100000 queries in 449 seconds;    222 per second
      "Continent",  // Continent:  100000 queries in  34 seconds;   2941 per second
      "IHO",        // IHO:        100000 queries in  12 seconds;   8333 per second
      "WGSRPD");    // WGSRPD:    1000000 queries in   8 seconds; 125000 per second

    //                 PostGIS comparison (localhost PostGIS)
    //                 Political:   10000 queries in   8 seconds;   1250 per second
    //                 EEZ:         10000 queries in  34 seconds;    294 per second
    //                 GADM3210:   100000 queries in 112 seconds;    892 per second
    //                 Continent:  100000 queries in1314 seconds;     76 per second
    //                 IHO:        100000 queries in  74 seconds;   1351 per second
    //                 WGSRPD:     100000 queries in  23 seconds;   4347 per second

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
  public void compareRandomQueriesGadm() {
    int count = 20_000;

    List<String> testSfLayers = Arrays.asList("GADM3210"); // ShapeFile: 100000 queries in 449 seconds; 222 per second
    System.out.println("Testing GADM with Shapefile backend");
    testSfLayers = Arrays.asList(
      "Political",  // Political:  100000 queries in   1 seconds; 100000 per second
      "EEZ",        // EEZ:        100000 queries in  16 seconds;   6250 per second
      "GADM3210",   // GADM3210:   100000 queries in 449 seconds;    222 per second
      //"Continent",  // Continent:  100000 queries in  34 seconds;   2941 per second
      "IHO",        // IHO:        100000 queries in  12 seconds;   8333 per second
      "WGSRPD");    // WGSRPD:    1000000 queries in   8 seconds; 125000 per second

    sf.reset();
    sf.start();
    for (int i = 0; i < count; i++) {
      double latitude = Math.random() * 180 - 90;
      double longitude = Math.random() * 360 - 180;
      shapefileGeocoder.get(latitude, longitude, 0.05, null, testSfLayers).size();
    }
    sf.stop();
    System.out.println("SF " + "GADM3210: " + count + " queries in " + sf.elapsed(TimeUnit.SECONDS) + " seconds; "
      + ((double)count) / sf.elapsed(TimeUnit.SECONDS) + " per second");

    List<String> testMBLayers = Arrays.asList("GADM3", "GADM2", "GADM1"); // PostGIS: 100000 queries in 449 seconds; 222 per second
    System.out.println("Testing GADM with MyBatis backend");
    testMBLayers = Arrays.asList(
      "Political",  // Political:  100000 queries in   1 seconds; 100000 per second
      "EEZ",        // EEZ:        100000 queries in  16 seconds;   6250 per second
      "GADM3", "GADM2", "GADM1",   // GADM3210:   100000 queries in 449 seconds;    222 per second
      //"Continent",  // Continent:  100000 queries in  34 seconds;   2941 per second
      "IHO",        // IHO:        100000 queries in  12 seconds;   8333 per second
      "WGSRPD");    // WGSRPD:    1000000 queries in   8 seconds; 125000 per second

    sf.reset();
    sf.start();
    for (int i = 0; i < count; i++) {
      double latitude = Math.random() * 180 - 90;
      double longitude = Math.random() * 360 - 180;
      myBatisGeocoder.get(latitude, longitude, 0.05, null, testMBLayers).size();
    }
    sf.stop();
    System.out.println("PG " + "GADM3210: " + count + " queries in " + sf.elapsed(TimeUnit.SECONDS) + " seconds; "
      + ((double)count) / sf.elapsed(TimeUnit.SECONDS) + " per second");
  }

  @Test
  @Disabled
  public void allLayerQueries() {
    int count = 20_000;

    // ShapeFile: 20,000 queries in 125 seconds; 160 per second
    System.out.println("Testing all layers with Shapefile backend");
    sf.reset();
    sf.start();
    for (int i = 0; i < count; i++) {
      double latitude = Math.random() * 180 - 90;
      double longitude = Math.random() * 360 - 180;
      shapefileGeocoder.get(latitude, longitude, 0.05, null, Collections.EMPTY_LIST).size();
    }
    sf.stop();
    System.out.println("SF all layers: " + count + " queries in " + sf.elapsed(TimeUnit.SECONDS) + " seconds; "
      + ((double)count) / sf.elapsed(TimeUnit.SECONDS) + " per second");

    // PostGIS: 20,000 queries in 493 seconds; 40 per second
    System.out.println("Testing all layers with MyBatis backend");
    sf.reset();
    sf.start();
    for (int i = 0; i < count; i++) {
      double latitude = Math.random() * 180 - 90;
      double longitude = Math.random() * 360 - 180;
      myBatisGeocoder.get(latitude, longitude, 0.05, null, Collections.EMPTY_LIST).size();
    }
    sf.stop();
    System.out.println("PG all layers: " + count + " queries in " + sf.elapsed(TimeUnit.SECONDS) + " seconds; "
      + ((double)count) / sf.elapsed(TimeUnit.SECONDS) + " per second");
  }
}
