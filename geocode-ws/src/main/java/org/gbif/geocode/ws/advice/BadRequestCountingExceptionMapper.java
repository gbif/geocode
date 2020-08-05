package org.gbif.geocode.ws.advice;

import org.gbif.geocode.ws.monitoring.GeocodeWsStatistics;
import org.gbif.ws.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BadRequestCountingExceptionMapper {

  private static final Logger LOG =
      LoggerFactory.getLogger(BadRequestCountingExceptionMapper.class);

  private final GeocodeWsStatistics statistics;

  @Autowired
  public BadRequestCountingExceptionMapper(GeocodeWsStatistics statistics) {
    this.statistics = statistics;
  }

  @ExceptionHandler(WebApplicationException.class)
  public void handleWebApplicationException(WebApplicationException exception) {
    statistics.badRequest();
    if (exception.getCause() instanceof NumberFormatException) {
      NumberFormatException nfe = (NumberFormatException) exception.getCause();
      LOG.warn("Bad request: {}", nfe.getMessage());
    } else {
      LOG.warn("Bad request caught", exception);
    }
    // we rethrow it to be handled by our common exception handler
    throw exception;
  }
}
