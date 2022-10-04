package org.gbif.geocode.ws.service.impl;

import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.ws.layers.AbstractShapefileLayer;
import org.gbif.geocode.ws.layers.CentroidsLayer;
import org.gbif.geocode.ws.layers.ContinentLayer;
import org.gbif.geocode.ws.layers.GadmLayer;
import org.gbif.geocode.ws.layers.IhoLayer;
import org.gbif.geocode.ws.layers.PoliticalLayer;
import org.gbif.geocode.ws.layers.WgsrpdLayer;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import au.org.ala.layers.intersect.SimpleShapeFile;

/**
 * This is a port of the functionality in geocode to using ALA's layer-store
 * (https://github.com/AtlasOfLivingAustralia/layers-store) SimpleShapeFile for intersections.
 *
 * @see SimpleShapeFile
 */
@Service
public class ShapefileGeocoder implements GeocodeService {
  private static final Logger LOG = LoggerFactory.getLogger(ShapefileGeocoder.class);

  private final Map<String,AbstractShapefileLayer> layers = new HashMap<>();

  private static final double DEFAULT_DISTANCE = 0.05d;

  public ShapefileGeocoder(@Value("${spring.shapefiles.root}") String root, @Value("${spring.shapefiles.enabled}") List<String> enabled) {

    synchronized (this) {
      LOG.info("Loading shapefiles {} from {}", enabled, root);

      String[] columns = new String[]{"id", "name", "isoCountry"};

      Class<AbstractShapefileLayer>[] availableLayers = new Class[]{
        PoliticalLayer.class,
        ContinentLayer.class,
        CentroidsLayer.class,
        IhoLayer.class,
        WgsrpdLayer.class,
        GadmLayer.class
      };

      for (Class class_ : availableLayers) {
        String name = class_.getSimpleName();

        if (enabled == null || enabled.isEmpty() || enabled.contains(name)) {
          try {
            Constructor<AbstractShapefileLayer> c = class_.getDeclaredConstructor(String.class);
            AbstractShapefileLayer layer = c.newInstance(new Object[]{root});
            layers.put(layer.name(), layer);
          } catch (Exception e) {
            throw new RuntimeException("Error loading layer "+name+" from "+root);
          }
        } else {
          LOG.info("Not loading {}, because it is not enabled in the configuration.", name);
        }
      }

      if (layers.isEmpty()) {
        throw new RuntimeException("No layers loaded!  Couldn't load any shapefile layers from «"+root+"»");
      }

      LOG.info("Available layers are {}", layers.keySet());
    }
  }

  /** Simple get candidates by point. */
  @Override
  public List<Location> get(
    Double lat, Double lng, Double uncertaintyDegrees, Double uncertaintyMeters) {
    return get(lat, lng, uncertaintyDegrees, uncertaintyMeters, Collections.EMPTY_LIST);
  }

  /** Simple get candidates by point. */
  @Override
  public List<Location> get(
    Double lat,
    Double lng,
    Double uncertaintyDegrees,
    Double uncertaintyMeters,
    List<String> useLayers) {
    List<Location> locations = new ArrayList<>();

    // Convert uncertainty in metres to degrees, approximating the Earth as a sphere.
    if (uncertaintyMeters != null) {
      uncertaintyDegrees = uncertaintyMeters / (111_319.491 * Math.cos(Math.toRadians(lat)));
      LOG.debug("{}m uncertainty converted to {}°", uncertaintyMeters, uncertaintyDegrees);
    }

    // Set a default uncertainty, if none was specified.
    if (uncertaintyDegrees == null) {
      uncertaintyDegrees = DEFAULT_DISTANCE;
    }

    // Increase to the default distance if needed, to account for inaccuracies in the layer data.
    // TODO: per layer
    uncertaintyDegrees = Math.max(uncertaintyDegrees, DEFAULT_DISTANCE);

    // For each layer, check the bitmap cache, then query the shapefile if needed.
    for (Map.Entry<String,AbstractShapefileLayer> entry : layers.entrySet()) {
      if (useLayers.isEmpty() || useLayers.contains(entry.getKey())) {
        AbstractShapefileLayer layer = entry.getValue();
        List<Location> found = null;
        final boolean query;
        if (uncertaintyDegrees <= DEFAULT_DISTANCE) {
          found = layer.checkBitmap(lat, lng);
          query = (found == null);
        } else {
          query = true;
        }

        if (query) {
          found = layer.lookup(lat, lng, uncertaintyDegrees);
          layer.putBitmap(lat, lng, found);
        }
        locations.addAll(found);
      }
    }

    Collections.sort(locations, Location.DISTANCE_COMPARATOR);
    return locations;
  }

  @Override
  public byte[] bitmap() {
    throw new UnsupportedOperationException("Not implemented.");
  }
}
