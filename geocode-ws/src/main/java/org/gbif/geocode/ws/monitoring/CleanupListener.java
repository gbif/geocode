package org.gbif.geocode.ws.monitoring;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

@WebListener
public class CleanupListener implements ServletContextListener {

  public static final Logger LOG = LoggerFactory.getLogger(CleanupListener.class);

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    MBeanServer mbeanServer =
        WebApplicationContextUtils.getRequiredWebApplicationContext(sce.getServletContext())
            .getBean(MBeanServer.class);
    try {
      mbeanServer.unregisterMBean(new ObjectName("Geocode WS:type=Statistics"));
      LOG.info("Statistics MBean unregistered");
    } catch (InstanceNotFoundException
        | MBeanRegistrationException
        | MalformedObjectNameException ignored) {
      LOG.info("Error unregistering MBeanServer", ignored);
    }
  }
}
