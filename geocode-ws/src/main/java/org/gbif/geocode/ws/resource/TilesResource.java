package org.gbif.geocode.ws.resource;

import org.gbif.geocode.ws.model.Tile;
import org.gbif.geocode.ws.persistence.mapper.TileMapper;
import org.gbif.geocode.ws.resource.exception.OffWorldException;
import org.gbif.maps.common.projection.Double2D;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;

import io.swagger.v3.oas.annotations.Hidden;

/**
 * Fetches Mapbox Vector Tiles from PostgreSQL, using PostGIS' inbuilt functions for generating
 * them.
 */
@Hidden
@RestController
@RequestMapping("geocode/tile")
@CrossOrigin(
  allowedHeaders = {"authorization", "content-type"},
  exposedHeaders = {
    "Access-Control-Allow-Origin",
    "Access-Control-Allow-Methods",
    "Access-Control-Allow-Headers"
  })
public class TilesResource {
  public static final Logger LOG = LoggerFactory.getLogger(TilesResource.class);

  // The tile width in screen pixels.  The real vector tile is 4096px wide, but we
  // don't need that detail.  They are displayed on screen covering 512 pixels — though for a high-density
  // screen, it may be 1024 or more pixels in reality.
  final int TILE_WIDTH_IN_PIXELS = 512;
  final double[] scale;
  final double[] buffer;

  private final TileMapper tileMapper;

  public TilesResource(TileMapper tileMapper) {
    this.tileMapper = tileMapper;

    scale = new double[20];
    buffer = new double[20];

    for (int z = 0; z < scale.length; z++) {
      Double2D[] b = tileBoundary(z, 1, 0, 0);
      scale[z] = 180d / (TILE_WIDTH_IN_PIXELS * Math.pow(2, z));
      buffer[z] = (b[1].getX() - b[0].getX()) / 64; // Date line! And X vs Y degree sizes.
    }
  }

  /** Fetch a single tile for a layer. */
  @GetMapping(value = "{layer}/{z}/{x}/{y}.mvt", produces = "application/x-protobuf")
  public byte[] get(
      @PathVariable("layer") String layer,
      @PathVariable("z") int z,
      @PathVariable("x") long x,
      @PathVariable("y") long y,
      @RequestParam(value = "id", required = false) String id) {
    if (z < 0 || x > Math.pow(2, (z + 1)) || y > Math.pow(2, z)) {
      LOG.warn("Off world {}/{}/{}", z, x, y);
      throw new OffWorldException();
    }
    if (z > scale.length) {
      LOG.warn("Zoom too high {}/{}/{}", z, x, y);
      throw new OffWorldException();
    }

    Tile tile = tileMapper.fromCache(layer, z, x, y, id);
    if (tile != null) {
      LOG.debug("Tile {}/{}/{}/{}{} found in cache", layer, z, x, y, id == null ? "" : "?"+id);
      return tile.getT();
    } else {
      LOG.debug("Tile {}/{}/{}/{}{} not found in cache", layer, z, x, y, id == null ? "" : "?"+id);
    }

    Double2D[] b = tileBoundary(z, x, y, 0);

    Stopwatch sw = Stopwatch.createStarted();

    switch (layer) {
      case "political":
        tile = tileMapper.tilePolitical(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY(), buffer[z], scale[z], id);
        break;

      case "continent":
        tile = tileMapper.tileContinent(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY(), buffer[z], scale[z], id);
        break;

      case "gadm5":
        tile = tileMapper.tileGadm5(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY(), buffer[z], scale[z], id);
        break;

      case "gadm4":
        tile = tileMapper.tileGadm4(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY(), buffer[z], scale[z], id);
        break;

      case "gadm3":
        tile = tileMapper.tileGadm3(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY(), buffer[z], scale[z], id);
        break;

      case "gadm2":
        tile = tileMapper.tileGadm2(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY(), buffer[z], scale[z], id);
        break;

      case "gadm1":
        tile = tileMapper.tileGadm1(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY(), buffer[z], scale[z], id);
        break;

      case "iho":
        tile = tileMapper.tileIho(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY(), buffer[z], scale[z], id);
        break;
        break;

      case "wgsrpd":
        tile = tileMapper.tileWgsrpd(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY(), buffer[z], scale[z], id);
        break;

      case "centroids":
        tile = tileMapper.tileCentroids(b[0].getX(), b[0].getY(), b[1].getX(), b[1].getY(), buffer[z], scale[z], id);
        break;

      default:
        throw new UnsupportedOperationException("Unknown layer " + layer);
    }
    sw.stop();

    if (tile != null) {
      LOG.debug(
          "Tile {}/{}/{}/{}{} generated in {}ms ({}B)",
          layer,
          z,
          x,
          y,
          id == null ? "" : "?"+id,
          sw.elapsed(TimeUnit.MILLISECONDS),
          tile.getT().length);

      tileMapper.toCache(layer, z, x, y, id, tile, sw.elapsed(TimeUnit.MILLISECONDS));

      return tile.getT();
    }

    throw new UnsupportedOperationException();
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
