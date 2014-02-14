package org.gbif.geocode.ws.resource;

import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.ws.monitoring.GeocodeWsStatistics;
import org.gbif.geocode.ws.service.Geocoder;

import java.util.Collection;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Provides the web service interface to query our Geocoder.
 */
@Path("/reverse")
@Singleton
public class GeocodeResource implements GeocodeService {

  private final Geocoder geocoder;

  private final GeocodeWsStatistics statistics;

  @Inject
  public GeocodeResource(Geocoder geocoder, GeocodeWsStatistics statistics) {
    this.geocoder = geocoder;
    this.statistics = statistics;
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<Location> get(@QueryParam("lat") Double latitude, @QueryParam("lng") Double longitude) {
    if (latitude == null || longitude == null) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    statistics.goodRequest();
    return geocoder.get(latitude, longitude);
  }
}
