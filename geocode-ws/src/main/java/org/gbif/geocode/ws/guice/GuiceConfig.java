package org.gbif.geocode.ws.guice;

import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.ws.model.LocationMapper;
import org.gbif.geocode.ws.monitoring.GeocodeWsStatistics;
import org.gbif.geocode.ws.service.Geocoder;
import org.gbif.geocode.ws.service.impl.MyBatisGeocoder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServer;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceServletContextListener;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.mybatis.guice.MyBatisModule;
import org.mybatis.guice.datasource.bonecp.BoneCPProvider;

/**
 * Overarching configuration of our WebService. Tying together Jersey, MyBatis and our own classes using three
 * different modules.
 */
public class GuiceConfig extends GuiceServletContextListener {

  @Override
  protected Injector getInjector() {
    return Guice.createInjector(Stage.PRODUCTION, new JerseyModule(), new GeocodeWsModule(), new InternalMyBatisModule());
  }

  /**
   * A Guice module for all Jersey relevant details
   */
  private static class JerseyModule extends JerseyServletModule {

    @Override
    protected void configureServlets() {
      Map<String, String> params = new HashMap<String, String>(2);

      // Configure automatic JSON output for Jersey
      params.put(JSONConfiguration.FEATURE_POJO_MAPPING, "true");

      // Let Jersey look for root resources and Providers automatically
      params.put(PackagesResourceConfig.PROPERTY_PACKAGES, "org.gbif.geocode");

      serve("/*").with(GuiceContainer.class, params);
    }

  }

  /**
   * A Guice module for all things not tied to either mybatis or Jersey
   */
  private static class GeocodeWsModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());
      bind(Geocoder.class).to(MyBatisGeocoder.class);
      bind(GeocodeWsStatistics.class).asEagerSingleton();
    }
  }

  /**
   * A Guice module for all things mybatis
   * <p/>
   * A config file named mybatis-config.xml is picked up automatically.
   */
  private static class InternalMyBatisModule extends MyBatisModule {

    @Override
    protected void initialize() {
      Properties properties = new Properties();
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream("mybatis.properties");
      try {
        properties.load(inputStream);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      Names.bindProperties(binder(), properties);
      bindConstant().annotatedWith(Names.named("bonecp.partitionCount")).to(4);
      bindConstant().annotatedWith(Names.named("bonecp.maxConnectionsPerPartition")).to(5);
      environmentId("default");
      bindTransactionFactoryType(JdbcTransactionFactory.class);
      bindDataSourceProviderType(BoneCPProvider.class);
      addAlias("Location").to(Location.class);
      addMapperClass(LocationMapper.class);
    }
  }
}
