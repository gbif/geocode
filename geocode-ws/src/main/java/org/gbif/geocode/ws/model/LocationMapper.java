package org.gbif.geocode.ws.model;

import org.gbif.geocode.api.model.Location;

import java.util.List;

import org.apache.ibatis.annotations.Param;

/**
 * Used by MyBatis to provide a typed interface to the mapped queries.
 * <p/>
 * Allows to get Locations from two different sources. Political being faster than EEZ.
 */
public interface LocationMapper {

  List<Location> listPolitical(@Param("point") String point, @Param("distance") double distance);

  List<Location> listEez(@Param("point") String point, @Param("distance") double distance);

  List<Location> listGeolocateCentroids(@Param("point") String point, @Param("distance") double distance);

  List<Location> listGadm(@Param("point") String point, @Param("distance") double distance);

  List<Location> listIho(@Param("point") String point, @Param("distance") double distance);

  List<Location> listSeaVoX(@Param("point") String point, @Param("distance") double distance);
}
