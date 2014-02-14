package org.gbif.geocode.ws.client;

import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;

import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GeocodeWsClientTest {

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

    this.client = new GeocodeWsClient(resource);
  }

  @Test
  public void testSuccessfulLookup() {
    when(resource.queryParams(params)).thenReturn(resource);
    when(resource.get(Location[].class)).thenReturn(new Location[] {});
    client.get(latitude, longitude);
  }

  @Test(expected = UniformInterfaceException.class)
  public void testFailure() {
    ClientResponse resp = mock(ClientResponse.class);
    Exception ex = new UniformInterfaceException(resp);

    when(resource.queryParams(params)).thenReturn(resource);
    when(resource.get(Location[].class)).thenThrow(ex);
    client.get(latitude, longitude);
  }

}
