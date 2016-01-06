package org.gbif.geocode.ws.service.impl;

import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.ws.service.Geocoder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link Geocoder} using a map image as a fast lookup,
 * followed (when necessary) by a database to search for results.
 */
@Singleton
public class BitmapFirstGeocoder implements Geocoder {
  public static Logger LOG = LoggerFactory.getLogger(BitmapFirstGeocoder.class);

  private final Geocoder geocoder;

  // World map image lookup
  private final BufferedImage img;
  private final static int BORDER = 0x000000;
  private final static int EEZ = 0x888888;
  private final static int INTERNATIONAL_WATER = 0xFFFFFF;
  private final int img_width;
  private final int img_height;
  private final Map<Integer, Collection<Location>> colourKey = new HashMap<>();

  @Inject
  public BitmapFirstGeocoder(@Named("Database") Geocoder geocoder) {
    this.geocoder = geocoder;

    try {
      img = ImageIO.read(this.getClass().getResourceAsStream("world.png"));
      img_height = img.getHeight();
      img_width = img.getWidth();
    } catch (IOException e) {
      throw new RuntimeException("Unable to load map image", e);
    }
  }

  /**
   * Simple get candidates by point.
   */
  @Override
  public Collection<Location> get(double lat, double lng) {
    Collection<Location> locations;

    // Check the image map for a sure location.
    locations = getFromBitmap(lat, lng);

    // If that doesn't help, use the database.
    if (locations == null) {
      locations = geocoder.get(lat, lng);
    }

    return locations;
  }

  /**
   * Check the colour of a pixel from the map image to determine the country.
   * <br/>
   * Other than the special cases, the colours are looked up in the database the first
   * time they are found.
   * @return Locations, or null if the database should be consulted.
   */
  protected Collection<Location> getFromBitmap(double lat, double lng) {
    // Convert the latitude and longitude to x,y coordinates on the image.
    // The axes are swapped, and the image's origin is the top left.
    int x = (int) (Math.round ((lng+180d)/360d*(img_width-1)));
    int y = img_height-1 - (int) (Math.round ((lat+90d)/180d*(img_height-1)));

    int colour = img.getRGB(x, y) & 0x00FFFFFF; // Ignore possible transparency.

    String hex = String.format("#%06x", colour);
    LOG.debug("LatLong {},{} has pixel {},{} with colour {}", lat, lng, x, y, hex);

    Collection<Location> locations;

    switch (colour) {
      case BORDER:
        return null;

      case EEZ:
        return null;

      case INTERNATIONAL_WATER:
        return new ArrayList<>();

      default:
        if (!colourKey.containsKey(colour)) {
          locations = geocoder.get(lat, lng);
          // Don't store this if there aren't any locations.
          if (locations.size() == 0) {
            LOG.warn("For colour {} (LL {},{}; pixel {},{}) the database gave zero locations.", hex, lat, lng, x, y);
          } else {

            // Don't store if the ISO code is -99; this code is used for some exceptional bits of territory (e.g. Baikonur Cosmodrome, the Korean DMZ).
            if ("-99".equals(locations.iterator().next().getIsoCountryCode2Digit())) {
              LOG.info("New colour {} (LL {},{}; pixel {},{}); exceptional territory of {} will not be cached", hex, lat, lng, x, y, locations.iterator().next().getTitle());
            } else {
              colourKey.put(colour, locations);
              LOG.info("New colour {} (LL {},{}; pixel {},{}); remembering as {}", hex, lat, lng, x, y, locations.iterator().next().getTitle());
            }
          }
        } else {
          locations = colourKey.get(colour);
          LOG.info("Known colour {} (LL {},{}; pixel {},{}) is {}", hex, lat, lng, x, y, locations.iterator().next().getTitle());
        }
    }

    return locations;
  }
}
