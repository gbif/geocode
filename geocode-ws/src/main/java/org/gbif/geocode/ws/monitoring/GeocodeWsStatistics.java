package org.gbif.geocode.ws.monitoring;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

/** Exposing some statistics for use in JMX. */
@Component
@ManagedResource(objectName = "Geocode WS:type=Statistics")
public class GeocodeWsStatistics implements GeocodeWsStatisticsMBean {

  private final AtomicLong goodRequestCount = new AtomicLong(0);
  private final AtomicLong badRequestCount = new AtomicLong(0);
  private final AtomicLong cacheHits = new AtomicLong(0);
  private final AtomicLong dbHits = new AtomicLong(0);
  private final AtomicLong politicalHits = new AtomicLong(0);
  private final AtomicLong within5KmHits = new AtomicLong(0);
  private final AtomicLong eezHits = new AtomicLong(0);
  private final AtomicLong noHits = new AtomicLong(0);
  private final AtomicLong totalResults = new AtomicLong(0);

  @ManagedOperation
  public void goodRequest() {
    goodRequestCount.incrementAndGet();
  }

  @ManagedAttribute
  @Override
  public long getTotalGoodRequests() {
    return goodRequestCount.get();
  }

  @ManagedOperation
  public void badRequest() {
    badRequestCount.incrementAndGet();
  }

  @ManagedAttribute
  @Override
  public long getTotalBadRequests() {
    return badRequestCount.get();
  }

  @ManagedOperation
  public void servedFromCache() {
    cacheHits.incrementAndGet();
  }

  @ManagedAttribute
  @Override
  public long getTotalServedFromCache() {
    return cacheHits.get();
  }

  @ManagedOperation
  public void servedFromDatabase() {
    dbHits.incrementAndGet();
  }

  @ManagedAttribute
  @Override
  public long getTotalServedFromDatabase() {
    return dbHits.get();
  }

  @ManagedOperation
  public void foundPolitical() {
    politicalHits.incrementAndGet();
  }

  @ManagedAttribute
  @Override
  public long getTotalPoliticalHits() {
    return politicalHits.get();
  }

  @ManagedOperation
  public void foundEez() {
    eezHits.incrementAndGet();
  }

  @ManagedAttribute
  @Override
  public long getTotalEezHits() {
    return eezHits.get();
  }

  @ManagedOperation
  public void foundWithin5Km() {
    within5KmHits.incrementAndGet();
  }

  @ManagedAttribute
  @Override
  public long getWithin5KmHits() {
    return within5KmHits.get();
  }

  @ManagedOperation
  public void noResult() {
    noHits.incrementAndGet();
  }

  @ManagedAttribute
  @Override
  public long getTotalNoResults() {
    return noHits.get();
  }

  @ManagedOperation
  public void resultSize(int size) {
    totalResults.addAndGet(size);
  }

  @ManagedOperation
  @Override
  public double getAverageResultSize() {
    double totalHits = politicalHits.get() + eezHits.get() + within5KmHits.get();
    return totalResults.get() / totalHits;
  }

  @ManagedOperation
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
    within5KmHits.set(0);
  }
}
