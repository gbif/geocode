package org.gbif.geocode.ws.service.impl;

import org.gbif.api.vocabulary.Country;
import org.gbif.common.parsers.CountryParser;
import org.gbif.common.parsers.core.ParseResult;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.ws.model.LocationMapper;
import org.gbif.geocode.ws.monitoring.GeocodeWsStatistics;
import org.gbif.geocode.ws.service.Geocoder;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * Implementation of {@link Geocoder} using MyBatis to search for results.
 */
@Singleton
public class MyBatisGeocoder implements Geocoder {

  private final static CountryParser COUNTRY_PARSER = CountryParser.getInstance();

  private final SqlSessionFactory sqlSessionFactory;

  private final GeocodeWsStatistics statistics;

  // Distance are calculated using the approximation that 1 degree is ~ 111 kilometers

  // 0.001 * 11100 KM = 111 m
  private final static double DEFAULT_DISTANCE = 0.001d;

  // 5 KM/111 KM is approximately 0.0450 degrees
  private final static double KM5_DISTANCE = 0.045d;

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
      locations = locationMapper.listPolitical(point, DEFAULT_DISTANCE);

      // only go to Marine EEZ for no results as it is slow
      if (locations.isEmpty()) {
        locations = locationMapper.listEez(point, DEFAULT_DISTANCE);
        if (locations.isEmpty()) {
          Optional<Location> optLocation = tryWithin(point, KM5_DISTANCE, locationMapper);
          if (optLocation.isPresent()) {
            statistics.foundWithing5Km();
            locations.add(optLocation.get());
          } else {
            statistics.noResult();
          }
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
   * Tries to find a single location within a distance.
   * Searches the political and eez locations within a distance and if both results returns the same location for the
   * first result, it is returned.
   */
  private Optional<Location> tryWithin(String point, double distance, LocationMapper locationMapper) {
    List<Location> politicalLocations = locationMapper.listPolitical(point, distance);
    List<Location> eeZlocations = locationMapper.listEez(point, distance);
    if (politicalLocations.size() == 1 && eeZlocations.size() == 1) {
      ParseResult<Country> result = COUNTRY_PARSER.parse(eeZlocations.get(0).getTitle());
      Location location = politicalLocations.get(0);
      String localLocationIsoCode = location.getIsoCountryCode2Digit();
      if (result.isSuccessful() && localLocationIsoCode != null
        && localLocationIsoCode.equalsIgnoreCase(result.getPayload().getIso2LetterCode())) {
        return Optional.of(location);
      }
    }
    return Optional.absent();
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

    for (Location loc : locations) {
      if (loc.getIsoCountryCode2Digit() == null) {
        ParseResult<Country> result = COUNTRY_PARSER.parse(loc.getTitle());
        if (result.isSuccessful()) {
          loc.setIsoCountryCode2Digit(result.getPayload().getIso2LetterCode());
        }
      }
    }
  }
}
