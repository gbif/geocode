package org.gbif.geocode.ws.model;

import org.apache.ibatis.annotations.Param;
import org.gbif.geocode.api.model.Location;

import java.util.List;

/**
 * Used by MyBatis to provide a typed interface to the mapped queries.
 * <p/>
 * Allows to get Mapbox Vector Tiles from PostgreSQL.
 */
public interface TileMapper {

  Tile tilePolitical(@Param("b1") double b1, @Param("b2") double b2, @Param("b3") double b3, @Param("b4") double b4);
  Tile tileEez(@Param("b1") double b1, @Param("b2") double b2, @Param("b3") double b3, @Param("b4") double b4);

  void toCache(@Param("layer") String layer, @Param("z") int z, @Param("x") long x, @Param("y") long y, @Param("tile") Tile tile, @Param("timeTaken") long timeTaken);
  Tile fromCache(@Param("layer") String layer, @Param("z") int z, @Param("x") long x, @Param("y") long y);
}
