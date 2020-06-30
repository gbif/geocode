package org.gbif.geocode.ws.model;

import org.apache.ibatis.annotations.Param;
import org.gbif.geocode.api.model.Location;

import java.util.List;

/**
 * Used by MyBatis to provide a typed interface to the mapped queries.
 */
public interface LocationMapper {
  List<Location> queryLayers(@Param("lng") double point, @Param("lat") double latitude, @Param("distance") double distance, @Param("layers") List<String> layers);
}
