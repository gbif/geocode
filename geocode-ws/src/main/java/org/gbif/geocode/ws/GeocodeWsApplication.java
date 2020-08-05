package org.gbif.geocode.ws;

import org.gbif.geocode.ws.layers.AbstractBitmapCachedLayer;
import org.gbif.geocode.ws.layers.EezLayer;
import org.gbif.geocode.ws.layers.Gadm0Layer;
import org.gbif.geocode.ws.layers.Gadm1Layer;
import org.gbif.geocode.ws.layers.Gadm2Layer;
import org.gbif.geocode.ws.layers.Gadm3Layer;
import org.gbif.geocode.ws.layers.GeolocateCentroidsLayer;
import org.gbif.geocode.ws.layers.IhoLayer;
import org.gbif.geocode.ws.layers.PoliticalLayer;
import org.gbif.geocode.ws.layers.SeaVoXLayer;
import org.gbif.geocode.ws.layers.WgsrpdLayer;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.management.MBeanServer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

// TODO: check there's no unneeded dependencies like ES or rabbit

@SpringBootApplication
@MapperScan("org.gbif.geocode.ws.persistence.mapper")
@ServletComponentScan
@ComponentScan(
    basePackages = {
      "org.gbif.geocode.ws.layers",
      "org.gbif.geocode.ws.monitoring",
      "org.gbif.geocode.ws.persistence",
      "org.gbif.geocode.ws.resource",
      "org.gbif.geocode.ws.service"
    })
public class GeocodeWsApplication {
  public static void main(String[] args) {
    SpringApplication.run(GeocodeWsApplication.class, args);
  }

  @Bean
  public List<AbstractBitmapCachedLayer> layers(
      PoliticalLayer politicalLayer,
      EezLayer eezLayer,
      Gadm0Layer gadm0Layer,
      Gadm1Layer gadm1Layer,
      Gadm2Layer gadm2Layer,
      Gadm3Layer gadm3Layer,
      IhoLayer ihoLayer,
      SeaVoXLayer seaVoxLayer,
      WgsrpdLayer wgsrpdLayer,
      GeolocateCentroidsLayer geolocateCentroidsLayer) {

    List<AbstractBitmapCachedLayer> layers = new ArrayList<>();
    layers.add(politicalLayer);
    layers.add(eezLayer);
    layers.add(gadm0Layer);
    layers.add(gadm1Layer);
    layers.add(gadm2Layer);
    layers.add(gadm3Layer);
    layers.add(ihoLayer);
    layers.add(seaVoxLayer);
    layers.add(wgsrpdLayer);
    layers.add(geolocateCentroidsLayer);

    return Collections.unmodifiableList(layers);
  }

  // TODO: check if Springs provides support for this
  @Bean
  public MBeanServer mBeanServer() {
    return ManagementFactory.getPlatformMBeanServer();
  }
}
