package org.gbif.geocode.ws.client.guice;

/**
 * A Module for Guice doing all the necessary wiring. The only thing left for clients to do is to
 * provide a named binding with the name <code>geocode.ws.url</code>.
 *
 * <p>Example:
 *
 * <pre>
 * {@code bindConstant().annotatedWith(Names.named("geocode.ws.url")).to("http://boma.gbif.org:8080/geocode-ws/reverse");}
 * </pre>
 */
// public class GeocodeWsClientModule extends AbstractModule {
//
//  @Override
//  protected void configure() {
//    bind(GeocodeService.class).to(GeocodeWsClient.class);
//  }
//
//  @Provides
//  @Singleton
//  @GeocodeWs
//  private Client providesJerseyClient() {
//    ClientConfig clientConfig = new DefaultClientConfig();
//    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, true);
//    return ApacheHttpClient4.create(clientConfig);
//  }
//
//  @Provides
//  @Singleton
//  @GeocodeWs
//  private WebResource providesGeocodeWsWebResource(@GeocodeWs Client client,
// @Named("geocode.ws.url") String url) {
//    return client.resource(url);
//  }
//
// }
