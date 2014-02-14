package org.gbif.geocode.ws.monitoring;

import java.util.concurrent.atomic.AtomicLong;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exposing some statistics for use in JMX.
 */
@Singleton
public class GeocodeWsStatistics implements GeocodeWsStatisticsMBean {

  private static final Logger LOG = LoggerFactory.getLogger(GeocodeWsStatistics.class);

  private final AtomicLong goodRequestCount = new AtomicLong(0);
  private final AtomicLong badRequestCount = new AtomicLong(0);
  private final AtomicLong cacheHits = new AtomicLong(0);
  private final AtomicLong dbHits = new AtomicLong(0);
  private final AtomicLong politicalHits = new AtomicLong(0);
  private final AtomicLong eezHits = new AtomicLong(0);
  private final AtomicLong noHits = new AtomicLong(0);
  private final AtomicLong totalResults = new AtomicLong(0);

  @Inject
  public void register(MBeanServer server) {
    try {
      server.registerMBean(this, new ObjectName("Geocode WS:type=Statistics"));
    } catch (Exception e) {
      LOG.warn("Exception caught registering JMX MBean", e);
    }
  }

  public void goodRequest() {
    goodRequestCount.incrementAndGet();
  }

  @Override
  public long getTotalGoodRequests() {
    return goodRequestCount.get();
  }

  public void badRequest() {
    badRequestCount.incrementAndGet();
  }

  @Override
  public long getTotalBadRequests() {
    return badRequestCount.get();
  }

  public void servedFromCache() {
    cacheHits.incrementAndGet();
  }

  @Override
  public long getTotalServedFromCache() {
    return cacheHits.get();
  }

  public void servedFromDatabase() {
    dbHits.incrementAndGet();
  }

  @Override
  public long getTotalServedFromDatabase() {
    return dbHits.get();
  }

  public void foundPolitical() {
    politicalHits.incrementAndGet();
  }

  @Override
  public long getTotalPoliticalHits() {
    return politicalHits.get();
  }

  public void foundEez() {
    eezHits.incrementAndGet();
  }

  @Override
  public long getTotalEezHits() {
    return eezHits.get();
  }

  public void noResult() {
    noHits.incrementAndGet();
  }

  @Override
  public long getTotalNoResults() {
    return noHits.get();
  }

  public void resultSize(int size) {
    totalResults.addAndGet(size);
  }

  @Override
  public double getAverageResultSize() {
    double totalHits = politicalHits.get() + eezHits.get();
    return totalResults.get() / totalHits;
  }

  @Override
  public void resetStats() {
    goodRequestCount.set(0);
    badRequestCount.set(0);
    cacheHits.set(0);
    dbHits.set(0);
    politicalHits.set(0);
    eezHits.set(0);
    noHits.set(0);
    totalResults.set(0);
  }

}
