package org.gbif.geocode.ws.resource;

import org.gbif.geocode.ws.persistence.mapper.GeoJsonMapper;

import java.util.concurrent.TimeUnit;

import org.gbif.utils.text.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Stopwatch;

import io.swagger.v3.oas.annotations.Hidden;

/**
 * Fetches GeoJSON from PostgreSQL, using PostGIS' inbuilt functions for generating it.
 */
@Hidden
@RestController
@RequestMapping("geocode/feature")
@CrossOrigin(
  allowedHeaders = {"authorization", "content-type"},
  exposedHeaders = {
    "Access-Control-Allow-Origin",
    "Access-Control-Allow-Methods",
    "Access-Control-Allow-Headers"
  })
public class FeatureResource {
  public static final Logger LOG = LoggerFactory.getLogger(FeatureResource.class);

  private final GeoJsonMapper geoJsonMapper;

  public FeatureResource(GeoJsonMapper geoJsonMapper) {
    this.geoJsonMapper = geoJsonMapper;
  }

  /** Fetch a single tile for a layer. */
  @GetMapping(value = "{layer}.json", produces = "application/javascript")
  public String get(
      @PathVariable("layer") String layer,
      @RequestParam(value = "id", required = true) String id) {

    Stopwatch sw = Stopwatch.createStarted();

    String[] features = null;
    String start = "{\"type\" : \"FeatureCollection\", \"features\" : [\n";
    String end = "]}\n";

    switch (layer) {
      case "political":
        features = geoJsonMapper.geoJsonPolitical(id);
        break;

      case "continent":
        features = geoJsonMapper.geoJsonContinent(id);
        break;

      case "gadm5":
        features = geoJsonMapper.geoJsonGadm5(id);
        break;

      case "gadm4":
        features = geoJsonMapper.geoJsonGadm4(id);
        break;

      case "gadm3":
        features = geoJsonMapper.geoJsonGadm3(id);
        break;

      case "gadm2":
        features = geoJsonMapper.geoJsonGadm2(id);
        break;

      case "gadm1":
        features = geoJsonMapper.geoJsonGadm1(id);
        break;

      case "iho":
        features = geoJsonMapper.geoJsonIho(id);
        break;

      case "iucn":
        features = geoJsonMapper.geoJsonIucn(id);
        break;

      case "wgsrpd":
        features = geoJsonMapper.geoJsonWgsrpd(id);
        break;

      case "centroids":
        features = geoJsonMapper.geoJsonCentroids(id);
        break;

      default:
        throw new UnsupportedOperationException("Unsupported layer " + layer);
    }
    sw.stop();

    if (features != null) {
      String json = start + StringUtils.joinIfNotNull(",\n", features) + end;

      LOG.info(
          "GeoJSON {}?id={} generated in {}ms ({}B)",
          layer,
          id,
          sw.elapsed(TimeUnit.MILLISECONDS),
          json.length());

      return json;
    }

    throw new UnsupportedOperationException();
  }
}
