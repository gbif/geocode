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

  long getTotalNoResults();

  long getWithin5KmHits();

  double getAverageResultSize();

  void resetStats();
}
