package org.gbif.geocode.ws.layers.shapefile;

import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.ws.layers.Bitmap;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import au.org.ala.layers.intersect.SimpleShapeFile;

public class PoliticalLayer extends AbstractShapefileLayer {
  public PoliticalLayer(String root) {
    super(new SimpleShapeFile(root + "political_subdivided", new String[]{"id", "name", "isoCountry"}),
      Bitmap.class.getResourceAsStream("political.png"),
      3);
  }

  @Override
  public String name() {
    return "Political";
  }

  @Override
  public String source() {
    return "https://www.marineregions.org/";
  }

  @Override
  public List<Location> resultToLocation(Pair<Integer, Double> countryValue, double latitude) {
    List<Location> locations = new ArrayList<>();

    String id = idColumnLookup[idColumnIndex[countryValue.getLeft()]];
    String title = titleColumnLookup[titleColumnIndex[countryValue.getLeft()]];
    String isoCode = isoCodeColumnLookup[isoCodeColumnIndex[countryValue.getLeft()]];

    String[] iso = isoCode.split(" ");

    for (String i : iso) {
      Location l = new Location();
      l.setType(name());
      l.setSource(source());
      l.setId("http://marineregions.org/mrgid/" + id);
      l.setTitle(title);
      l.setIsoCountryCode2Digit(i);
      l.setDistance(countryValue.getRight());
      locations.add(l);
    }

    return locations;
  }
}
