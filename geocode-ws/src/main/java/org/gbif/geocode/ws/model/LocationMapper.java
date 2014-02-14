package org.gbif.geocode.ws.model;

import org.gbif.geocode.api.model.Location;

import java.util.List;

/**
 * Used by MyBatis to provide a typed interface to the mapped queries.
 * <p/>
 * Allows to get Locations from two different sources. Political being faster than EEZ.
 */
public interface LocationMapper {

  List<Location> listPolitical(String point);

  List<Location> listEez(String point);

}
