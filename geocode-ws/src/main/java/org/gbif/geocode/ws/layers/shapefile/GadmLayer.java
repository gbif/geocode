package org.gbif.geocode.ws.layers.shapefile;

import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.ws.layers.Bitmap;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import au.org.ala.layers.intersect.SimpleShapeFile;

public class GadmLayer extends AbstractShapefileLayer {
  final String[] name = new String[4];

  public GadmLayer(String root) {
    super(new SimpleShapeFile(root + "gadm_subdivided", new String[]{"gid_0", "gid_1", "gid_2", "gid_3", "name_0", "name_1", "name_2", "name_3", "isoCountry"}),
      Bitmap.class.getResourceAsStream("gadm3210.png"),
      4);
    name[0] = "GADM0";
    name[1] = "GADM1";
    name[2] = "GADM2";
    name[3] = "GADM3";
  }

  @Override
  public double adjustUncertainty(double uncertaintyDegrees, double latitude) {
    // This is a very rough approximation of
    // return Math.min(10.0, 250_000 / (111_319.491 * Math.cos(Math.toRadians(latitude))));
    if (latitude >= 78) {
      return Math.min(10.0, uncertaintyDegrees);
    } else if (latitude >= 74) {
      return Math.min(8.0, uncertaintyDegrees);
    } else if (latitude >= 68) {
      return Math.min(6.0, uncertaintyDegrees);
    } else if (latitude >= 64) {
      return Math.min(5.0, uncertaintyDegrees);
    } else if (latitude >= 56) {
      return Math.min(4.0, uncertaintyDegrees);
    } else if (latitude >= 42) {
      return Math.min(3.0, uncertaintyDegrees);
    } else {
      return Math.min(2.5, uncertaintyDegrees);
    }
  }

  @Override
  public String name() {
    return "GADM";
  }

  @Override
  public String source() {
    return "http://gadm.org/";
  }

  @Override
  public List<Location> resultToLocation(Pair<Integer, Double> countryValue, double latitude) {
    List<Location> locations = new ArrayList<>();

    String[] gid = new String[4];
    String[] title = new String[4];
    gid[0] = simpleShapeFile.getColumnLookup(0)[simpleShapeFile.getColumnIdxs("GID_0")[countryValue.getLeft()]];
    gid[1] = simpleShapeFile.getColumnLookup(1)[simpleShapeFile.getColumnIdxs("GID_1")[countryValue.getLeft()]];
    gid[2] = simpleShapeFile.getColumnLookup(2)[simpleShapeFile.getColumnIdxs("GID_2")[countryValue.getLeft()]];
    gid[3] = simpleShapeFile.getColumnLookup(3)[simpleShapeFile.getColumnIdxs("GID_3")[countryValue.getLeft()]];
    title[0] = simpleShapeFile.getColumnLookup(4)[simpleShapeFile.getColumnIdxs("NAME_0")[countryValue.getLeft()]];
    title[1] = simpleShapeFile.getColumnLookup(5)[simpleShapeFile.getColumnIdxs("NAME_1")[countryValue.getLeft()]];
    title[2] = simpleShapeFile.getColumnLookup(6)[simpleShapeFile.getColumnIdxs("NAME_2")[countryValue.getLeft()]];
    title[3] = simpleShapeFile.getColumnLookup(7)[simpleShapeFile.getColumnIdxs("NAME_3")[countryValue.getLeft()]];
    String isoCode = simpleShapeFile.getColumnLookup(8)[simpleShapeFile.getColumnIdxs("ISOCOUNTRY")[countryValue.getLeft()]];

    for (int i = 0; i < gid.length; i++) {
      if (!(gid[i] == null || gid[i].length() == 0)) {
        Location l = new Location();
        l.setType(name[i]);
        l.setSource(source());
        l.setId(gid[i]);
        l.setTitle(title[i]);
        l.setIsoCountryCode2Digit(isoCode);
        l.setDistance(countryValue.getRight());
        locations.add(l);
      }
    }

    return locations;
  }
}
