package org.gbif.geocode.ws.persistence;

import org.gbif.geocode.api.model.GadmRegion;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.ws.layers.jts.PointLocation;
import org.gbif.mybatis.type.StringArrayTypeHandler;

import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisConfiguration {

  @Bean
  ConfigurationCustomizer mybatisConfigCustomizer() {
    return configuration -> {
      configuration.setMapUnderscoreToCamelCase(true);
      configuration.getTypeAliasRegistry().registerAlias("Location", Location.class);
      configuration.getTypeAliasRegistry().registerAlias("GadmRegion", GadmRegion.class);
      configuration.getTypeAliasRegistry().registerAlias("PointLocation", PointLocation.class);
      configuration.getTypeAliasRegistry().registerAlias("StringArrayTypeHandler", StringArrayTypeHandler.class);
      configuration.getTypeAliasRegistry().registerAlias("HStoreHigherLevelsMapTypeHandler", HStoreHigherLevelsMapTypeHandler.class);
    };
  }
}
