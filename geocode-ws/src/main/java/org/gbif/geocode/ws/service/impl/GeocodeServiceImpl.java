package org.gbif.geocode.ws.service.impl;

import org.gbif.geocode.api.cache.AbstractBitmapCachedLayer;
import org.gbif.geocode.api.cache.Layer;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.ws.layers.combined.MrLayer;
import org.gbif.geocode.ws.layers.combined.PGMrLayer;
import org.gbif.geocode.ws.layers.jts.AbstractJTSLayer;
import org.gbif.geocode.ws.layers.postgis.AbstractPostGISLayer;
import org.gbif.geocode.ws.layers.postgis.PGCentroidsLayer;
import org.gbif.geocode.ws.layers.postgis.PGContinentLayer;
import org.gbif.geocode.ws.layers.postgis.PGGadmLayer;
import org.gbif.geocode.ws.layers.postgis.PGIucnLayer;
import org.gbif.geocode.ws.layers.postgis.PGMrEezLayer;
import org.gbif.geocode.ws.layers.postgis.PGMrIhoLayer;
import org.gbif.geocode.ws.layers.postgis.PGMrLmeLayer;
import org.gbif.geocode.ws.layers.postgis.PGPoliticalLayer;
import org.gbif.geocode.ws.layers.postgis.PGWgsrpdLayer;
import org.gbif.geocode.ws.layers.shapefile.AbstractShapefileLayer;
import org.gbif.geocode.ws.persistence.mapper.LocationMapper;

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

/**
 * Provides a GeocodeService with three backends:
 * - the ALA's layer-store (https://github.com/AtlasOfLivingAustralia/layers-store)
 * - PostGIS
 * - JTS, reading point data from PostGIS
 */
@Service
public class GeocodeServiceImpl implements GeocodeService {
  private static final Logger LOG = LoggerFactory.getLogger(GeocodeServiceImpl.class);

  private final Map<String, Layer> layers = new HashMap<>();
  private final List<String> defaultLayers = new ArrayList<>();

  // The default distance was originally chosen at ~5km to allow for gaps between land and sea in the simplified datasets.
  // 0.05° ~= 5.55 km
  private static final double MINIMUM_UNCERTAINTY_DEGREES = 0.05d;

  // The maximum uncertainty is chosen for performance.  It's not strictly correct behaviour, but if a point really is
  // more than 10° uncertain it seems unlikely that it is more than 10° from something as large as a country.
  private static final double MAXIMUM_UNCERTAINTY_DEGREES = 10.0d;
  // But allow 60° at very high latitudes so we have about 100km of uncertainty.
  private static final double MAXIMUM_POLAR_UNCERTAINTY_DEGREES = 60.0d;

  public GeocodeServiceImpl(LocationMapper locationMapper,
                            @Value("${spring.shapefiles.root}") String root,
                            @Value("${spring.shapefiles.enabled}") List<String> enabled,
                            @Value("${spring.defaultLayers}") List<String> defaultLayers) {

    synchronized (this) {
      LOG.info("Enabled layers: {}", enabled);

      // 1: Load enabled shapefile layers
      Class<AbstractShapefileLayer>[] availableShapefileLayers = new Class[]{
        org.gbif.geocode.ws.layers.shapefile.ContinentLayer.class,
        org.gbif.geocode.ws.layers.shapefile.GadmLayer.class,
        org.gbif.geocode.ws.layers.shapefile.MrEezLayer.class,
        org.gbif.geocode.ws.layers.shapefile.MrIhoLayer.class,
        org.gbif.geocode.ws.layers.shapefile.MrLmeLayer.class,
        org.gbif.geocode.ws.layers.shapefile.PoliticalLayer.class,
        org.gbif.geocode.ws.layers.shapefile.WgsrpdLayer.class
      };

      for (Class class_ : availableShapefileLayers) {
        String name = class_.getSimpleName();

        if (enabled == null || enabled.isEmpty() || enabled.contains(name)) {
          try {
            Constructor<AbstractShapefileLayer> c = class_.getDeclaredConstructor(String.class);
            AbstractShapefileLayer layer = c.newInstance(root);
            layers.put(layer.name(), layer);
          } catch (Exception e) {
            throw new RuntimeException("Error loading layer "+name+" from "+root, e);
          }
        } else {
          LOG.info("Not loading {}, because it is not enabled in the configuration.", name);
        }
      }

      // 2: Load PostGIS layers
      Class<AbstractPostGISLayer>[] availablePostGISLayers = new Class[]{
        PGCentroidsLayer.class,
        PGContinentLayer.class,
        PGGadmLayer.class,
        PGIucnLayer.class,
        PGMrEezLayer.class,
        PGMrIhoLayer.class,
        PGMrLmeLayer.class,
        PGPoliticalLayer.class,
        PGWgsrpdLayer.class
      };

      for (Class class_ : availablePostGISLayers) {
        try {
          Constructor<AbstractPostGISLayer> c = class_.getDeclaredConstructor(LocationMapper.class);
          AbstractPostGISLayer layer = c.newInstance(locationMapper);
          layers.put(layer.name(), layer);
        } catch (Exception e) {
          throw new RuntimeException("Error loading layer "+class_.getName(), e);
        }
      }

      // 3: Load JTS layers
      Class<AbstractJTSLayer>[] availableJTSLayers = new Class[]{
        org.gbif.geocode.ws.layers.jts.CentroidsLayer.class
      };

      for (Class class_ : availableJTSLayers) {
        try {
          Constructor<AbstractJTSLayer> c = class_.getDeclaredConstructor(LocationMapper.class);
          AbstractJTSLayer layer = c.newInstance(locationMapper);
          layers.put(layer.name(), layer);
        } catch (Exception e) {
          throw new RuntimeException("Error loading layer "+class_.getName(), e);
        }
      }

      // 4: Initialize combined layers
      if (enabled != null && enabled.containsAll(List.of("MrEezLayer", "MrIhoLayer", "MrLmeLayer"))) {
        try {
          Layer mrLayer = new MrLayer(layers.get("MR_EEZ"), layers.get("MR_IHO"), layers.get("MR_LME"));
          layers.put(mrLayer.name(), mrLayer);
        } catch (Exception e) {
          throw new RuntimeException("Error loading combined layer MR", e);
        }
      } else {
        LOG.info("Not loading combined layer MR because dependent layers are not present.");
      }

      try {
        Layer pgMrLayer = new PGMrLayer(layers.get("PG_MR_EEZ"), layers.get("PG_MR_IHO"), layers.get("PG_MR_LME"));
        layers.put(pgMrLayer.name(), pgMrLayer);
      } catch (Exception e) {
        throw new RuntimeException("Error loading layer PG_MR", e);
      }

      // 5: Verify.
      if (layers.isEmpty()) {
        throw new RuntimeException("No layers loaded!");
      }

      LOG.info("Available layers are {}", layers.keySet().stream().sorted());

      for (String d : defaultLayers) {
        if (layers.containsKey(d)) {
          this.defaultLayers.add(d);
        } else {
          System.err.println("Default layer "+d+" is configured, but the layer doesn't exist");
          throw new RuntimeException("Default layer "+d+" is configured, but the layer doesn't exist");
        }
      }
      LOG.info("Default layers are {}", this.defaultLayers);
    }
  }

  /** Simple get candidates by point. */
  @Override
  public List<Location> get(
    Double lat, Double lng, Double uncertaintyDegrees, Double uncertaintyMeters) {
    return get(lat, lng, uncertaintyDegrees, uncertaintyMeters, defaultLayers);
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

    if (useLayers.isEmpty()) {
      useLayers = defaultLayers;
    }

    // Convert uncertainty in metres to degrees, approximating the Earth as a sphere.
    if (uncertaintyMeters != null) {
      uncertaintyDegrees = uncertaintyMeters / (111_319.491 * Math.cos(Math.toRadians(lat)));
      LOG.debug("{}m uncertainty converted to {}°", uncertaintyMeters, uncertaintyDegrees);
    }

    // Set a default uncertainty, if none was specified.
    if (uncertaintyDegrees == null) {
      uncertaintyDegrees = MINIMUM_UNCERTAINTY_DEGREES;
    }

    // Limit the maximum uncertainty.
    if (uncertaintyDegrees > MAXIMUM_UNCERTAINTY_DEGREES) {
      if (Math.abs(lat) > 85) {
        if (uncertaintyDegrees > MAXIMUM_POLAR_UNCERTAINTY_DEGREES) {
          LOG.debug("Excessive polar uncertainty {}° clamped to {}°", uncertaintyDegrees, MAXIMUM_POLAR_UNCERTAINTY_DEGREES);
          uncertaintyDegrees = MAXIMUM_POLAR_UNCERTAINTY_DEGREES;
        }
      } else {
        LOG.debug("Excessive uncertainty {}° clamped to {}°", uncertaintyDegrees, MAXIMUM_UNCERTAINTY_DEGREES);
        uncertaintyDegrees = MAXIMUM_UNCERTAINTY_DEGREES;
      }
    }

    // Increase to the default distance if needed, to account for inaccuracies in the layer data.
    // TODO: per layer
    uncertaintyDegrees = Math.max(uncertaintyDegrees, MINIMUM_UNCERTAINTY_DEGREES);

    // For each layer, check the bitmap cache, then query the shapefile if needed.
    for (Map.Entry<String,Layer> entry : layers.entrySet()) {
      if (useLayers.contains(entry.getKey())) {
        locations.addAll(entry.getValue().query(lat, lng, uncertaintyDegrees));
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
