package org.gbif.geocode.ws.client;

// @Singleton
// public class GeocodeWsClient extends BaseWsClient implements GeocodeService {
//
//  private static final String GEOCODE_PATH = "geocode";
//
//  @Inject
//  public GeocodeWsClient(@GeocodeWs WebResource resource) {
//    super(resource.path(GEOCODE_PATH));
//  }
//
//  @Override
//  public Collection<Location> get(Double latitude, Double longitude, Double uncertaintyDegrees,
// Double uncertaintyMeters) {
//    return get(latitude, longitude, uncertaintyDegrees, uncertaintyMeters,
// Collections.EMPTY_LIST);
//  }
//
//  @Override
//  public Collection<Location> get(Double latitude, Double longitude, Double uncertaintyDegrees,
// Double uncertaintyMeters, List<String> layers) {
//    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
//    queryParams.add("lat", latitude.toString());
//    queryParams.add("lng", longitude.toString());
//    if (uncertaintyDegrees != null) queryParams.add("uncertaintyDegrees",
// uncertaintyDegrees.toString());
//    if (uncertaintyMeters != null) queryParams.add("uncertaintyMeters",
// uncertaintyMeters.toString());
//    layers.stream().forEach(l -> queryParams.add("layer", l));
//
//    return Arrays.asList(resource.path("reverse").queryParams(queryParams).get(Location[].class));
//  }
//
//  @Override
//  public byte[] bitmap() {
//    return resource.path("bitmap").get(byte[].class);
//  }
// }
