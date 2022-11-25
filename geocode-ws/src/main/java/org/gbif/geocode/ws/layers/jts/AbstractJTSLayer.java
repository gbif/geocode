package org.gbif.geocode.ws.layers.jts;

import org.gbif.geocode.api.cache.AbstractBitmapCachedLayer;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.ws.persistence.mapper.LocationMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

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
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * A reverse geocoder backed by distance calculations using JTS, with data
 * sourced from a PostGIS database and indexed into memory on startup.
 */
public abstract class AbstractJTSLayer extends AbstractBitmapCachedLayer {
  private Logger LOG = LoggerFactory.getLogger(getClass());
  private long queries = 0;

  // Results will only be returned if they are within this distance of the query point.
  private final double minimumDistanceCutoffMeters;

  static GeometryFactory geomFact = new GeometryFactory();
  static CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;

  final SpatialIndex index = new STRtree();

  // At very high latitudes, an offset of a few km is many degrees.  A point at -89.9°, 140° must
  // still match a centroid at -90°, 0°, so just check all polar coordinates in this case.
  // (There are <10000 occurrences affected, so performance is irrelevant.)
  final List<PreparedGeometry> polarPoints = new ArrayList<>();

  // GeodeticCalculator is not thread safe, so have some ready.
  private final LinkedBlockingQueue<GeodeticCalculator> calculators = new LinkedBlockingQueue<>(50);
  private final AtomicInteger calculatorCount = new AtomicInteger(0);

  AbstractJTSLayer(LocationMapper locationMapper, InputStream bitmap, int maxLocations,
                   double minimumDistanceCutoffMeters) {
    super(bitmap, maxLocations);
    this.minimumDistanceCutoffMeters = minimumDistanceCutoffMeters;

    for (PointLocation p : locationMapper.fetchPointLocations()) {
      Point c = geomFact.createPoint(new Coordinate(p.longitude, p.latitude));
      c.setUserData(p);
      index.insert(c.getEnvelopeInternal(), PreparedGeometryFactory.prepare(c));

      if (isPolar(p.latitude)) {
        polarPoints.add(PreparedGeometryFactory.prepare(c));
      }
    }

    // Just a sanity check, we have Coordinate(longitude, latitude) below.
    assert CRS.getAxisOrder(DefaultGeographicCRS.WGS84).equals(CRS.AxisOrder.EAST_NORTH);
  }

  boolean isPolar(double latitude) {
    return (-85 > latitude || latitude > 85);
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
  protected List<Location> queryDatasource(double latitude, double longitude, double uncertaintyDegrees) {
    double factor = 111_319.491 * Math.cos(Math.toRadians(latitude));

    // Minimum distance in degrees based on latitude, with a minimum of the query argument,
    // used to query the index.
    double minimumDistanceCutoffDegrees = minimumDistanceCutoffMeters / factor;
    double distanceCutoffDegrees = Math.max(minimumDistanceCutoffDegrees, uncertaintyDegrees);

    // Minimum distance in metres based on latitude, with a minimum of the configured default for this instance,
    // used to return (or not) matches.
    double uncertaintyMeters = uncertaintyDegrees * factor;
    double distanceCutoffMeters = Math.max(minimumDistanceCutoffMeters, uncertaintyMeters);

    // Query the index of points for intersections of a circle with radius distanceCutoffDegrees
    Coordinate query = new Coordinate(longitude, latitude);
    Point queryPoint = geomFact.createPoint(query);
    // Add a small factor to account for spheroidal/ellipsoidal calculation differences, or more likely
    // me not really understanding the maths.
    Geometry queryCircle = queryPoint.buffer(1.5 * distanceCutoffDegrees);
    List<PreparedGeometry> candidates = index.query(queryCircle.getEnvelopeInternal());
    // LOG.debug("Found {} candidates with cutoff {}° ({}).", candidates.size(), distanceCutoffDegrees, queryCircle.getEnvelopeInternal());

    List<Location> locations = new ArrayList<>();
    for (PreparedGeometry p : candidates) {
      double distance = calculateDistance(query, p.getGeometry().getCoordinate());
      // LOG.debug("Candidate {} distance {}m", p.getGeometry().getCentroid(), distance);
      if (distance <= distanceCutoffMeters) {
        locations.addAll(resultToLocation(ImmutablePair.of((PointLocation)p.getGeometry().getUserData(), distance), latitude));
      }
    }

    // Also check any polar coordinates, if necessary.
    if (isPolar(latitude)) {
      // LOG.debug("Polar {} candidates with latitude {}°.", polarPoints.size(), latitude);
      for (PreparedGeometry p : polarPoints) {
        double distance = calculateDistance(query, p.getGeometry().getCoordinate());
        // LOG.debug("Polar candidate {} distance {}m", p.getGeometry().getCentroid(), distance);
        if (distance <= distanceCutoffMeters) {
          locations.addAll(resultToLocation(ImmutablePair.of((PointLocation)p.getGeometry().getUserData(), distance), latitude));
        }
      }
    }

    if ((++queries % 10_000) == 0) {
      LOG.info("{} did {} JTS queries.", name(), queries);
    }

    return locations;
  }

  private double calculateDistance(Coordinate from, Coordinate to) {
    GeodeticCalculator gc = null;
    try {
      gc = calculators.poll();
      if (gc == null) {
        LOG.info("Creating {}. new GeodeticCalculator", calculatorCount.incrementAndGet());
        gc = new GeodeticCalculator(crs);
      }

      gc.setStartingPosition(JTS.toDirectPosition(from, crs));
      gc.setDestinationPosition(JTS.toDirectPosition(to, crs));

      return gc.getOrthodromicDistance();
    } catch (ArithmeticException | TransformException e) {
      LOG.error("Error calculating distance", e);
      return Double.MAX_VALUE;
    } finally {
      try {
        calculators.put(gc);
      } catch (InterruptedException e) {
      }
    }
  }
}
