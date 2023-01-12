package org.gbif.geocode.ws.resource;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.geocode.api.cache.GeocodeBitmapCache;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.api.service.InternalGeocodeService;
import org.gbif.geocode.ws.resource.exception.OffWorldException;
import org.gbif.geocode.ws.resource.exception.VeryUncertainException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
  private final InternalGeocodeService geocoderService;

  private final List<String> defaultLayers;

  private static final int DEFAULT_PAGE_LIMIT = 1000;

  private static final String DEFAULT_LAYERS_CACHE_BITMAP = "cache-bitmap.png";
  private final String eTag;

  public GeocodeResource(InternalGeocodeService geocoderService,
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
      "WGSRPD");
  }

  @GetMapping(value = "reverse", produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public PagingResponse<Location> get(
      @RequestParam(value = "lat") Double latitude,
      @RequestParam(value = "lng") Double longitude,
      @Nullable @RequestParam(value = "uncertaintyDegrees", required = false)
          Double uncertaintyDegrees,
      @Nullable @RequestParam(value = "uncertaintyMeters", required = false)
          Double uncertaintyMeters,
      @Nullable @RequestParam(value = "layer", required = false) List<String> layers,
      @Nullable Pageable page) {
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

    if (page == null) {
      page = new PagingRequest(0, DEFAULT_PAGE_LIMIT);
    }

    List<Location> locations = geocoderService.get(latitude, longitude, uncertaintyDegrees, uncertaintyMeters, layers);
    long size = locations.size();
    return new PagingResponse<>(
      page,
      size,
      locations.subList((int)page.getOffset(), (int)Math.min(size, page.getOffset()+page.getLimit())));
  }

  @Override
  public PagingResponse<Location> get(
      Double latitude, Double longitude, Double uncertaintyDegrees, Double uncertaintyMeters, Pageable page) {
    return get(latitude, longitude, uncertaintyDegrees, uncertaintyMeters, Collections.EMPTY_LIST, page);
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
      return ByteStreams.toByteArray(this.getClass().getResourceAsStream(DEFAULT_LAYERS_CACHE_BITMAP));
    } catch (IOException e) {
      return null;
    }
  }
}
