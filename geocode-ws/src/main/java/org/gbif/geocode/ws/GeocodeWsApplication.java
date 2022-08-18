package org.gbif.geocode.ws;

import org.gbif.ws.server.provider.CountryHandlerMethodArgumentResolver;
import org.gbif.ws.server.provider.PageableHandlerMethodArgumentResolver;

import java.util.List;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication(
    exclude = {
      SecurityAutoConfiguration.class,
      FeignAutoConfiguration.class,
      ManagementWebSecurityAutoConfiguration.class
    })
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
public class GeocodeWsApplication {
  public static void main(String[] args) {
    SpringApplication.run(GeocodeWsApplication.class, args);
  }

  @Configuration
  public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
      argumentResolvers.add(new PageableHandlerMethodArgumentResolver());
      argumentResolvers.add(new CountryHandlerMethodArgumentResolver());
    }
  }
}
