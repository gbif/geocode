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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
public class GeocodeResourceIT {

  @MockBean(name = "myBatisGeocoder")
  private GeocodeService myBatisGeocoder;

  private final GeocodeService geocodeClient;

  public GeocodeResourceIT(@LocalServerPort int localServerPort) {
    this.geocodeClient =
        new ClientBuilder()
            .withUrl("http://localhost:" + localServerPort)
            .build(GeocodeWsClient.class);
  }

  @Test
  public void reverseTest() {
    Location testLocation = new Location();
    testLocation.setTitle("my title");

    when(myBatisGeocoder.get(any(), any(), any(), any()))
        .thenReturn(Collections.singletonList(testLocation));

    Collection<Location> result = geocodeClient.get(1d, 1d, 1d, null, Collections.emptyList());
    assertEquals(1, result.size());
    assertEquals(testLocation, result.iterator().next());
  }

  @Test
  public void reverseWithLayersTest() {
    Location testLocation = new Location();
    testLocation.setTitle("my title");

    when(myBatisGeocoder.get(any(), any(), any(), any(), any()))
        .thenReturn(Collections.singletonList(testLocation));

    Collection<Location> result =
        geocodeClient.get(1d, 1d, 1d, null, Collections.singletonList("political"));
    assertEquals(1, result.size());
    assertEquals(testLocation, result.iterator().next());
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
