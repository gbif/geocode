package org.gbif.geocode.ws.persistence.mapper;

import org.apache.ibatis.annotations.Param;

import org.gbif.geocode.ws.model.SvgShape;
import org.gbif.geocode.ws.model.Tile;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Used by MyBatis to provide a typed interface to the mapped queries.
 *
 * <p>Allows to get GeoJSON from PostgreSQL.
 */
@Repository
public interface GeoJsonMapper {

  String[] geoJsonPolitical(@Param("id") String id);
  String[] geoJsonCentroids(@Param("id") String id);
  String[] geoJsonContinent(@Param("id") String id);
  String[] geoJsonGadm5(@Param("id") String id);
  String[] geoJsonGadm4(@Param("id") String id);
  String[] geoJsonGadm3(@Param("id") String id);
  String[] geoJsonGadm2(@Param("id") String id);
  String[] geoJsonGadm1(@Param("id") String id);
  String[] geoJsonGadm0(@Param("id") String id);
  String[] geoJsonIho(@Param("id") String id);
  String[] geoJsonIucn(@Param("id") String id);
  String[] geoJsonWgsrpd(@Param("id") String id);

}
