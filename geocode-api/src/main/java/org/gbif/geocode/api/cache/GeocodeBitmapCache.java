package org.gbif.geocode.api.cache;

import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A cache which uses a bitmap to cache coordinate lookups.
 */
public class GeocodeBitmapCache implements GeocodeService {
  public static Logger LOG = LoggerFactory.getLogger(GeocodeBitmapCache.class);

  private final GeocodeService geocodeService;

  // Bitmap image cache
  private final BufferedImage img;
  // Border colour must be queried
  private final static int BORDER = 0x000000;
  // Nothing colour is not part of this layer (e.g. ocean for a land layer)
  private final static int NOTHING = 0xFFFFFF;
  private final int imgWidth;
  private final int imgHeight;
  private final Map<Integer, List<Location>> colourKey = new HashMap<>();

  public GeocodeBitmapCache(GeocodeService geocodeService, InputStream bitmap) {
    this.geocodeService = geocodeService;

    try {
      img = ImageIO.read(bitmap);
      imgHeight = img.getHeight();
      imgWidth = img.getWidth();
    } catch (IOException e) {
      throw new RuntimeException("Unable to load map image", e);
    }
  }

  /**
   * Simple get candidates by point.  No cache if layers are specified.
   */
  @Override
  public List<Location> get(Double lat, Double lng, Double uncertaintyDegrees, Double uncertaintyMeters, List<String> layers) {
    if (layers == null || layers.isEmpty()) {
      // No layers, use the method below
      return get(lat, lng, uncertaintyDegrees, uncertaintyMeters);
    } else {
      // Layers, skip the cache and go straight to the database.
      return geocodeService.get(lat, lng, uncertaintyDegrees, uncertaintyMeters, layers);
    }
  }

  /**
   * Simple get candidates by point.
   */
  @Override
  public List<Location> get(Double lat, Double lng, Double uncertaintyDegrees, Double uncertaintyMeters) {
    List<Location> locations = null;

    // Convert uncertainty in metres to degrees, approximating the Earth as a sphere.
    if (uncertaintyMeters != null) {
      uncertaintyDegrees = uncertaintyMeters / (111_319.491 * Math.cos(Math.toRadians(lat)));
      LOG.debug("{}m uncertainty converted to {}°", uncertaintyMeters, uncertaintyDegrees);
    }

    // Check the image map for a sure location.
    if (uncertaintyDegrees == null || uncertaintyDegrees <= 0.05d) {
      locations = getFromBitmap(lat, lng);
    }

    // If that doesn't help, use the database.
    if (locations == null) {
      locations = geocodeService.get(lat, lng, uncertaintyDegrees, uncertaintyMeters);
    }

    return locations;
  }

  @Override
  public byte[] bitmap() {
    return new byte[0];
  }

  /**
   * Check the colour of a pixel from the map image to determine the country.
   * <br/>
   * Other than the special cases, the colours are looked up using the web service the first
   * time they are found.
   * @return Locations or null if the bitmap can't answer.
   */
  protected List<Location> getFromBitmap(double lat, double lng) {
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

      case NOTHING:
        return Collections.EMPTY_LIST;

      default:
        if (!colourKey.containsKey(colour)) {
          locations = geocodeService.get(lat, lng, null, null);
          // Don't store this if there aren't any locations.
          if (locations.size() == 0) {
            LOG.error("For colour {} (LL {},{}; pixel {},{}) the webservice gave zero locations.", hex, lat, lng, x, y);
          } else {
            if (LOG.isInfoEnabled()) LOG.info("New colour {} (LL {},{}; pixel {},{}); remembering as {}", hex, lat, lng, x, y, joinLocations(locations));
            colourKey.put(colour, locations);
          }
        } else {
          locations = colourKey.get(colour);
          if (LOG.isDebugEnabled()) LOG.debug("Known colour {} (LL {},{}; pixel {},{}) is {}", hex, lat, lng, x, y, joinLocations(locations));
        }
    }

    return locations;
  }

  private String joinLocations(List<Location> loc) {
    return loc.stream().map(l -> l.getId()).distinct().collect(Collectors.joining(", "));
  }
}
