package org.gbif.geocode.ws.layers;

import org.gbif.geocode.api.model.Location;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

import au.org.ala.layers.intersect.SimpleShapeFile;

public class Gadm3210Layer extends AbstractShapefileLayer {
  String[] name = new String[4];

  public Gadm3210Layer() {
    super(Gadm3210Layer.class.getResourceAsStream("gadm3210.png"), 4);
  }

  public Gadm3210Layer(SimpleShapeFile simpleShapeFile) {
    super(simpleShapeFile, Gadm3210Layer.class.getResourceAsStream("gadm3210.png"), 4);
    name[0] = "GADM0";
    name[1] = "GADM1";
    name[2] = "GADM2";
    name[3] = "GADM3";
  }

  @Override
  public String name() {
    return "GADM3210";
  }

  @Override
  public String source() {
    return "http://gadm.org/";
  }

  @Override
  public List<Location> convertResultToLocation(List<ImmutablePair<Integer, Double>> countryValues) {
    List<Location> locations = new ArrayList<>();

    for (Pair<Integer, Double> countryValue : countryValues) {
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
        if (!Strings.isNullOrEmpty(gid[i])) {
          boolean already = false;
          for (Location e : locations) {
            if (e.getId().equals(gid[i])) {
              already = true;
              if (e.getDistance() > countryValue.getRight()) {
                // Replace
                e.setDistance(countryValue.getRight());
              } else {
                break;
              }
            }
          }
          if (!already) {
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
      }
    }

    return locations;
  }
}
