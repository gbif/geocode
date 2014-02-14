package org.gbif.geocode.ws.monitoring;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An ExceptionMapper that counts all {@link WebApplicationException} exceptions as bad requests in our statistics
 * bean.
 * <p/>
 * Those include all bad requests like wrong parameters or plain missing URLs (which favicon.ico belongs to)
 */
@Provider
@Singleton
public class BadRequestCountingExceptionMapper implements ExceptionMapper<WebApplicationException> {

  private final GeocodeWsStatistics statistics;

  private static final Logger LOG = LoggerFactory.getLogger(BadRequestCountingExceptionMapper.class);

  @Inject
  public BadRequestCountingExceptionMapper(GeocodeWsStatistics statistics) {
    this.statistics = statistics;
  }

  @Override
  public Response toResponse(WebApplicationException exception) {
    statistics.badRequest();
    if (exception.getCause() instanceof NumberFormatException) {
      NumberFormatException nfe = (NumberFormatException) exception.getCause();
      LOG.warn("Bad request: {}", nfe.getMessage());
    } else {
      LOG.warn("Bad request caught", exception);
    }
    return exception.getResponse();
  }
}
