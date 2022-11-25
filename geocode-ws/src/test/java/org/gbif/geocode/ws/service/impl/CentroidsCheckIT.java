package org.gbif.geocode.ws.service.impl;

import com.google.common.base.Stopwatch;

import org.gbif.api.vocabulary.Country;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.gbif.api.vocabulary.Country.*;

/**
 * Check we give the correct response for centroids..
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = GeocoderIntegrationTestsConfiguration.class)
@TestPropertySource(value = "classpath:application-test.properties",
  properties = {"spring.shapefiles.enabled=CentroidsLayer", "spring.defaultLayers=Centroids"})
public class CentroidsCheckIT {
  private static final Logger LOG = LoggerFactory.getLogger(CentroidsCheckIT.class);

  private final GeocodeService geocoder;

  @Autowired
  public CentroidsCheckIT(GeocodeServiceImpl geocodeServiceImpl) {
    this.geocoder = geocodeServiceImpl;
  }

  @Test
  public void test5kmFromCentroid() {
    // Straightforward test — centroid of São Tomé, which is at POINT(7 1)
    testCentroidDistance("São Tomé", 1, 7, 0, SAO_TOME_PRINCIPE, 0d);
    // WITH stp AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(7 1)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 5000.0, RADIANS(00))) FROM stp;
    testCentroidDistance("São Tomé N", 1.04521832920333, 7, 0, SAO_TOME_PRINCIPE, 5_000.0d);
    // WITH stp AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(7 1)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 5000.0, RADIANS(90))) FROM stp;
    testCentroidDistance("São Tomé E", 1, 7.04492256033466, 0, SAO_TOME_PRINCIPE, 5_000.0d);

    // Straightforward test — centroid of Greenland, which is at POINT(-40 72)
    testCentroidDistance("Greenland", 72, -40, 0, GREENLAND, 0d);
    // WITH gld AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(-40 72)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 5000.0, RADIANS(00))) FROM gld;
    testCentroidDistance("Greenland N", 72.044808287057, -40, 0, GREENLAND, 5_000.0d);
    // WITH gld AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(-40 72)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 5000.0, RADIANS(90))) FROM gld;
    testCentroidDistance("Greenland E", 72, -39.8550905389327, 0, GREENLAND, 5_000.0d);

    // Check Antarctica at various longitudes.
    for (double longitude = -180; longitude <= 180; longitude += 30) {
      testCentroidDistance("South Pole", -90, longitude, 0, ANTARCTICA, 0d);
      // WITH ant AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(0 -90)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 5000.0, RADIANS(0))) FROM ant;
      testCentroidDistance("Antarctica", -89.9552348297552, longitude, 0, ANTARCTICA, 5_000d);
    }
  }

  @Test
  public void test20kmFromCentroid() {
    // Straightforward test — centroid of São Tomé, which is at POINT(7 1)
    testCentroidDistance("São Tomé", 1, 7, 20_000, SAO_TOME_PRINCIPE, 0d);
    // WITH stp AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(7 1)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 19999.0, RADIANS(00))) FROM stp;
    testCentroidDistance("São Tomé N", 1.18086419246701, 7, 20_000, SAO_TOME_PRINCIPE, 19_999.0d);
    // WITH stp AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(7 1)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 20001.0, RADIANS(00))) FROM stp;
    testCentroidDistance("Too far  N", 1.18088227977941, 7, 20_000, null, -1);
    // WITH stp AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(7 1)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 19999.0, RADIANS(90))) FROM stp;
    testCentroidDistance("São Tomé E", 1, 7.17968125665836, 20_000, SAO_TOME_PRINCIPE, 19_999.0d);
    // WITH stp AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(7 1)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 20001.0, RADIANS(90))) FROM stp;
    testCentroidDistance("Too far  E", 1, 7.17969922568244, 20_000, null, -1);

    // Straightforward test — centroid of Greenland, which is at POINT(-40 72)
    testCentroidDistance("Greenland", 72, -40, 20_000, GREENLAND, 0d);
    // WITH gld AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(-40 72)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 19999.0, RADIANS(00))) FROM gld;
    testCentroidDistance("Greenland N", 72.1792229426653, -40, 20_000, GREENLAND, 19_999.0d);
    // WITH gld AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(-40 72)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 20001.0, RADIANS(00))) FROM gld;
    testCentroidDistance("Greenland N", 72.1792408656902, -40, 20_000, null, -1);
    // WITH gld AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(-40 72)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 19999.0, RADIANS(90))) FROM gld;
    testCentroidDistance("Greenland E", 71.9991378619874, -39.4204079023137, 20_000, GREENLAND, 19_999.0d);
    // WITH gld AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(-40 72)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 20001.0, RADIANS(90))) FROM gld;
    testCentroidDistance("Greenland E", 72, -39.4203499437828, 20_000, null, -1);

    // Check Antarctica at various longitudes.
    for (double longitude = -180; longitude <= 180; longitude += 30) {
      testCentroidDistance("South Pole", -90, longitude, 20_000, ANTARCTICA, 0d);
      // WITH ant AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(0 -90)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 19999.0, RADIANS(0))) FROM ant;
      testCentroidDistance("Antarctica", -89.820948266531, longitude, 20_000, ANTARCTICA, 19_999);
      // WITH ant AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(0 -90)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 20001.0, RADIANS(0))) FROM ant;
      testCentroidDistance("Antarctica", -89.820930360461, longitude, 20_000, null, -1);
    }
  }

  private void testCentroidDistance(String explanation, double lat, double lng, double uncM, Country country, double distance) {
    List<String> testLayers = Arrays.asList("Centroids");

    LOG.info("Testing {} ({},{}); want {} {}m", explanation, lat, lng, country, distance);
    List<Location> locations = geocoder.get(lat, lng, 0.05, uncM, testLayers);

    if (country == null) {
      Assertions.assertEquals(0, locations.size());
    } else {
      Location firstLocation = locations.get(0);
      Assertions.assertEquals(country, Country.fromIsoCode(firstLocation.getIsoCountryCode2Digit()));
      Assertions.assertEquals(distance, firstLocation.getDistanceMeters(), 0.1d);
    }
  }
}
