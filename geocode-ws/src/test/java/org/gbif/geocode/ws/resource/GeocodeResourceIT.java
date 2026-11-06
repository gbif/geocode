package org.gbif.geocode.ws.resource;

import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.ws.client.GeocodeWsClient;
import org.gbif.ws.client.ClientBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.gbif.ws.json.JacksonJsonObjectMapperProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:application-test.properties",
  properties = {"spring.shapefiles.enabled=PoliticalLayer, MrEezLayer, MrIhoLayer, MrLmeLayer", "spring.defaultLayers=Political, MR"})
public class GeocodeResourceIT {

  private final GeocodeService geocodeClient;

  public GeocodeResourceIT(@LocalServerPort int localServerPort) {
    this.geocodeClient =
        new ClientBuilder()
            .withObjectMapper(JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport())
            .withUrl("http://localhost:" + localServerPort)
            .build(GeocodeWsClient.class);
  }

  @Test
  public void reverseTest() {
    List<Location> result = geocodeClient.get(-1d, -1d, 0.1d, null, Collections.emptyList());
    assertEquals(2, result.size());
    result.sort(new Comparator<Location>() {
      @Override
      public int compare(Location location, Location t1) {
        return location.getTitle().compareTo(t1.getTitle());
      }
    });
    assertEquals("High Seas", result.get(0).getTitle());
    assertEquals("South Atlantic Ocean", result.get(1).getTitle());
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
