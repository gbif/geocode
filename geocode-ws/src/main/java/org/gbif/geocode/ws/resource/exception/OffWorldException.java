package org.gbif.geocode.ws.resource.exception;

import org.gbif.ws.WebApplicationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public class OffWorldException extends WebApplicationException {

  /** Create an HTTP 400 (Bad Request) exception with a JSON body. */
  public OffWorldException() {
    super("[]", HttpStatus.BAD_REQUEST, MediaType.APPLICATION_JSON);
  }

  /** Create an HTTP 400 (Bad Request) exception with a JSON body. */
  public OffWorldException(String message) {
    super(message, HttpStatus.BAD_REQUEST, MediaType.APPLICATION_JSON);
  }
}
