package org.gbif.geocode.ws.model;

import org.apache.ibatis.annotations.Param;
import org.gbif.geocode.api.model.Location;

import java.util.List;

/**
 * Used by MyBatis to provide a typed interface to the mapped queries.
 * <p/>
 * Allows to get Mapbox Vector Tiles or SVG shapes from PostgreSQL.
 */
public interface TileMapper {

  List<SvgShape> svgPolitical();
  List<SvgShape> svgEez();
  List<SvgShape> svgGadm();
  List<SvgShape> svgGadm5();
  List<SvgShape> svgGadm4();
  List<SvgShape> svgGadm3();
  List<SvgShape> svgGadm2();
  List<SvgShape> svgGadm1();
  List<SvgShape> svgGadm0();
  List<SvgShape> svgIho();
  List<SvgShape> svgSeaVoX();
  List<SvgShape> svgWgsrpd();
  List<SvgShape> svgGeolocateCentroids();

  Tile tilePolitical(@Param("b1") double b1, @Param("b2") double b2, @Param("b3") double b3, @Param("b4") double b4);
  Tile tileEez(@Param("b1") double b1, @Param("b2") double b2, @Param("b3") double b3, @Param("b4") double b4);
  Tile tileGadm(@Param("b1") double b1, @Param("b2") double b2, @Param("b3") double b3, @Param("b4") double b4);
  Tile tileGadm5(@Param("b1") double b1, @Param("b2") double b2, @Param("b3") double b3, @Param("b4") double b4);
  Tile tileGadm4(@Param("b1") double b1, @Param("b2") double b2, @Param("b3") double b3, @Param("b4") double b4);
  Tile tileGadm3(@Param("b1") double b1, @Param("b2") double b2, @Param("b3") double b3, @Param("b4") double b4);
  Tile tileGadm2(@Param("b1") double b1, @Param("b2") double b2, @Param("b3") double b3, @Param("b4") double b4);
  Tile tileGadm1(@Param("b1") double b1, @Param("b2") double b2, @Param("b3") double b3, @Param("b4") double b4);
  Tile tileGadm0(@Param("b1") double b1, @Param("b2") double b2, @Param("b3") double b3, @Param("b4") double b4);
  Tile tileIho(@Param("b1") double b1, @Param("b2") double b2, @Param("b3") double b3, @Param("b4") double b4);
  Tile tileSeaVoX(@Param("b1") double b1, @Param("b2") double b2, @Param("b3") double b3, @Param("b4") double b4);
  Tile tileWgsrpd(@Param("b1") double b1, @Param("b2") double b2, @Param("b3") double b3, @Param("b4") double b4);
  Tile tileGeolocateCentroids(@Param("b1") double b1, @Param("b2") double b2, @Param("b3") double b3, @Param("b4") double b4);

  void toCache(@Param("layer") String layer, @Param("z") int z, @Param("x") long x, @Param("y") long y, @Param("tile") Tile tile, @Param("timeTaken") long timeTaken);
  Tile fromCache(@Param("layer") String layer, @Param("z") int z, @Param("x") long x, @Param("y") long y);
}
