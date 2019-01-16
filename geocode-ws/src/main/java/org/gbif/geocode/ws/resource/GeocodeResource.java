package org.gbif.geocode.ws.resource;

import org.gbif.geocode.api.cache.GeocodeBitmapCache;
import org.gbif.geocode.api.cache.HbaseCache;
import org.gbif.geocode.api.model.HbaseProperties;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.ws.monitoring.GeocodeWsStatistics;

import java.io.IOException;
import java.util.Collection;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the web service interface to query our Geocoder.
 */
@Path("/geocode")
@Singleton
public class GeocodeResource implements GeocodeService {

  private static final Logger LOG = LoggerFactory.getLogger(GeocodeResource.class);

  private final GeocodeService geocoder;

  private final GeocodeWsStatistics statistics;

  private final EntityTag eTag = EntityTag.valueOf('"' + getClass().getPackage().getImplementationVersion() + '"');

  @Inject
  public GeocodeResource(GeocodeService geocoder, GeocodeWsStatistics statistics, HbaseProperties properties) {
    this.statistics = statistics;
    GeocodeBitmapCache bitmapCache = new GeocodeBitmapCache(geocoder, this.getClass().getResourceAsStream("world.png"));
    this.geocoder = new HbaseCache(bitmapCache, properties);
    try {
      ((HbaseCache) this.geocoder).initialize();
    } catch (Exception e) {
      LOG.error("Could not initialize hbase cache.", e);
    }
  }

  @Override
  @GET
  @Path("reverse")
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<Location> get(
    @QueryParam("lat") Double latitude,
    @QueryParam("lng") Double longitude,
    @QueryParam("uncertainty") @Nullable Double uncertainty
  ) {
    if (latitude == null
        || longitude == null
        || latitude < -90
        || latitude > 90
        || longitude < -180
        || longitude > 180) {
      throw new OffWorldException();
    }

    statistics.goodRequest();
    return geocoder.get(latitude, longitude, uncertainty);
  }

  @GET
  @Path("bitmap")
  @Produces("image/png")
  public Response bitmap(@Context Request request) {
    Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(eTag);

    if (responseBuilder == null) {
      return Response.ok(this.getClass().getResourceAsStream("world.png")).tag(eTag).build();
    } else {
      return responseBuilder.build();
    }
  }

  @Override
  public byte[] bitmap() {
    try {
      return ByteStreams.toByteArray(this.getClass().getResourceAsStream("world.png"));
    } catch (IOException e) {
      return null;
    }
  }
}
