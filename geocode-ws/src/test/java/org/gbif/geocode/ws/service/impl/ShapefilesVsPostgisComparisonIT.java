package org.gbif.geocode.ws.service.impl;

import org.gbif.geocode.api.model.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import au.org.ala.layers.intersect.ComplexRegion;

/**
 * Compare results (accuracy) between PostGIS and ShapeFile geocoders.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = GeocoderIntegrationTestsConfiguration.class)
@Disabled
public class ShapefilesVsPostgisComparisonIT {
  final MyBatisGeocoder myBatisGeocoder;
  final ShapefileGeocoder shapefileGeocoder;

  List<String> layers = new ArrayList<>();
  int i = 0;

  @Autowired
  public ShapefilesVsPostgisComparisonIT(MyBatisGeocoder myBatisGeocoder, ShapefileGeocoder shapefileGeocoder) {
    this.myBatisGeocoder = myBatisGeocoder;
    this.shapefileGeocoder = shapefileGeocoder;
  }

  @Test
  public void compareVariousPoliticalQueries() {
    layers.clear();
    layers.add("Political");

    List<double[]> points = new ArrayList<>();
    points.add(new double[]{-26.867118294961116, 32.130339342292245}); // Just outside MZ
    points.add(new double[]{-28.73952617955809, 28.278228795828138}); // Just inside Lesotho
    points.add(new double[]{-23.26833477330808, 33.53656009465627}); // Middle of Mozambique
    points.add(new double[]{60.2954261485896, 25.525862842197768}); // Finland

    for (double[] point : points) {
      check(point[0], point[1], false);
    }
  }

  @Test
  public void compareVariousEezQueries() {
    layers.clear();
    layers.add("EEZ");

    List<double[]> points = new ArrayList<>();
    points.add(new double[]{-53.37113659640561, -73.32594171138219}); // Chile EEZ (I think)
    points.add(new double[]{-51.524367727596626, -57.16043853997107}); // FK/AR EEZ
    points.add(new double[]{14.142595200379944, -77.29781383340294}); // CO/JM EEZ
    points.add(new double[]{80.73046019781083, -78.02028435656064}); // Canada EEZ
    points.add(new double[]{67.78450648724953, -109.05597414852102}); // Canada EEZ 2
    points.add(new double[]{60.40749059677498, 22.158713066700017}); // Finland
    points.add(new double[]{61.00346895410195, -147.92568394223184}); // Alaska EEZ
    points.add(new double[]{59.00003632575556, 178.88992354530086}); // US/RU joint regime (US before RU)
    points.add(new double[]{70.29634012819375, 20.305876112895362}); // Norway EEZ (I think)

    for (double[] point : points) {
      check(point[0], point[1], false);
    }
  }

  @Test
  @Disabled // The data is fine, except the IDs etc are different.
  public void compareVariousCentroidsQueries() {
    layers.clear();
    layers.add("Centroids");

    List<double[]> points = new ArrayList<>();
    points.add(new double[]{1.59435, 10.49347}); // Equatorial Guinea centroid

    for (double[] point : points) {
      check(point[0], point[1], false);
    }
  }

  @Test
  public void compareVariousGadmQueries() {
    layers.clear();
    layers.add("GADM");

    List<double[]> points = new ArrayList<>();
    points.add(new double[]{-20.87899757475735,-62.752318771018494});
    points.add(new double[]{-80.21296894330146,50.52462835664676});
    points.add(new double[]{71.39213139654245,-28.738038021484726});
    points.add(new double[]{66.69399063140037,147.04773355029});
    points.add(new double[]{-79.60440128137665,62.31539587253485});
    points.add(new double[]{44.25933775704945,97.0534597022787});
    points.add(new double[]{53.21785201252206,88.84700552614464});
    points.add(new double[]{52.9313560477876,122.71514675874727});
    points.add(new double[]{-74.72108973271868,42.498406709271535});
    points.add(new double[]{-75.7882187778486,-140.36844990373686});
    points.add(new double[]{-8.129791605459658, -61.50063994508639});

    for (double[] point : points) {
      check(point[0], point[1], false);
    }
  }

  /**
   * I think these errors are problems with the PostGIS bitmap cache.
   */
  //@Test
  public void compareVariousQueries() {
    layers.clear();
    layers.add("Political");
    layers.add("EEZ");

    List<double[]> points = new ArrayList<>();
    points.add(new double[]{1.041618756336291, 140.76331408205158});
    points.add(new double[]{12.3174042901762, -79.50913724439015});
    points.add(new double[]{-13.370538040001136, -174.41342633213756});
    points.add(new double[]{15.321034019551846, -60.45502326257859});
    points.add(new double[]{15.810625961743952, -16.586794133294433});
    points.add(new double[]{-18.601315049127606, 173.5228805539682});
    points.add(new double[]{18.765770693201205, 107.1112122738221});
    points.add(new double[]{19.085197502181543, 107.10951785533837});
    points.add(new double[]{-19.39552817844654, -176.61992081619593});
    points.add(new double[]{-19.44477339702513, -176.62405259786777});
    points.add(new double[]{-19.730197744755444, -157.74158500929198});
    points.add(new double[]{19.730420045673966, 97.96177816074146});
    points.add(new double[]{-21.562105046460857, -156.2714834696214});
    points.add(new double[]{-23.508334444221205, -156.16913193345826});
    points.add(new double[]{-23.538284511835798, -156.16661503050483});
    points.add(new double[]{27.757082862246108, 87.70195289830946});
    points.add(new double[]{29.007384917819095, 95.2937547471866});
    points.add(new double[]{32.99451579915551, 32.578521376062156});
    points.add(new double[]{41.76101872099596, -10.19890433904618});
    points.add(new double[]{44.95342980496699, -73.72180387350798});
    points.add(new double[]{44.96045622139687, -72.57291584241652});
    points.add(new double[]{49.20708803117398, 109.52028219965445});
    points.add(new double[]{49.21018536318988, 109.58778195443085});
    points.add(new double[]{51.51003824164192, 27.970298339528398});
    points.add(new double[]{-51.960305461843994, -70.20718170347382});
    points.add(new double[]{-51.96316680059603, -70.23017147266165});
    points.add(new double[]{-51.96399690854205, -70.19282422678327});
    points.add(new double[]{54.810107308056445, 67.6489923499226});
    points.add(new double[]{56.30860295899109, -10.015957706642808});
    points.add(new double[]{56.31132123822104, -10.092972564775437});
    points.add(new double[]{56.31272152325957, -9.537260291357114});
    points.add(new double[]{56.31333973106632, -9.806211494846167});
    points.add(new double[]{60.16315280704771, -6.507505065305679});
    points.add(new double[]{6.158837276132559, 165.4706662481301});
    points.add(new double[]{6.161912397770806, 165.47218446005684});
    points.add(new double[]{68.51231553414081, 24.920866587291357});
    points.add(new double[]{70.81959915221739, 32.859966176500336});
    points.add(new double[]{74.46324340706806, 25.49868490931047});
    points.add(new double[]{-8.527030413648163, -154.86581860535435});
    points.add(new double[]{8.749530850892285, 135.91334123964162});
    points.add(new double[]{-9.511075495429864, 142.01194310596043});

    for (double[] point : points) {
      check(point[0], point[1], false);
    }
  }

  @Test
  public void compareRandomQueries() {
    int count = 10_000;

    layers.clear();
    layers.add("Political");
    layers.add("EEZ");
    layers.add("Continent");
    layers.add("GADM3210");
    layers.add("IHO");
    layers.add("WGSRPD");

    for (int i = 0; i < count; i++) {
      double latitude = Math.random() * 180 - 90;
      double longitude = Math.random() * 360 - 180;

      check(latitude, longitude, false);
    }
  }

  private void check(double latitude, double longitude, boolean debug) {
    System.out.println(i + "> Checking "+latitude+", "+longitude);

    ComplexRegion.debug = debug;
    boolean fail = false;

    List<Location> p = myBatisGeocoder.get(latitude, longitude, 0.05, null, layers).stream().sorted().collect(Collectors.toList());
    List<Location> s = shapefileGeocoder.get(latitude, longitude, 0.05, null, layers).stream().distinct().sorted().collect(Collectors.toList());

    if (debug) {
      if (p.size() == s.size()) {
        for (int j = 0; j < p.size(); j++) {
          System.out.println(i + " PG[" + j + "]" + p.get(j));
          System.out.println(i + " SF[" + j + "]" + s.get(j));
        }

      } else {
        for (int j = 0; j < p.size(); j++) {
          System.out.println(i + " PG[" + j + "]" + p.get(j));
          if (j % 5 == 4) System.out.println("-");
        }
        System.out.println();
        for (int j = 0; j < s.size(); j++) {
          System.out.println(i + " SF[" + j + "]" + s.get(j));
          if (j % 5 == 4) System.out.println("-");
        }
      }
    }

    String detail = i+": "+latitude+", "+longitude+ " https://api.gbif.org/v1/geocode/reverse?lat="+latitude+"&lng="+longitude;
    if (s.size() != p.size()) {
      if (debug) {
        System.out.println("Size problem for "+" https://api.gbif.org/v1/geocode/reverse?lat="+latitude+"&lng="+longitude);
      }
      fail = true;
    }
    for (int j = 0; j < p.size() && j < s.size(); j++) {
      if (!p.get(j).getType().equals(s.get(j).getType())) {
        if (debug) {
          System.out.println("Type equality problem for "+j+" "+detail);
        }
        fail = true;
      }
      if (!p.get(j).getId().equals(s.get(j).getId())) {
        if (debug) {
          System.out.println("Id problem for "+j+" "+detail);
        }
        fail = true;
      }
      if (Math.abs(p.get(j).getDistance() - s.get(j).getDistance()) > 0.0000001) {
        if (debug) {
          System.out.println("Distance problem for "+j+" "+detail);
        }
        fail = true;
      }
    }

    if (fail) {
      if (debug) {
        Assertions.fail("Difference between PostGIS and ShapeFile responses");
      } else {
        check(latitude, longitude, true);
      }
    }

    i++;
  }
}
