package org.gbif.geocode.ws.service.impl;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

@TestConfiguration
@SpringBootApplication
@MapperScan("org.gbif.geocode.ws.persistence.mapper")
@ComponentScan(
  basePackages = {
    "org.gbif.geocode.ws.advice",
    "org.gbif.geocode.ws.layers",
    "org.gbif.geocode.ws.monitoring",
    "org.gbif.geocode.ws.persistence",
    "org.gbif.geocode.ws.resource",
    "org.gbif.geocode.ws.service"
  })
@PropertySource(GeocoderIntegrationTestsConfiguration.TEST_PROPERTIES)
public class GeocoderIntegrationTestsConfiguration {
  public static final String TEST_PROPERTIES = "classpath:application-test.yml";
}
