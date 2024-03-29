package org.gbif.geocode.ws.layers.shapefile;

import org.gbif.geocode.api.cache.AbstractBitmapCachedLayer;
import org.gbif.geocode.api.model.Location;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.org.ala.layers.intersect.SimpleShapeFile;

/**
 * A shapefile-backed layer with a bitmap cache.
 *
 * Assumes shapefiles have three columns (id, title, isoCode), as generated by database/import.sh.
 */
public abstract class AbstractShapefileLayer extends AbstractBitmapCachedLayer {
  private Logger LOG = LoggerFactory.getLogger(getClass());
  final SimpleShapeFile simpleShapeFile;
  private long queries = 0;

  String[] idColumnLookup;
  String[] titleColumnLookup;
  String[] isoCodeColumnLookup;
  int[] idColumnIndex;
  int[] titleColumnIndex;
  int[] isoCodeColumnIndex;

  AbstractShapefileLayer(SimpleShapeFile simpleShapeFile, InputStream bitmap, int maxLocations) {
    super(bitmap, maxLocations);
    this.simpleShapeFile = simpleShapeFile;
    idColumnLookup = simpleShapeFile.getColumnLookup(0);
    titleColumnLookup = simpleShapeFile.getColumnLookup(1);
    isoCodeColumnLookup = simpleShapeFile.getColumnLookup(2);
    idColumnIndex = simpleShapeFile.getColumnIdxs("ID");
    titleColumnIndex = simpleShapeFile.getColumnIdxs("NAME");
    isoCodeColumnIndex = simpleShapeFile.getColumnIdxs("ISOCOUNTRY");
  }

  /**
   * Convert the shapefile query result into Locations.
   */
  List<Location> convertResultToLocation(List<ImmutablePair<Integer, Double>> countryValues, double latitude) {
    List<Location> locations = new ArrayList<>();

    for (Pair<Integer, Double> countryValue : countryValues) {
      List<Location> ls = resultToLocation(countryValue, latitude);

      for (Location l : ls) {
        boolean additionalHit = false;
        for (Location e : locations) {
          // We may get multiple results for a MultiPolygon; return only the nearest hit.
          // We may also get multiple genuine results, e.g. for GADM and Political.
          if (Objects.equals(e.getId(), l.getId())
            && Objects.equals(e.getIsoCountryCode2Digit(), l.getIsoCountryCode2Digit())) {
            additionalHit = true;
            if (e.getDistance() > countryValue.getRight()) {
              // Replace
              e.setDistance(countryValue.getRight());
            }
          }
        }
        if (!additionalHit) {
          locations.add(l);
        }
      }
    }

    return locations;
  }

  /**
   * Convert a single shapefile result into a Location.
   *
   * Non-trivial layers will need to override this.
   */
  List<Location> resultToLocation(Pair<Integer, Double> countryValue, double latitude) {
    String id = idColumnLookup[idColumnIndex[countryValue.getLeft()]];
    String title = titleColumnLookup[titleColumnIndex[countryValue.getLeft()]];
    String isoCode = isoCodeColumnLookup[isoCodeColumnIndex[countryValue.getLeft()]];

    Location l = new Location();
    l.setType(name());
    l.setSource(source());
    l.setId(id);
    l.setTitle(title);
    l.setIsoCountryCode2Digit(isoCode);
    l.setDistance(countryValue.getRight());

    return Collections.singletonList(l);
  }

  /**
   * Query the shapefile.
   */
  @Override
  protected List<Location> queryDatasource(double latitude, double longitude, double uncertainty) {
    List<ImmutablePair<Integer, Double>> intersections = simpleShapeFile.intersectInt(longitude, latitude, uncertainty);
    List<Location> locations = convertResultToLocation(intersections, latitude);

    if ((++queries % 10_000) == 0) {
      LOG.info("{} did {} shapefile queries.", name(), queries);
    }

    return locations;
  }
}
