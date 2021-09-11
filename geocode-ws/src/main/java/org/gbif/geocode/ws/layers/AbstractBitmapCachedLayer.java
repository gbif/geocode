package org.gbif.geocode.ws.layers;

import org.gbif.geocode.api.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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

  public abstract String name();

  /**
   * Check the colour of a pixel from the map image to determine the country.
   * @return Locations or null if the bitmap can't answer.
   */
  public List<Location> checkBitmap(double lat, double lng) {
    // Convert the latitude and longitude to x,y coordinates on the image.
    // The axes are swapped, and the image's origin is the top left.
    int x = (int) (Math.round ((lng+180d)/360d*(imgWidth -1)));
    int y = imgHeight -1 - (int) (Math.round ((lat+90d)/180d*(imgHeight -1)));

    int colour = img.getRGB(x, y) & 0x00FFFFFF; // Ignore possible transparency.

    String hex = String.format("#%06x", colour);
    LOG.debug("LatLong {},{} has pixel {},{} with colour {}", lat, lng, x, y, hex);

    List<Location> locations;

    switch (colour) {
      case BORDER:
        return null;

      case EMPTY:
        return Collections.EMPTY_LIST;

      default:
        if (!colourKey.containsKey(colour)) {
          return null;
        } else {
          locations = colourKey.get(colour);
          LOG.debug("Known colour {} (LL {},{}; pixel {},{}) is {}", hex, lat, lng, x, y, joinLocations(locations));
        }
    }

    return locations;
  }

  /**
   * Store a result in the bitmap's cache, if it's not a border region.
   */
  public void putBitmap(double lat, double lng, List<Location> locations) {
    // Convert the latitude and longitude to x,y coordinates on the image.
    // The axes are swapped, and the image's origin is the top left.
    int x = (int) (Math.round ((lng+180d)/360d*(imgWidth -1)));
    int y = imgHeight -1 - (int) (Math.round ((lat+90d)/180d*(imgHeight -1)));

    int colour = img.getRGB(x, y) & 0x00FFFFFF; // Ignore possible transparency.

    String hex = String.format("#%06x", colour);

    switch (colour) {
      case BORDER:
      case EMPTY:
        return;

      default:
        if (!colourKey.containsKey(colour)) {
          if (locations.isEmpty() || locations.size() > maxLocations) {
            LOG.error("{} (max {}) locations for a colour! {} (LL {},{}; pixel {},{}); locations {}",
              locations.size(), maxLocations, hex, lat, lng, x, y, joinLocations(locations));
          } else {
            LOG.info("New colour {} (LL {},{}; pixel {},{}); remembering as {}",
              hex, lat, lng, x, y, joinLocations(locations));
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
