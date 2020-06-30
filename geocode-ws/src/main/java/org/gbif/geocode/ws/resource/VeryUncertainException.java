package org.gbif.geocode.ws.resource;

import com.sun.jersey.api.Responses;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class VeryUncertainException extends WebApplicationException {

  /**
   * Create a HTTP 400 (Bad Request) exception with a JSON body.
   */
  public VeryUncertainException() {
    super(Response.status(Responses.CLIENT_ERROR).entity("[]").type(MediaType.APPLICATION_JSON).build());
  }

  /**
   * Create a HTTP 400 (Bad Request) exception with a JSON body.
   */
  public VeryUncertainException(String message) {
    super(Response.status(Responses.CLIENT_ERROR).entity(message).type(MediaType.APPLICATION_JSON).build());
  }
}
