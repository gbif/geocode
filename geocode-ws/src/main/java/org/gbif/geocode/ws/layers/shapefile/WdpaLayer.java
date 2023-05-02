package org.gbif.geocode.ws.layers.shapefile;

import org.gbif.geocode.ws.layers.Bitmap;

import au.org.ala.layers.intersect.SimpleShapeFile;

public class WdpaLayer extends AbstractMultiShapefileLayer {
  private static final String WDPA_NAME = "WDPA";
  private static final String WDPA_SOURCE = "https://www.protectedplanet.net/";
  private static final String[] WDPA_COLUMNS = new String[]{"id", "name", "isoCountry"};
  private static final String WDPA_BITMAP = "wdpa.png";
  private static final int WDPA_MAX_LOCATIONS = 100;

  /*
   * The WDPA shapefile is over 2GiB, so must be split into several shapefiles.
   *
   * Modifying layer-store to read Geopackages looks like a very large task.  Modifying it to read files larger than
   * 2GiB but less than the 4GiB shapefile limit would be OK, then this would be two files rather than three.
   */
  public WdpaLayer(String root) {
    super(new AbstractShapefileLayer[]{
        new AbstractShapefileLayer(new SimpleShapeFile(root + "wdpa_1", WDPA_COLUMNS), Bitmap.class.getResourceAsStream(WDPA_BITMAP), WDPA_MAX_LOCATIONS) {
          @Override
          public String name() {
            return WDPA_NAME;
          }

          @Override
          public String source() {
            return WDPA_SOURCE;
          }
        },

        new AbstractShapefileLayer(new SimpleShapeFile(root + "wdpa_2", WDPA_COLUMNS), Bitmap.class.getResourceAsStream(WDPA_BITMAP), WDPA_MAX_LOCATIONS) {
          @Override
          public String name() {
            return WDPA_NAME;
          }

          @Override
          public String source() {
            return WDPA_SOURCE;
          }
        },

        new AbstractShapefileLayer(new SimpleShapeFile(root + "wdpa_3", WDPA_COLUMNS), Bitmap.class.getResourceAsStream(WDPA_BITMAP), WDPA_MAX_LOCATIONS) {
          @Override
          public String name() {
            return WDPA_NAME;
          }

          @Override
          public String source() {
            return WDPA_SOURCE;
          }
        }},
      Bitmap.class.getResourceAsStream(WDPA_BITMAP),
      WDPA_MAX_LOCATIONS);
  }

  @Override
  public String name() {
    return WDPA_NAME;
  }

  @Override
  public String source() {
    return WDPA_SOURCE;
  }
}
