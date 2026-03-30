/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.geocode.api.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

/**
 * Provides an Avro encoding for the locations contained within the geocode response.
 *
 * <p>This exists to better compress responses specifically to allow better cache performance.
 * The HBase block cache is an example that benefits from this.
 *
 * <p>Optimisations applied during encoding:
 * <ul>
 *   <li>The source string is dropped from the response! This is because GBIF pipelines do not require
 *   this and it is verbose.</li>
 *   <li>The {@code type} string is stored as a compact {@link LocationType} enum. Should new types be
 *   added to the database, the avro schema must be changed to support it.</li>
 *   <li>Well-known ID prefixes are stripped from {@code id} and recorded in {@code id_prefix},
 *       saving repeated prefix bytes per record.</li>
 * </ul>
 */
public class LocationEncoder {

  // Optimisation: Detect common prefixes to reduce bytes. One URL per value only!
  private static final Map<LocationIdPrefix, String> ID_PREFIX_MAP =
    Map.of(LocationIdPrefix.MRGID, "http://marineregions.org/mrgid/");
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private LocationEncoder() {}

  /** Converts to the Avro representation. */
  public static LocationAvro toAvro(Location location) {
    if (location == null) return null;

    LocationType typeEnum = LocationType.valueOf(location.getType());

    // Optimisation: detect common prefixes
    String id = location.getId();
    LocationIdPrefix idPrefix = null;
    for (Map.Entry<LocationIdPrefix, String> entry : ID_PREFIX_MAP.entrySet()) {
      if (id != null && id.startsWith(entry.getValue())) {
        idPrefix = entry.getKey();
        id = id.substring(entry.getValue().length());
        break;
      }
    }

    return LocationAvro.newBuilder()
      .setId(id)
      .setIdPrefix(idPrefix)
      .setType(typeEnum)
      .setTitle(location.getTitle())
      .setIsoCountryCode2Digit(location.getIsoCountryCode2Digit())
      .setDistance(location.getDistance())
      .setDistanceMeters(location.getDistanceMeters())
      .build();
  }

  /** Converts from the Avro representation. */
  public static Location fromAvro(LocationAvro locationAvro) {
    if (locationAvro == null) {
      return null;
    }

    String id = locationAvro.getId();
    if (locationAvro.getIdPrefix() != null) {
      id = ID_PREFIX_MAP.get(locationAvro.getIdPrefix()) + id;
    }

    Location location = new Location();
    location.setId(id);
    location.setType(locationAvro.getType().toString());
    location.setTitle(locationAvro.getTitle());
    location.setIsoCountryCode2Digit(locationAvro.getIsoCountryCode2Digit());
    location.setDistance(locationAvro.getDistance());
    location.setDistanceMeters(locationAvro.getDistanceMeters());
    return location;
  }

  /** Converts to the Avro representation. */
  public static LocationList toAvro(List<Location> locations) {
    List<LocationAvro> payload = new ArrayList<>(locations.size());
    for (Location location : locations) {
      payload.add(toAvro(location));
    }
    return LocationList.newBuilder().setLocations(payload).build();
  }


  /** Converts from the Avro representation. */
  public static List<Location> fromAvro(LocationList locations) {
    if (locations == null || locations.getLocations() == null) return new ArrayList<>();

    List<Location> response = new ArrayList<>();
    for (LocationAvro location : locations.getLocations()) {
      response.add(fromAvro(location));
    }
    return response;
  }

  /** Encodes the locations in Avro omitting the schema. */
  public static byte[] encode(List<Location> locations) throws IOException {
    LocationList avro = toAvro(locations);
    DatumWriter<LocationList> datumWriter = new SpecificDatumWriter<>(LocationList.class);
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
      datumWriter.write(avro, encoder);
      encoder.flush();
      return out.toByteArray();
    }
  }

  /** Decodes the locations from Avro. */
  public static List<Location> decode(byte[] data) throws IOException {
    DatumReader<LocationList> datumReader = new SpecificDatumReader<>(LocationList.class);
    try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
      BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(in, null);
      LocationList avro = datumReader.read(null, decoder);
      return fromAvro(avro);
    }
  }

  /**
   * A utility method that reads JSON that has been serialized as bytes and converts to the Avro representation.
   * This is only expected to be useful to e.g. rewrite an existing cache.
   */
  public static byte[] encodeFromJsonBytes(byte[] json) throws IOException {
    JsonNode root = OBJECT_MAPPER.readTree(json);
    List<Location> locations;
    if (root.isObject() && root.has("locations")) {
      locations = OBJECT_MAPPER.convertValue(root.get("locations"), new TypeReference<>() {});
    } else {
      locations = OBJECT_MAPPER.convertValue(root, new TypeReference<>() {});
    }
    return encode(locations);
  }
}
