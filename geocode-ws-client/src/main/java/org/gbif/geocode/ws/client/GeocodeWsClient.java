package org.gbif.geocode.ws.client;

import org.gbif.geocode.api.model.Location;
import org.gbif.geocode.api.service.GeocodeService;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.io.ByteStreams;

import feign.Response;

@RequestMapping("geocode")
public interface GeocodeWsClient extends GeocodeService {

  @RequestMapping(
      method = RequestMethod.GET,
      value = "reverse",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  List<Location> get(
      @RequestParam("lat") Double latitude,
      @RequestParam("lng") Double longitude,
      @Nullable @RequestParam(value = "uncertaintyDegrees", required = false)
          Double uncertaintyDegrees,
      @Nullable @RequestParam(value = "uncertaintyMeters", required = false)
          Double uncertaintyMeters,
      @Nullable @RequestParam(value = "layer", required = false) List<String> layers);

  @Override
  default List<Location> get(
      Double latitude, Double longitude, Double uncertaintyDegrees, Double uncertaintyMeters) {
    return get(latitude, longitude, uncertaintyDegrees, uncertaintyMeters, null);
  }

  @RequestMapping(
      method = RequestMethod.GET,
      value = "bitmap",
      produces = MediaType.IMAGE_PNG_VALUE)
  Response getBitmap();

  @Override
  default byte[] bitmap() {
    try {
      return ByteStreams.toByteArray(getBitmap().body().asInputStream());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
