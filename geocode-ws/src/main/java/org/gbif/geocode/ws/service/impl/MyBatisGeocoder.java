package org.gbif.geocode.ws.service.impl;

import org.gbif.common.parsers.ParseResult;
import org.gbif.common.parsers.countryname.CountryNameParser;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.ws.model.LocationMapper;
import org.gbif.geocode.ws.monitoring.GeocodeWsStatistics;
import org.gbif.geocode.ws.service.Geocoder;

import java.util.Collection;
import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * Implementation of {@link Geocoder} using MyBatis to search for results.
 */
@Singleton
public class MyBatisGeocoder implements Geocoder {

  private final SqlSessionFactory sqlSessionFactory;

  private final GeocodeWsStatistics statistics;

  @Inject
  public MyBatisGeocoder(SqlSessionFactory sqlSessionFactory, GeocodeWsStatistics statistics) {
    this.sqlSessionFactory = sqlSessionFactory;
    this.statistics = statistics;
  }

  /**
   * Simple get candidates by point.
   */
  @Override
  public Collection<Location> get(double lat, double lng) {
    Collection<Location> locations;

    SqlSession session = sqlSessionFactory.openSession();
    try {
      LocationMapper locationMapper = session.getMapper(LocationMapper.class);
      String point = "POINT(" + lng + ' ' + lat + ')';

      // try using the terrestrial table as it is far quicker
      locations = locationMapper.listPolitical(point);

      // only go to Marine EEZ for no results as it is slow
      if (locations.isEmpty()) {
        locations = locationMapper.listEez(point);
        if (locations.isEmpty()) {
          statistics.noResult();
        } else {
          statistics.foundEez();
          fixEezIsoCodes(locations);
        }
      } else {
        statistics.foundPolitical();
      }

      statistics.servedFromDatabase();
    } finally {
      session.close();
    }

    statistics.resultSize(locations.size());
    return locations;
  }

  /**
   * Some EEZ locations won't have iso countries, so need to fill them in based on returned title.
   * <p/>
   * This will change the objects in place.
   *
   * @param locations list of locations to fix.
   */
  private static void fixEezIsoCodes(@Nullable Collection<Location> locations) {
    if (locations == null || locations.isEmpty()) {
      return;
    }

    CountryNameParser countryParser = CountryNameParser.getInstance();
    for (Location loc : locations) {
      if (loc.getIsoCountryCode2Digit() == null) {
        ParseResult<String> result = countryParser.parse(loc.getTitle());
        if (result.isSuccessful()) {
          loc.setIsoCountryCode2Digit(result.getPayload());
        }
      }
    }
  }
}
