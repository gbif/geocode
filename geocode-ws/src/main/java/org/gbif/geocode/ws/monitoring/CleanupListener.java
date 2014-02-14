package org.gbif.geocode.ws.monitoring;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.google.inject.Injector;

public class CleanupListener implements ServletContextListener {

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    // Nothing to initialize
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    System.out.println();
    Injector injector = (Injector) sce.getServletContext().getAttribute(Injector.class.getName());
    if (injector != null) {
      MBeanServer mbeanServer = injector.getInstance(MBeanServer.class);
      try {
        mbeanServer.unregisterMBean(new ObjectName("Geocode WS:type=Statistics"));
      } catch (InstanceNotFoundException ignored) {
      } catch (MBeanRegistrationException ignored) {
      } catch (MalformedObjectNameException ignored) {
      }
    }
  }
}
