package org.gbif.geocode.ws.guice;

import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.ws.model.LocationMapper;
import org.gbif.geocode.ws.monitoring.GeocodeWsStatistics;
import org.gbif.geocode.ws.service.Geocoder;
import org.gbif.geocode.ws.service.impl.BitmapFirstGeocoder;
import org.gbif.geocode.ws.service.impl.MyBatisGeocoder;
import org.gbif.mybatis.guice.MyBatisModule;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.gbif.ws.app.ConfUtils;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServer;

import com.google.common.base.Throwables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceServletContextListener;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

/**
 * Overarching configuration of our WebService. Tying together Jersey, MyBatis and our own classes using three
 * different modules.
 */
public class GuiceConfig extends GuiceServletContextListener {

  private static final String APP_CONF_FILE = "mybatis.properties";

  @Override
  protected Injector getInjector() {
    try {
      Properties props = PropertiesUtil.readFromFile(ConfUtils.getAppConfFile(APP_CONF_FILE));
      return Guice.createInjector(Stage.PRODUCTION, new JerseyModule(), new InternalMyBatisModule(props));
    } catch (IOException e) {
      Throwables.propagate(e);
      return null;
    }
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
   * A Guice module for all things mybatis
   * <p/>
   * A config file named mybatis-config.xml is picked up automatically.
   */
  private static class InternalMyBatisModule extends MyBatisModule {

    public InternalMyBatisModule(Properties props) {
      super("geocode", props);
    }

    @Override
    protected void bindMappers() {
      addAlias("Location").to(Location.class);
      addMapperClass(LocationMapper.class);
    }

    @Override
    protected void bindTypeHandlers() {
    }

    @Override
    protected void bindManagers() {
      bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());
      bind(Geocoder.class).annotatedWith(Names.named("Database")).to(MyBatisGeocoder.class);
      bind(Geocoder.class).to(BitmapFirstGeocoder.class);
      bind(GeocodeWsStatistics.class).asEagerSingleton();
    }

  }

}
