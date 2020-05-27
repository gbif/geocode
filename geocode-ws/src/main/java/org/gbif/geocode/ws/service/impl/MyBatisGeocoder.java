package org.gbif.geocode.ws.service.impl;

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

import java.util.Collection;
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

  private final List<AbstractBitmapCachedLayer> layers;

  // Distance are calculated using the approximation that 1 degree is ~ 111 kilometers

  // The default distance is chosen at ~5km to allow for gaps between land and sea (political and EEZ)
  // and to cope with the inaccuracies introduced in the simplified datasets.

  // 0.05° ~= 5.55 km
  private final static double DEFAULT_DISTANCE = 0.05d;

  @Inject
  public MyBatisGeocoder(SqlSessionFactory sqlSessionFactory, GeocodeWsStatistics statistics, List<AbstractBitmapCachedLayer> layers) {
    this.sqlSessionFactory = sqlSessionFactory;
    this.statistics = statistics;
    this.layers = layers;
  }

  /**
   * Simple get candidates by point.
   */
  @Override
  public Collection<Location> get(Double lat, Double lng, Double uncertaintyInDegrees) {
    Collection<Location> locations;

    if (uncertaintyInDegrees == null) uncertaintyInDegrees = DEFAULT_DISTANCE;
    uncertaintyInDegrees = Math.max(uncertaintyInDegrees, DEFAULT_DISTANCE);

    final double unc = uncertaintyInDegrees;

    try (SqlSession session = sqlSessionFactory.openSession()) {
      LocationMapper locationMapper = session.getMapper(LocationMapper.class);

      locations = layers.stream()
        .flatMap(
          layer -> layer.get(locationMapper, lat, lng, unc).stream()
        )
        .collect(Collectors.toList());

      statistics.servedFromDatabase();
    }

    statistics.resultSize(locations.size());
    return locations;
  }

  @Override
  public byte[] bitmap() {
    throw new UnsupportedOperationException("Not implemented.");
  }
}
