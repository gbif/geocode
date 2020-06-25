package org.gbif.geocode.ws.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.ws.client.guice.GeocodeWs;
import org.gbif.ws.client.BaseWsClient;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Singleton
public class GeocodeWsClient extends BaseWsClient implements GeocodeService {

  private static final String GEOCODE_PATH = "geocode";

  @Inject
  public GeocodeWsClient(@GeocodeWs WebResource resource) {
    super(resource.path(GEOCODE_PATH));
  }

  @Override
  public Collection<Location> get(Double latitude, Double longitude, Double uncertainty) {
    return get(latitude, longitude, uncertainty, Collections.EMPTY_LIST);
  }

  @Override
  public Collection<Location> get(Double latitude, Double longitude, Double uncertainty, List<String> layers) {
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    queryParams.add("lat", latitude.toString());
    queryParams.add("lng", longitude.toString());
    if (uncertainty != null) queryParams.add("uncertainty", uncertainty.toString());
    layers.stream().forEach(l -> queryParams.add("layer", l));

    return Arrays.asList(resource.path("reverse").queryParams(queryParams).get(Location[].class));
  }

  @Override
  public byte[] bitmap() {
    return resource.path("bitmap").get(byte[].class);
  }
}
