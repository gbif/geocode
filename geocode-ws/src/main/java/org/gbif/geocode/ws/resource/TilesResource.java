package org.gbif.geocode.ws.resource;

import org.gbif.geocode.ws.model.Tile;
import org.gbif.geocode.ws.persistence.mapper.TileMapper;
import org.gbif.geocode.ws.resource.exception.OffWorldException;
import org.gbif.maps.common.projection.Double2D;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;

/**
 * Fetches Mapbox Vector Tiles from PostgreSQL, using PostGIS' inbuilt functions for generating
 * them.
 */
@RestController
@RequestMapping("tile")
@CrossOrigin(
  allowedHeaders = {"authorization", "content-type"},
  exposedHeaders = {
    "Access-Control-Allow-Origin",
    "Access-Control-Allow-Methods",
    "Access-Control-Allow-Headers"
  })
public class TilesResource {
  public static final Logger LOG = LoggerFactory.getLogger(TilesResource.class);

  private static final List<String> layers =
      Arrays.asList(
          "political",
          "eez",
          "geolocate_centroids",
          "gadm5",
          "gadm4",
          "gadm3",
          "gadm2",
          "gadm1",
          "iho",
          "seavox",
          "wgsrpd");

  private final TileMapper tileMapper;

  public TilesResource(TileMapper tileMapper) {
    this.tileMapper = tileMapper;
  }

  /** Fetch a single tile for a layer. */
  @GetMapping(value = "{layer}/{z}/{x}/{y}.mvt", produces = "application/x-protobuf")
  public byte[] get(
      @PathVariable("layer") String layer,
      @PathVariable("z") int z,
      @PathVariable("x") long x,
      @PathVariable("y") long y) {
    checkParameters(layer, z, x, y);

    Tile tile = tileMapper.fromCache(layer, z, x, y);
    if (tile != null) {
      LOG.debug("Tile {}/{}/{}/{} found in cache", layer, z, x, y);
      return tile.getT();
    } else {
      LOG.debug("Tile {}/{}/{}/{} not found in cache", layer, z, x, y);
    }

    Double2D[] b = tileBoundary(z, x, y, 0);

    Stopwatch sw = Stopwatch.createStarted();

    switch (layer) {
      case "political":
        tile = tileMapper.tilePolitical(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY());
        break;

      case "eez":
        tile = tileMapper.tileEez(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY());
        break;

      case "gadm5":
        tile = tileMapper.tileGadm5(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY());
        break;

      case "gadm4":
        tile = tileMapper.tileGadm4(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY());
        break;

      case "gadm3":
        tile = tileMapper.tileGadm3(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY());
        break;

      case "gadm2":
        tile = tileMapper.tileGadm2(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY());
        break;

      case "gadm1":
        tile = tileMapper.tileGadm1(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY());
        break;

      case "iho":
        tile = tileMapper.tileIho(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY());
        break;

      case "seavox":
        tile = tileMapper.tileSeaVoX(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY());
        break;

      case "wgsrpd":
        tile = tileMapper.tileWgsrpd(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY());
        break;

      case "geolocate_centroids":
        tile =
            tileMapper.tileGeolocateCentroids(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY());
        break;

      default:
        throw new UnsupportedOperationException("Unknown layer " + layer);
    }
    sw.stop();

    if (tile != null) {
      LOG.debug(
          "Tile {}/{}/{}/{} generated in {}ms ({}B)",
          layer,
          z,
          x,
          y,
          sw.elapsed(TimeUnit.MILLISECONDS),
          tile.getT().length);

      tileMapper.toCache(layer, z, x, y, tile, sw.elapsed(TimeUnit.MILLISECONDS));

      return tile.getT();
    }

    throw new UnsupportedOperationException();
  }

  private static void checkParameters(String layer, int z, long x, long y) {
    if (!layers.contains(layer)) {
      LOG.warn("Unknown layer {}", layer);
    }
    if (z < 0 || x > Math.pow(2, (z + 1)) || y > Math.pow(2, z)) {
      LOG.warn("Off world {}/{}/{}", z, x, y);
      throw new OffWorldException();
    }
  }

  /**
   * For the given tile, returns the envelope for the tile, with a buffer.
   *
   * @param z zoom
   * @param x tile X address
   * @param y tile Y address
   * @return an envelope for the tile, with the appropriate buffer
   */
  /*
   * Method copied from GBIF maps project.
   */
  private Double2D[] tileBoundary(int z, long x, long y, double tileBuffer) {
    int tilesPerZoom = 1 << z;
    double degreesPerTile = 180d / tilesPerZoom;
    double bufferDegrees = tileBuffer * degreesPerTile;

    // the edges of the tile after buffering
    double minLng = to180Degrees((degreesPerTile * x) - 180 - bufferDegrees);
    double maxLng = to180Degrees(minLng + degreesPerTile + (bufferDegrees * 2));

    // clip the extent (ES barfs otherwise)
    double maxLat = Math.min(90 - (degreesPerTile * y) + bufferDegrees, 90);
    double minLat = Math.max(maxLat - degreesPerTile - 2 * bufferDegrees, -90);

    return new Double2D[] {new Double2D(minLng, minLat), new Double2D(maxLng, maxLat)};
  }

  /** If the longitude is expressed from 0..360 it is converted to -180..180. */
  @VisibleForTesting
  static double to180Degrees(double longitude) {
    if (longitude > 180) {
      return longitude - 360;
    } else if (longitude < -180) {
      return longitude + 360;
    }
    return longitude;
  }
}
