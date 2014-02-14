package org.gbif.geocode.ws.client.guice;

import org.gbif.geocode.api.service.GeocodeService;
import org.gbif.geocode.ws.client.GeocodeWsClient;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.client.apache4.ApacheHttpClient4;

/**
 * A Module for Guice doing all the necessary wiring. The only thing left for clients to do is to provide a named
 * binding with the name <code>geocode.ws.url</code>.
 * <p/>
 * Example:
 * <pre>
 * {@code bindConstant().annotatedWith(Names.named("geocode.ws.url")).to("http://boma.gbif.org:8080/geocode-ws/reverse");}
 * </pre>
 */
public class GeocodeWsClientModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(GeocodeService.class).to(GeocodeWsClient.class);
  }

  @Provides
  @Singleton
  @GeocodeWs
  private Client providesJerseyClient() {
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, true);
    return ApacheHttpClient4.create(clientConfig);
  }

  @Provides
  @Singleton
  @GeocodeWs
  private WebResource providesGeocodeWsWebResource(@GeocodeWs Client client, @Named("geocode.ws.url") String url) {
    return client.resource(url);
  }

}
