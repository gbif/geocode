package org.gbif.geocode.api.model;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

public class LocationTest {

  @Test
  public void testEquals() {
    Location locationOne = new Location("id", "type", "source", "title", "iso", 0d);
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
    MatcherAssert.assertThat(locationOne, CoreMatchers.equalTo(locationTwo));
    MatcherAssert.assertThat(locationOne.hashCode(), CoreMatchers.equalTo(locationTwo.hashCode()));
  }
}
