package org.gbif.geocode.ws.resource;

import org.gbif.geocode.api.cache.GeocodeBitmapCache;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.ws.resource.exception.OffWorldException;
import org.gbif.geocode.ws.resource.exception.VeryUncertainException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Provides the web service interface to query our Geocoder. */
@Tag(
  name = "Reverse geocoding",
  description = "This API allows querying the GBIF layers store to find known " +
    "regions (countries, continents, seas and so on) at a particular coordinate.\n\n" +
    "It is used during GBIF occurrence indexing.",
  extensions = @io.swagger.v3.oas.annotations.extensions.Extension(
    name = "Order", properties = @ExtensionProperty(name = "Order", value = "0200"))
)
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
  private static final Logger LOG = LoggerFactory.getLogger(GeocodeResource.class);

  private final GeocodeService geocoderService;

  private final List<String> defaultLayers;

  private static final String DEFAULT_LAYERS_CACHE_BITMAP = "cache-bitmap.png";
  private final String eTag;

  public GeocodeResource(GeocodeService geocoderService,
                         @Nullable BuildProperties buildProperties) {
    this.geocoderService = new GeocodeBitmapCache(
        geocoderService, this.getClass().getResourceAsStream(DEFAULT_LAYERS_CACHE_BITMAP));
    this.eTag = buildProperties != null ? buildProperties.getVersion() : "unknown";

    this.defaultLayers = Arrays.asList(
      "Political",
      "Centroids",
      "Continent",
      "GADM",
      "GADM1",
      "GADM2",
      "GADM3",
      "IHO",
      "WDPA",
      "WGSRPD");
  }

  @Operation(
    operationId = "reverse",
    summary = "Reverse geocode a coordinate.",
    description = "Query the GBIF layer store for known areas containing the " +
      "given coordinate."
  )
  @Parameter(
    name = "lat",
    description = "Latitude in WGS84 decimal degrees."
  )
  @Parameter(
    name = "lng",
    description = "Longitude in WGS84 decimal degrees."
  )
  @Parameter(
    name = "uncertaintyDegrees",
    description = "An uncertainty in WGS84 decimal degrees.\n\n" +
      "Note the service has a built-in minimum according to the resolution " +
      "of the layer data.",
    schema = @Schema(minimum = "0.05", maximum = "10.0")
  )
  @Parameter(
    name = "uncertaintyMeters",
    description = "An uncertainty in metres.\n\n" +
      "Note the service has a built-in minimum according to the resolution " +
      "of the layer data.",
    schema = @Schema(minimum = "0.05", maximum = "10.0")
  )
  @Parameter(
    name = "layer",
    description = "One or more layers to query. If unspecified, a default " +
      "set of layers is queried.",
    array = @ArraySchema(schema = @Schema(implementation = String.class)),
    explode = Explode.TRUE
  )
  @GetMapping(value = "reverse", produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public List<Location> get(
      @RequestParam(value = "lat") Double latitude,
      @RequestParam(value = "lng") Double longitude,
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

    if (layers == null || layers.isEmpty()) {
      layers = new ArrayList<>(defaultLayers);
    } else if (layers.contains("GADM0") || layers.contains("GADM1") || layers.contains("GADM2") || layers.contains("GADM3")) {
      // As an optimization, GADM is queried as single layer
      layers.add("GADM");
    }

    // Limit the response to 1000 entries.  See the branch paging-responses for an initial implementation of paging,
    // though this is not a backwards compatible change.
    List<Location> locations = geocoderService.get(latitude, longitude, uncertaintyDegrees, uncertaintyMeters, layers);
    if (locations.size() > 1000) {
      LOG.warn("Truncating response for {}, {}, {}, {}, {} with {} results to 1000", latitude, longitude, uncertaintyDegrees, uncertaintyMeters, layers, locations.size());
      return locations.subList(0, 1000);
    } else {
      return locations;
    }
  }

  @Override
  public List<Location> get(
      Double latitude, Double longitude, Double uncertaintyDegrees, Double uncertaintyMeters) {
    return get(latitude, longitude, uncertaintyDegrees, uncertaintyMeters, Collections.EMPTY_LIST);
  }

  /*
   * Disable client-side caching until I work out a reasonable way to do it.
   */
  @Operation(
    operationId = "bitmap",
    summary = "A bitmap to use as a cache.",
    description = "This bitmap may be used as a cache.\n\n" +
      "The map colours are all distinct.  Associate the response from the `reverse` " +
      "method with a colour.  White is null, and black always requires querying " +
      "the API."
  )
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
      return ByteStreams.toByteArray(this.getClass().getResourceAsStream(DEFAULT_LAYERS_CACHE_BITMAP));
    } catch (IOException e) {
      return null;
    }
  }
}
