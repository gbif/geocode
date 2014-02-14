package org.gbif.geocode.ws.monitoring;

/**
 * JMX interface required to be exposed.
 */
public interface GeocodeWsStatisticsMBean {

  long getTotalGoodRequests();

  long getTotalBadRequests();

  long getTotalServedFromCache();

  long getTotalServedFromDatabase();

  long getTotalPoliticalHits();

  long getTotalEezHits();

  long getTotalNoResults();

  double getAverageResultSize();

  void resetStats();
}
