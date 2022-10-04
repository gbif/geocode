package org.gbif.geocode.ws.resource;

import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.ws.client.GeocodeWsClient;
import org.gbif.ws.client.ClientBuilder;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:application-test.properties", properties = "spring.shapefiles.enabled=PoliticalLayer, IhoLayer")
public class GeocodeResourceIT {

  private final GeocodeService geocodeClient;

  public GeocodeResourceIT(@LocalServerPort int localServerPort) {
    this.geocodeClient =
        new ClientBuilder()
            .withUrl("http://localhost:" + localServerPort)
            .build(GeocodeWsClient.class);
  }

  @Test
  public void reverseTest() {
    Collection<Location> result = geocodeClient.get(-1d, -1d, 0.1d, null, Collections.emptyList());
    assertEquals(1, result.size());
    assertEquals("South Atlantic Ocean", result.iterator().next().getTitle());
  }

  @Test
  public void reverseWithLayersTest() {
    Collection<Location> result =
        geocodeClient.get(51d, 18d, 0.1d, null, Collections.singletonList("Political"));
    assertEquals(1, result.size());
    assertEquals("PL", result.iterator().next().getIsoCountryCode2Digit());
  }

  @Test
  public void bitmapTest() {
    assertNotNull(geocodeClient.bitmap());
  }

  @Test
  public void offWorldParametersTest() {
    assertThrows(IllegalArgumentException.class, () -> geocodeClient.get(100d, 1d, 1d, 1d));
  }

  @Test
  public void uncertainParametersTest() {
    assertThrows(IllegalArgumentException.class, () -> geocodeClient.get(1d, 1d, 1d, 1d));
  }
}
