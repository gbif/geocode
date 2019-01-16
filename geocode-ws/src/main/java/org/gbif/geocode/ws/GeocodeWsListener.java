package org.gbif.geocode.ws;

import org.gbif.geocode.api.model.HbaseProperties;
import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.ws.model.LocationMapper;
import org.gbif.geocode.ws.monitoring.GeocodeWsStatistics;
import org.gbif.geocode.ws.resource.GeocodeResource;
import org.gbif.geocode.ws.service.impl.MyBatisGeocoder;
import org.gbif.mybatis.guice.MyBatisModule;
import org.gbif.ws.app.ConfUtils;
import org.gbif.ws.mixin.Mixins;
import org.gbif.ws.server.guice.GbifServletListener;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.management.MBeanServer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeocodeWsListener extends GbifServletListener {

  private static final String APP_CONF_FILE = "geocode.properties";
  private static final Logger LOG = LoggerFactory.getLogger(GeocodeWsListener.class);

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

    Map<Boolean, List<Map.Entry<Object, Object>>> partitionedproperties = properties.entrySet()
      .stream()
      .collect(Collectors.partitioningBy(entry -> entry.getKey().toString().startsWith("hbase")));
    Properties hbaseProperties = new Properties();
    hbaseProperties.putAll(partitionedproperties.get(true)
                             .stream()
                             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    Properties myBatisProperties = new Properties();
    myBatisProperties.putAll(partitionedproperties.get(false)
                               .stream()
                               .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

    LOG.info("My batis Properties: {}", myBatisProperties);
    LOG.info("Hbase properties: {}", hbaseProperties);

    modules.add(new InternalMyBatisModule(myBatisProperties));
    modules.add(new HbaseModule(hbaseProperties));
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

  private static class HbaseModule extends AbstractModule {

    Properties properties;

    public HbaseModule(Properties properties) {
      this.properties = properties;
    }

    @Override
    protected void configure() {
      Names.bindProperties(binder(), properties);
      bind(GeocodeResource.class);
    }

    @Provides
    @Singleton
    public HbaseProperties getConfiguration(
      @Named("hbase.env") String environment,
      @Named("hbaseclient.config.path") String hbaseXmlProperties,
      @Named("hbase.table") String tableName,
      @Named("hbase.cf") String cf,
      @Named("hbase.mod") int mod
    ) {

      Configuration configuration = new Configuration();
      String filePath = Optional.ofNullable(environment.isEmpty() ? null : environment)
        .map(env -> GeocodeResource.class.getClassLoader().getResource(env).getFile())
        .orElse(hbaseXmlProperties);

      File f = new File(filePath);
      if (f.exists()) System.out.println(f.getAbsolutePath());
      List<URL> files =
        Arrays.asList(f.listFiles()).stream().filter(file -> file.getName().endsWith(".xml")).map(file -> {
          try {
            return file.toURL();
          } catch (Exception ex) {
            throw new RuntimeException();
          }
        }).collect(Collectors.toList());
      for (URL url : files) {
        configuration.addResource(new org.apache.hadoop.fs.Path(url.toString()));
      }

      String finalTableName = tableName.isEmpty() ? "geo" : tableName;
      String finalColumnFamily = cf.isEmpty() ? "locations" : cf;
      int finalMod = mod == 0 ? 1000 : mod;
      HbaseProperties hbaseProperties = new HbaseProperties(configuration, finalTableName, finalColumnFamily, finalMod);
      LOG.info(" HbaseProperties initialized with {}.", hbaseProperties);
      return hbaseProperties;
    }
  }
}
