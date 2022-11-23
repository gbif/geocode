package org.gbif.geocode.ws.persistence.mapper;

import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.ws.layers.jts.PointLocation;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/** Used by MyBatis to provide a typed interface to the mapped queries. */
@Repository
public interface LocationMapper {
  List<Location> queryLayers(
    @Param("lng") double point,
    @Param("lat") double latitude,
    @Param("distance") double distance,
    @Param("layers") List<String> layers);

  List<Location> queryLayer(
    @Param("lng") double point,
    @Param("lat") double latitude,
    @Param("distance") double distance,
    @Param("layer") String layer);

  List<PointLocation> fetchPointLocations();
}
