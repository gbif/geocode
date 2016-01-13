package org.gbif.geocode.ws.resource;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sun.jersey.api.Responses;

public class OffWorldException extends WebApplicationException {

  /**
   * Create a HTTP 400 (Bad Request) exception with a JSON body.
   */
  public OffWorldException() {
    super(Response.status(Responses.CLIENT_ERROR).entity("[]").type(MediaType.APPLICATION_JSON).build());
  }

}
