/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.geocode.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LocationEncoder}.0
 */
public class LocationEncoderTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Loads locations from the test JSON resource file.
   */
  private List<Location> loadLocations() throws Exception {
    InputStream in = getClass().getResourceAsStream("/locations.json");
    assertNotNull(in, "locations.json resource not found");
    JsonNode root = MAPPER.readTree(in);
    JsonNode locationsNode = root.get("locations");
    List<Location> locations = new ArrayList<>();
    for (JsonNode node : locationsNode) {
      Location location = new Location();
      location.setId(node.get("id").asText());
      location.setType(node.get("type").asText());
      location.setSource(node.get("source").asText());
      location.setTitle(node.get("title").asText());
      location.setIsoCountryCode2Digit(node.get("isoCountryCode2Digit").asText());
      location.setDistance(node.get("distance").asDouble());
      location.setDistanceMeters(node.get("distanceMeters").asDouble());
      locations.add(location);
    }
    return locations;
  }

  /**
   * Asserts the result list represents the same as the input.
   */
  private void verifyResult(List<Location> result) throws Exception {
    List<Location> original = loadLocations();
    assertNotNull(result);
    assertEquals(10, result.size());
    for (int i=0; i<original.size(); i++) {
      assertEquals(original.get(i).getId(), result.get(i).getId());
      assertEquals(original.get(i).getType(), result.get(i).getType());
      assertNull(result.get(i).getSource()); // it is expected to be dropped
      assertEquals(original.get(i).getTitle(), result.get(i).getTitle());
      assertEquals(original.get(i).getIsoCountryCode2Digit(), result.get(i).getIsoCountryCode2Digit());
      assertEquals(original.get(i).getDistance(), result.get(i).getDistance());
      assertEquals(original.get(i).getDistanceMeters(), result.get(i).getDistanceMeters());
    }
  }

  /**
   * Round-trip check of encoding, calling attention to the removal of the source.
   */
  @Test
  public void testEncodeDecode() throws Exception {
    byte[] encoded = LocationEncoder.encode(loadLocations());
    List<Location> decoded = LocationEncoder.decode(encoded);
    verifyResult(decoded);
  }

  /**
   * Round-trip check of encoding from bytes, calling attention to the removal of the source.
   */
  @Test
  public void testEncodeDecodFromBytes() throws Exception {
    try (InputStream in = getClass().getResourceAsStream("/locations.json")) {
      byte[] encoded = LocationEncoder.encodeFromJsonBytes(in.readAllBytes());
      List<Location> decoded = LocationEncoder.decode(encoded);
      verifyResult(decoded);
    }
  }

  /**
   * Verify the id prefixes are extracted and recreated.
   */
  @Test
  public void testIDPrefix() throws Exception {
    Location marine = loadLocations().stream()
        .filter(l -> l.getId().startsWith("http://marineregions.org/mrgid/"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No marine-regions location in test data"));

    LocationAvro encoded = LocationEncoder.toAvro(marine);
    assertEquals("8431", encoded.getId());
    assertEquals(LocationIdPrefix.MRGID, encoded.getIdPrefix());

    Location decoded = LocationEncoder.fromAvro(encoded);
    assertEquals("http://marineregions.org/mrgid/8431", decoded.getId());
  }
}
