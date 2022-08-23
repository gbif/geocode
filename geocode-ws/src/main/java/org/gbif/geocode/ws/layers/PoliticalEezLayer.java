package org.gbif.geocode.ws.layers;

import org.gbif.geocode.api.model.Location;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import au.org.ala.layers.intersect.SimpleShapeFile;

@Component
public class PoliticalEezLayer extends AbstractShapefileLayer {
  public PoliticalEezLayer() {
    super(PoliticalEezLayer.class.getResourceAsStream("politicalEez.png"), 3);
  }

  public PoliticalEezLayer(SimpleShapeFile simpleShapeFile) {
    super(simpleShapeFile, PoliticalEezLayer.class.getResourceAsStream("politicalEez.png"), 3);
  }

  @Override
  public String name() {
    return "PoliticalEEZ";
  }

  @Override
  public String source() {
    return "https://www.marineregions.org/";
  }

  @Override
  public List<Location> resultToLocation(Pair<Integer, Double> countryValue) {
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
