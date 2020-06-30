package org.gbif.geocode.ws.service.impl;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.ws.layers.AbstractBitmapCachedLayer;
import org.gbif.geocode.ws.model.LocationMapper;
import org.gbif.geocode.ws.monitoring.GeocodeWsStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link GeocodeService} using MyBatis to search for results.
 */
@Singleton
public class MyBatisGeocoder implements GeocodeService {
  public static Logger LOG = LoggerFactory.getLogger(MyBatisGeocoder.class);

  private final SqlSessionFactory sqlSessionFactory;

  private final GeocodeWsStatistics statistics;

  private final List<AbstractBitmapCachedLayer> availableLayers;
  private final List<String> availableLayerNames;

  // The default distance is chosen at ~5km to allow for gaps between land and sea (political and EEZ)
  // and to cope with the inaccuracies introduced in the simplified datasets.
  // 0.05° ~= 5.55 km
  private final static double DEFAULT_DISTANCE = 0.05d;

  @Inject
  public MyBatisGeocoder(SqlSessionFactory sqlSessionFactory, GeocodeWsStatistics statistics, List<AbstractBitmapCachedLayer> layers) {
    this.sqlSessionFactory = sqlSessionFactory;
    this.statistics = statistics;
    this.availableLayers = layers;

    this.availableLayerNames = layers.stream().map(l -> l.name()).collect(Collectors.toList());
    LOG.info("Available (and thus default) layers are {}", this.availableLayerNames);
  }

  /**
   * Simple get candidates by point.
   */
  @Override
  public Collection<Location> get(Double lat, Double lng, Double uncertaintyDegrees, Double uncertaintyMeters) {
    return get(lat, lng, uncertaintyDegrees, uncertaintyMeters, Collections.EMPTY_LIST);
  }

  /**
   * Simple get candidates by point.
   */
  @Override
  public Collection<Location> get(Double lat, Double lng, Double uncertaintyDegrees, Double uncertaintyMeters, List<String> useLayers) {
    List<Location> locations = new ArrayList<>();

    // Convert uncertainty in metres to degrees, approximating the Earth as a sphere.
    if (uncertaintyMeters != null) {
      uncertaintyDegrees = uncertaintyMeters / (111_319.491 * Math.cos(Math.toRadians(lat)));
      LOG.debug("{}m uncertainty converted to {}°", uncertaintyMeters, uncertaintyDegrees);
    }

    // Set a default uncertainty, if none was specified.
    if (uncertaintyDegrees == null) {
      uncertaintyDegrees = DEFAULT_DISTANCE;
    }

    // Increase to the default distance if needed, to account for inaccuracies in the layer data.
    uncertaintyDegrees = Math.max(uncertaintyDegrees, DEFAULT_DISTANCE);

    final double unc = uncertaintyDegrees;

    try (SqlSession session = sqlSessionFactory.openSession()) {
      LocationMapper locationMapper = session.getMapper(LocationMapper.class);

      List<String> toQuery = new ArrayList<>();

      // Check the bitmaps
      for (AbstractBitmapCachedLayer layer : availableLayers) {
        if (useLayers.isEmpty() || useLayers.contains(layer.name())) {
          if (unc <= DEFAULT_DISTANCE) {
            Collection<Location> found = layer.checkBitmap(lat, lng);
            if (found == null) {
              toQuery.add(layer.name());
            } else {
              locations.addAll(found);
            }
          } else {
            toQuery.add(layer.name());
          }
        }
      }

      if (!toQuery.isEmpty()) {
        // Retrieve anything the bitmaps couldn't help with, or didn't yet have
        Stopwatch sw = Stopwatch.createStarted();
        List<Location> queriedLocations = locationMapper.queryLayers(lng, lat, uncertaintyDegrees, toQuery);
        locations.addAll(queriedLocations);
        LOG.info("Time for {} is {}", toQuery, sw.stop());

        // Push values into the bitmap caches
        for (AbstractBitmapCachedLayer layer : availableLayers) {
          if (toQuery.contains(layer.name())) {
            List<Location> found = new ArrayList<>();
            found.addAll(queriedLocations.stream().filter(l -> l.getType().equals(layer.name())).collect(Collectors.toList()));
            layer.putBitmap(lat, lng, found);
          }
        }

        statistics.servedFromDatabase();
      }
    }

    statistics.resultSize(locations.size());
    return locations;
  }

  @Override
  public byte[] bitmap() {
    throw new UnsupportedOperationException("Not implemented.");
  }
}
