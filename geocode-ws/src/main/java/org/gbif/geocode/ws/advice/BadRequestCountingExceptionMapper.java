package org.gbif.geocode.ws.advice;

import org.gbif.ws.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BadRequestCountingExceptionMapper {

  private static final Logger LOG =
      LoggerFactory.getLogger(BadRequestCountingExceptionMapper.class);

  public BadRequestCountingExceptionMapper() {
  }

  @ExceptionHandler(WebApplicationException.class)
  public ResponseEntity handleWebApplicationException(WebApplicationException ex) {
    if (ex.getCause() instanceof NumberFormatException) {
      NumberFormatException nfe = (NumberFormatException) ex.getCause();
      LOG.warn("Bad request: {}", nfe.getMessage());
    } else {
      LOG.warn("Bad request caught", ex);
    }
    return ResponseEntity.status(ex.getStatus())
        .contentType(ex.getContentType())
        .body(ex.getMessage());
  }
}
