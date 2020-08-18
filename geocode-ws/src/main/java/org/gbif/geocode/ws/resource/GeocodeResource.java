package org.gbif.geocode.ws.resource;

import org.gbif.geocode.api.cache.GeocodeBitmapCache;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.ws.monitoring.GeocodeWsStatistics;
import org.gbif.geocode.ws.resource.exception.OffWorldException;
import org.gbif.geocode.ws.resource.exception.VeryUncertainException;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.springframework.boot.info.BuildProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import com.google.common.io.ByteStreams;

/** Provides the web service interface to query our Geocoder. */
@RestController
@RequestMapping("geocode")
@CrossOrigin(
    allowedHeaders = {"authorization", "content-type"},
    exposedHeaders = {
      "Access-Control-Allow-Origin",
      "Access-Control-Allow-Methods",
      "Access-Control-Allow-Headers"
    })
public class GeocodeResource implements GeocodeService {

  private final GeocodeService geocoder;

  private final GeocodeWsStatistics statistics;

  private static final String ALL_LAYER_CACHE_BITMAP = "cache-bitmap.png";
  private final String eTag;

  public GeocodeResource(
      GeocodeService geocoder, GeocodeWsStatistics statistics, BuildProperties buildProperties) {
    this.statistics = statistics;
    this.geocoder =
        new GeocodeBitmapCache(
            geocoder, this.getClass().getResourceAsStream(ALL_LAYER_CACHE_BITMAP));
    this.eTag = buildProperties != null ? buildProperties.getVersion() : "unknown";
  }

  @Override
  @GetMapping(value = "reverse", produces = MediaType.APPLICATION_JSON_VALUE)
  public Collection<Location> get(
      @RequestParam("lat") Double latitude,
      @RequestParam("lng") Double longitude,
      @Nullable @RequestParam(value = "uncertaintyDegrees", required = false)
          Double uncertaintyDegrees,
      @Nullable @RequestParam(value = "uncertaintyMeters", required = false)
          Double uncertaintyMeters,
      @Nullable @RequestParam(value = "layer", required = false) List<String> layers) {
    if (latitude == null
        || longitude == null
        || latitude < -90
        || latitude > 90
        || longitude < -180
        || longitude > 180) {
      throw new OffWorldException("Latitude and/or longitude is out of range.");
    }
    if (uncertaintyDegrees != null && uncertaintyMeters != null) {
      throw new VeryUncertainException("Cannot specify uncertainty in both degrees and metres.");
    }

    statistics.goodRequest();
    return geocoder.get(latitude, longitude, uncertaintyDegrees, uncertaintyMeters, layers);
  }

  @Override
  public Collection<Location> get(
      Double latitude, Double longitude, Double uncertaintyDegrees, Double uncertaintyMeters) {
    return get(latitude, longitude, uncertaintyDegrees, uncertaintyMeters, Collections.emptyList());
  }

  /*
   * Disable client-side caching until I work out a reasonable way to do it.
   */
  @GetMapping(value = "bitmap", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<Resource> bitmap(WebRequest request) throws IOException {
    if (request.checkNotModified(eTag)) {
      // spring already set the response accordingly
      return null;
    }

    return ResponseEntity.ok().eTag(eTag).body(new ByteArrayResource(bitmap()));
  }

  /*
   * Disable client-side caching until I work out a reasonable way to do it.
   */
  @Override
  public byte[] bitmap() {
    try {
      return ByteStreams.toByteArray(this.getClass().getResourceAsStream(ALL_LAYER_CACHE_BITMAP));
    } catch (IOException e) {
      return null;
    }
  }
}
