package org.gbif.geocode.ws.layers.jts;

import org.gbif.api.vocabulary.Country;
import org.gbif.geocode.api.cache.AbstractBitmapCachedLayer;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.ws.persistence.mapper.LocationMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A reverse geocoder backed by distance calculations using JTS, with data
 * sourced from a PostGIS database and loaded on startup.
 */
public abstract class AbstractJTSLayer extends AbstractBitmapCachedLayer {
  private Logger LOG = LoggerFactory.getLogger(getClass());

  CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
  List<PointLocation> centroids = new ArrayList<>();
  private long queries = 0;

  // Results will only be returned if they are within this distance of the query point.
  private final double distanceCutoffMeters;

  AbstractJTSLayer(LocationMapper locationMapper, InputStream bitmap, int maxLocations,
                   double distanceCutoffMeters) {
    super(bitmap, maxLocations);
    this.distanceCutoffMeters = distanceCutoffMeters;

    for (PointLocation p : locationMapper.fetchPointLocations()) {
      centroids.add(p);
    }

    //assert CRS.getAxisOrder(DefaultGeographicCRS.WGS84).equals(CRS.AxisOrder.NORTH_EAST);
  }

  List<Location> resultToLocation(Pair<PointLocation, Double> countryValue, double latitude) {
    Location l = new Location();
    l.setType(name());
    l.setSource(countryValue.getLeft().source);
    l.setId(countryValue.getLeft().id);
    l.setTitle(countryValue.getLeft().title);
    l.setIsoCountryCode2Digit(countryValue.getLeft().isoCountryCode2Digit);
    l.setDistanceMeters(countryValue.getRight());
    l.calculateDistanceDegreesFromMeters(latitude);

    return Collections.singletonList(l);
  }

  @Override
  protected List<Location> queryDatasource(double latitude, double longitude, double uncertainty) {
    List<Location> locations = new ArrayList<>();

    for (PointLocation p : centroids) {
      // Calculate distance to arguments
      Coordinate from = (new Coordinate(longitude, latitude));
      Coordinate to   = (new Coordinate(p.longitude, p.latitude));

      double distance = calculateDistance(from, to);
        if (distance <= distanceCutoffMeters) {
          locations.addAll(resultToLocation(ImmutablePair.of(p, distance), latitude));
        }
    }

    if ((++queries % 10_000) == 0) {
      LOG.info("{} did {} JTS queries.", name(), queries);
    }

    return locations;
  }

  private double calculateDistance(Coordinate from, Coordinate to) {
    try {
      GeodeticCalculator gc = new GeodeticCalculator(crs);
      gc.setStartingPosition(JTS.toDirectPosition(from, crs));
      gc.setDestinationPosition(JTS.toDirectPosition(to, crs));

      return gc.getOrthodromicDistance();
    } catch (TransformException e) {
      LOG.error("Error calculating distance", e);
      return Double.MAX_VALUE;
    }
  }
}
