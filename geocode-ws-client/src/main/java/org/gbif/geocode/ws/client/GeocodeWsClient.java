package org.gbif.geocode.ws.client;

import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.ws.client.guice.GeocodeWs;

import java.util.Arrays;
import java.util.Collection;
import javax.ws.rs.core.MultivaluedMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

@Singleton
public class GeocodeWsClient implements GeocodeService {

  private final WebResource resource;

  @Inject
  public GeocodeWsClient(@GeocodeWs WebResource resource) {
    this.resource = resource;
  }

  @Override
  public Collection<Location> get(Double latitude, Double longitude) {
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    queryParams.add("lat", latitude.toString());
    queryParams.add("lng", longitude.toString());
    return Arrays.asList(resource.queryParams(queryParams).get(Location[].class));
  }
}
