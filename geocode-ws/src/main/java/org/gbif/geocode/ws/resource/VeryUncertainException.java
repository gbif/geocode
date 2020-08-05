package org.gbif.geocode.ws.resource;

import org.gbif.ws.WebApplicationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public class VeryUncertainException extends WebApplicationException {

  /** Create a HTTP 400 (Bad Request) exception with a JSON body. */
  public VeryUncertainException() {
    super("[]", HttpStatus.BAD_REQUEST, MediaType.APPLICATION_JSON);
  }

  /** Create a HTTP 400 (Bad Request) exception with a JSON body. */
  public VeryUncertainException(String message) {
    super(message, HttpStatus.BAD_REQUEST, MediaType.APPLICATION_JSON);
  }
}
