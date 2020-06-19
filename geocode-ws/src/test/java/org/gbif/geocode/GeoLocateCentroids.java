//package org.gbif.geocode;
//
//import org.apache.http.HttpHost;
//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.client.utils.URIBuilder;
//import org.apache.http.conn.routing.HttpRoute;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
//import org.apache.http.util.EntityUtils;
//import org.gbif.api.util.iterables.Iterables;
//import org.gbif.api.vocabulary.Country;
//import org.geotools.feature.FeatureCollection;
//import org.geotools.feature.FeatureIterator;
//import org.geotools.geojson.feature.FeatureJSON;
//import org.geotools.geojson.geom.GeometryJSON;
//import org.junit.Test;
//import org.opengis.feature.Feature;
//import org.opengis.feature.simple.SimpleFeature;
//import org.opengis.feature.type.FeatureType;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.io.Reader;
//import java.io.StringReader;
//import java.io.Writer;
//import java.math.BigDecimal;
//import java.net.URI;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Objects;
//import java.util.Optional;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//public class GeoLocateCentroids {
//
//  class GeoLocateCentroid implements Comparable<GeoLocateCentroid> {
//
//    Country country;
//    String point;
//
//    GeoLocateCentroid(Country country, String point) {
//      this.country = country;
//      this.point = point;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//      if (this == o) return true;
//      if (o == null || getClass() != o.getClass()) return false;
//      GeoLocateCentroid that = (GeoLocateCentroid) o;
//      return country == that.country &&
//        Objects.equals(point, that.point);
//    }
//
//    @Override
//    public int hashCode() {
//      return Objects.hash(country, point);
//    }
//
//    @Override
//    public String toString() {
//      return "GeoLocateCentroid{" +
//        "country=" + country.getIso2LetterCode() +
//        ", point='" + point + '\'' +
//        '}';
//    }
//
//    @Override
//    public int compareTo(GeoLocateCentroid o) {
//      return this.country.compareTo(o.country);
//    }
//  }
//
//  @Test
//  public void fetchGeoLocateCentroids() throws Exception {
//
//    PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
//    connManager.setMaxPerRoute(new HttpRoute(new HttpHost("geo-locate.org", 80)), 3);
//
//    CloseableHttpClient httpclient = HttpClients.custom()
//      .setConnectionManager(connManager)
//      .build();
//
//    PrintWriter fileOut = new PrintWriter(new File("geoLocateCentroids.txt"));
//
//    List<GeoLocateCentroid> centroids = Arrays.stream(Country.values()).parallel().limit(400).map(
//      c -> {
//        Set<GeoLocateCentroid> points = new HashSet<>();
//        try {
//          URI uri = new URIBuilder()
//            .setScheme("http")
//            .setHost("geo-locate.org")
//            .setPath("/webservices/geolocatesvcv2/glcwrap.aspx")
//            .setParameter("country", c.getTitle())
//            .setParameter("locality", c.getTitle())
//            .build();
//
//          HttpGet httpget = new HttpGet(uri);
//
//          System.err.println("executing request " + httpget.getURI());
//
//          try (CloseableHttpResponse response = httpclient.execute(httpget)) {
//            String jsonContent = EntityUtils.toString(response.getEntity());
//
//            GeometryJSON geometryJson = new GeometryJSON(15);
//            FeatureJSON featureJson = new FeatureJSON(geometryJson);
//            Reader stringReader = null;
//            @SuppressWarnings("rawtypes")
//            FeatureCollection<FeatureType, SimpleFeature> featureCollection = null;
//
//            try {
//              stringReader = new StringReader(jsonContent);
//              featureCollection = featureJson.readFeatureCollection(stringReader);
//            } finally {
//              stringReader.close();
//            }
//
//            FeatureIterator<SimpleFeature> iterator = featureCollection.features();
//            while (iterator.hasNext()) {
//              SimpleFeature f = iterator.next();
//              points.add(new GeoLocateCentroid(c, f.getDefaultGeometry().toString()));
//            }
//
//            return points;
//          } catch (IOException e) {
//            e.printStackTrace();
//          } finally {
//
//          }
//        } catch (Exception e) {
//
//        }
//
//        return points;
//      }
//    ).flatMap(Collection::stream).sorted().collect(Collectors.toList());
//
//    centroids.stream().forEach(
//      System.out::println
//    );
//
//    centroids.stream().forEach(
//      fileOut::println
//    );
//
//    fileOut.close();
//  }
//}
