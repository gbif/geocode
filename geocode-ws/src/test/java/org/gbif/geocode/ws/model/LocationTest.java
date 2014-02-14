package org.gbif.geocode.ws.model;

import org.gbif.geocode.api.model.Location;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class LocationTest {

  @Test
  public void testEquals() {
    Location locationOne = new Location("id", "type", "source", "title", "iso");
    Location locationTwo = new Location();

    assertThat(locationOne, not(equalTo(locationTwo)));

    locationTwo.setId("id");
    locationTwo.setType("type");
    locationTwo.setSource("source");
    locationTwo.setTitle("title");
    assertThat(locationOne, not(equalTo(locationTwo)));

    locationTwo.setIsoCountryCode2Digit("foobar");
    assertThat(locationOne, not(equalTo(locationTwo)));

    locationTwo.setIsoCountryCode2Digit("iso");
    assertThat(locationOne, equalTo(locationTwo));
    assertThat(locationOne.hashCode(), equalTo(locationTwo.hashCode()));
  }

}
