package org.gbif.geocode.api.model;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LocationTest {

  @Test
  public void testEquals() {
    Location locationOne = new Location("id", "type", "source", "title", "iso", 0d, 0d);
    Location locationTwo = new Location();

    MatcherAssert.assertThat(locationOne, CoreMatchers.not(CoreMatchers.equalTo(locationTwo)));

    locationTwo.setId("id");
    locationTwo.setType("type");
    locationTwo.setSource("source");
    locationTwo.setTitle("title");
    MatcherAssert.assertThat(locationOne, CoreMatchers.not(CoreMatchers.equalTo(locationTwo)));

    locationTwo.setIsoCountryCode2Digit("foobar");
    MatcherAssert.assertThat(locationOne, CoreMatchers.not(CoreMatchers.equalTo(locationTwo)));

    locationTwo.setIsoCountryCode2Digit("iso");
    locationTwo.setDistance(0d);
    locationTwo.setDistanceMeters(0d);
    MatcherAssert.assertThat(locationOne, CoreMatchers.equalTo(locationTwo));
    MatcherAssert.assertThat(locationOne.hashCode(), CoreMatchers.equalTo(locationTwo.hashCode()));
  }

  @Test
  public void testCompare() {
    Location locationOne = new Location("EC", "EEZ", "EEZ", "EC", "EC", 0d, 0d);
    Location locationOneB = new Location("EC", "EEZ", "EEZ", "EC", "EC", 0d, 0d);
    Location locationTwo = new Location("EC", "Political", "Political", "EC", "EC", 0.015848712600069228d, 111_319.491 * 0.015848712600069228d);
    Location locationThree = new Location("EC", "Political", "Political", "EC", "EC", 0d, 0d);

    Assertions.assertTrue(Location.DISTANCE_COMPARATOR.compare(locationOne, locationTwo) < 0);
    Assertions.assertTrue(Location.DISTANCE_COMPARATOR.compare(locationOne, locationOneB) == 0);
    Assertions.assertTrue(Location.DISTANCE_COMPARATOR.compare(locationTwo, locationOne) > 0);
    Assertions.assertTrue(Location.DISTANCE_COMPARATOR.compare(locationOne, locationThree) == 0);
  }
}
