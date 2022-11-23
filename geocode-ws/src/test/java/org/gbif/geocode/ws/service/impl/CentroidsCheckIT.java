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
    // Straightforward test — centroid of São Tomé, which is at POINT(6.6 0.25)
    testCentroidDistance("São Tomé", 0.25, 6.6, SAO_TOME_PRINCIPE, 0d);
    // WITH stp AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(6.6 0.25)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 5000.0, RADIANS(00))) FROM stp;
    testCentroidDistance("São Tomé N", 0.295218463549971, 6.6, SAO_TOME_PRINCIPE, 5_000.0d);
    // WITH stp AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(6.6 0.25)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 5000.0, RADIANS(90))) FROM stp;
    testCentroidDistance("São Tomé E", 0.25, 6.64491618891327, SAO_TOME_PRINCIPE, 5_000.0d);

    // Straightforward test — centroid of Greenland, which is at POINT(-40 72)
    testCentroidDistance("Greenland", 72, -40, GREENLAND, 0d);
    // WITH gld AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(-40 72)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 5000.0, RADIANS(00))) FROM gld;
    testCentroidDistance("Greenland N", 72.044808287057, -40, GREENLAND, 5_000.0d);
    // WITH gld AS (SELECT ST_SetSRID(ST_GeomFromText('POINT(-40 72)'), 4326) AS geom) SELECT ST_AsEWKT(geom), ST_AsEWKT(ST_Project(geom::geography, 5000.0, RADIANS(90))) FROM gld;
    testCentroidDistance("Greenland E", 72, -39.8550905389327, GREENLAND, 5_000.0d);
  }

  private void testCentroidDistance(String explanation, double lat, double lng, Country country, double distance) {
    List<String> testLayers = Arrays.asList("Centroids");

    LOG.info("Testing {} ({},{}); want {} {}m", explanation, lat, lng, country, distance);
    List<Location> locations = geocoder.get(lat, lng, 0.05, null, testLayers);
    Location firstLocation = locations.get(0);

    Assertions.assertEquals(country, Country.fromIsoCode(firstLocation.getIsoCountryCode2Digit()));
    Assertions.assertEquals(distance, firstLocation.getDistanceMeters(), 0.1d);
  }
}
