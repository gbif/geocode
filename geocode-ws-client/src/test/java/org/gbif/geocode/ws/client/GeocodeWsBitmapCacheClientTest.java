package org.gbif.geocode.ws.client;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.MultivaluedMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GeocodeWsBitmapCacheClientTest {

  @Mock
  private WebResource resource;
  private MultivaluedMap<String, String> params;
  private GeocodeService client;

  Double latitude = 10.0d;
  Double longitude = 10.0d;

  @Before
  public void setUp() {
    MultivaluedMap<String, String> params = new MultivaluedMapImpl();
    params.add("lng", longitude.toString());
    params.add("lat", latitude.toString());
    this.params = params;

    when(resource.path("geocode")).thenReturn(resource);
    this.client = new GeocodeWsClient(resource);
  }

  @Test
  public void testSuccessfulLookup() {
    when(resource.path("reverse")).thenReturn(resource);
    when(resource.queryParams(params)).thenReturn(resource);
    when(resource.get(Location[].class)).thenReturn(new Location[] {});
    client.get(latitude, longitude, null, null);
  }

  @Test(expected = UniformInterfaceException.class)
  public void testFailure() {
    ClientResponse resp = mock(ClientResponse.class);
    Exception ex = new UniformInterfaceException(resp);

    when(resource.path("reverse")).thenReturn(resource);
    when(resource.queryParams(params)).thenReturn(resource);
    when(resource.get(Location[].class)).thenThrow(ex);
    client.get(latitude, longitude, null, null);
  }

}
