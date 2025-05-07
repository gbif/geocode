package org.gbif.geocode.api.cache;

import org.gbif.geocode.api.model.Location;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a single geo-layer and its bitmap cache.
 */
public abstract class AbstractBitmapCachedLayer {
  private Logger LOG = LoggerFactory.getLogger(getClass());

  // Bitmap image cache
  private final BufferedImage img;
  // Border colour must be queried
  private final static int BORDER = 0x000000;
  // Empty colour is not part of this layer (e.g. ocean for a land layer)
  private final static int EMPTY = 0xFFFFFF;
  private final int imgWidth;
  private final int imgHeight;
  // Maximum number of locations in a coloured part of the map
  private final int maxLocations;
  private final Map<Integer, List<Location>> colourKey = new HashMap<>();
  public long queries = 0, border = 0, empty = 0, miss = 0, hit = 0;

  private static final double BITMAP_UNCERTAINTY_DEGREES = 0.05d;

  public AbstractBitmapCachedLayer(InputStream bitmap) {
    this(bitmap, 1);
  }

  public AbstractBitmapCachedLayer(InputStream bitmap, int maxLocations) {
    try {
      img = ImageIO.read(bitmap);
      imgHeight = img.getHeight();
      imgWidth = img.getWidth();
      this.maxLocations = maxLocations;
    } catch (IOException e) {
      throw new RuntimeException("Unable to load map image", e);
    }
  }

  /**
   * Layer name, used in API responses and logging.
   */
  public abstract String name();

  /**
   * Layer source (e.g. a URL), used in API responses.
   */
  public abstract String source();

  /**
   * Maximum uncertainty distance, in degrees, to allow for queries to this layer.
   */
  public abstract double adjustUncertainty(double uncertaintyDegrees, double latitude);

  /**
   * Query the layer, using the bitmap cache first if the uncertainty allows it.
   */
  public final List<Location> query(double latitude, double longitude, double uncertaintyDegrees) {
    List<Location> found = null;
    if (uncertaintyDegrees <= BITMAP_UNCERTAINTY_DEGREES) { // TODO per layer
      found = checkBitmap(latitude, longitude);
    }

    if (found == null) {
      found = queryDatasource(latitude, longitude, adjustUncertainty(uncertaintyDegrees, latitude));

      if (uncertaintyDegrees <= BITMAP_UNCERTAINTY_DEGREES) {
        putBitmap(latitude, longitude, found);
      }
    }

    return found;
  }

  /**
   * Query the underlying datasource (PostGIS, shapefile etc).
   */
  protected abstract List<Location> queryDatasource(double latitude, double longitude, double uncertainty);

  /**
   * Check the colour of a pixel from the map image to determine the country.
   * @return Locations or null if the bitmap can't answer.
   */
  List<Location> checkBitmap(double lat, double lng) {
    // Convert the latitude and longitude to x,y coordinates on the image.
    // The axes are swapped, and the image's origin is the top left.
    int x = (int) (Math.round((lng + 180d) / 360d * (imgWidth - 1)));
    int y = imgHeight - 1 - (int) (Math.round((lat + 90d) / 180d * (imgHeight - 1)));

    int colour = img.getRGB(x, y) & 0x00FFFFFF; // Ignore possible transparency.

    String hex = String.format("#%06x", colour);

    List<Location> locations;

    switch (colour) {
      case BORDER:
        border++;
        LOG.trace("LatLong {},{} has pixel {},{} with colour {} (BORDER)", lat, lng, x, y, hex);
        locations = null;
        break;

      case EMPTY:
        empty++;
        LOG.trace("LatLong {},{} has pixel {},{} with colour {} (EMPTY)", lat, lng, x, y, hex);
        locations = Collections.EMPTY_LIST;
        break;

      default:
        if (!colourKey.containsKey(colour)) {
          miss++;
          LOG.trace("LatLong {},{} has pixel {},{} with colour {} (MISS)", lat, lng, x, y, hex);
          locations = null;
        } else {
          hit++;
          locations = colourKey.get(colour);
          LOG.trace("LatLong {},{} has pixel {},{} with colour {} (HIT)", lat, lng, x, y, hex);
        }
    }

    if ((++queries % 10_000) == 0) {
      LOG.info("{} did {} cache lookups: {} border, {} empty, {} hit, {} miss.",
        name(), queries, border, empty, hit, miss);
    }

    return locations;
  }

  /**
   * Store a result in the bitmap cache, if it's not a border region.
   */
  void putBitmap(double lat, double lng, List<Location> locations) {
    // Convert the latitude and longitude to x,y coordinates on the image.
    // The axes are swapped, and the image's origin is the top left.
    int x = (int) (Math.round ((lng+180d)/360d*(imgWidth -1)));
    int y = imgHeight -1 - (int) (Math.round ((lat+90d)/180d*(imgHeight -1)));

    int colour = img.getRGB(x, y) & 0x00FFFFFF; // Ignore possible transparency.

    switch (colour) {
      case BORDER:
      case EMPTY:
        return;

      default:
        String hex = String.format("#%06x", colour);

        if (!colourKey.containsKey(colour)) {
          if (locations.isEmpty() || locations.size() > maxLocations) {
            LOG.error("{} (max {}) locations for a colour! {} (LL {},{}; pixel {},{}); locations {}",
              locations.size(), maxLocations, hex, lat, lng, x, y, joinLocations(locations));
          } else {
            if (LOG.isTraceEnabled()) {
              LOG.trace("LatLong {},{} has pixel {},{} with colour {} (STORE) {}", lat, lng, x, y, hex, joinLocations(locations));
            }
            colourKey.put(colour, locations);
          }
        }
    }
  }

  /**
   * Only used for the log message.
   */
  private String joinLocations(List<Location> loc) {
    return loc.stream().map(Location::getId).collect(Collectors.joining(", ")) + " " +
      loc.stream().map(l -> l.getDistance().toString()).collect(Collectors.joining(", "));
  }
}
