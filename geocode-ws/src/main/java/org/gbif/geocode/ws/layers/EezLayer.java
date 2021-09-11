package org.gbif.geocode.ws.layers;

import org.gbif.geocode.api.model.Location;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import au.org.ala.layers.intersect.SimpleShapeFile;

@Component
public class EezLayer extends AbstractShapefileLayer {
  public EezLayer() {
    super(EezLayer.class.getResourceAsStream("eez.png"), 3);
  }

  public EezLayer(SimpleShapeFile simpleShapeFile) {
    super(simpleShapeFile, EezLayer.class.getResourceAsStream("eez.png"), 3);
  }

  @Override
  public String name() {
    return "EEZ";
  }

  @Override
  public String source() {
    return "http://vliz.be/vmdcdata/marbound/";
  }

  @Override
  public List<Location> convertResultToLocation(List<ImmutablePair<Integer, Double>> countryValues) {
    List<Location> locations = new ArrayList<>();

    for (Pair<Integer, Double> countryValue : countryValues) {
      String id = idColumnLookup[idColumnIndex[countryValue.getLeft()]];
      String title = titleColumnLookup[titleColumnIndex[countryValue.getLeft()]];
      String isoCode = isoCodeColumnLookup[isoCodeColumnIndex[countryValue.getLeft()]];

      String[] iso = isoCode.split(" ");
      for (String i : iso) {
        Location l = new Location();
        l.setType(name());
        l.setSource(source());
        l.setId("http://marineregions.org/mrgid/"+id);
        l.setTitle(title);
        l.setIsoCountryCode2Digit(i);
        l.setDistance(countryValue.getRight());
        locations.add(l);
      }
    }

    return locations;
  }
}
