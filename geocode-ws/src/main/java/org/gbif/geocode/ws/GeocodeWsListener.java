package org.gbif.geocode.ws;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.ws.model.LocationMapper;
import org.gbif.geocode.ws.model.TileMapper;
import org.gbif.geocode.ws.monitoring.GeocodeWsStatistics;
import org.gbif.geocode.ws.service.impl.MyBatisGeocoder;
import org.gbif.mybatis.guice.MyBatisModule;
import org.gbif.ws.app.ConfUtils;
import org.gbif.ws.mixin.Mixins;
import org.gbif.ws.server.guice.GbifServletListener;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class GeocodeWsListener extends GbifServletListener {

  private static final String APP_CONF_FILE = "geocode.properties";

  public GeocodeWsListener() {
    super(ConfUtils.getAppConfFile(APP_CONF_FILE), "org.gbif.geocode.ws", false);
  }

  @Override
  @VisibleForTesting
  protected Injector getInjector() {
    return super.getInjector();
  }

  @Override
  protected List<Module> getModules(Properties properties) {
    List<Module> modules = Lists.newArrayList();
    modules.add(new InternalMyBatisModule(properties));
    return modules;
  }

  @Override
  protected Map<Class<?>, Class<?>> getMixIns() {
    return Mixins.getPredefinedMixins();
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
      addMapperClass(TileMapper.class);
    }

    @Override
    protected void bindTypeHandlers() {
    }

    @Override
    protected void bindManagers() {
      bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());
      bind(GeocodeService.class).to(MyBatisGeocoder.class);
      bind(GeocodeWsStatistics.class).asEagerSingleton();
    }
  }
}
