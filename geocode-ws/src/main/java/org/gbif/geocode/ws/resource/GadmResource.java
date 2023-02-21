package org.gbif.geocode.ws.resource;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.geocode.api.model.GadmRegion;
import org.gbif.geocode.api.service.GadmService;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

@RestController
@RequestMapping(value = "geocode/gadm", produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin(
  allowedHeaders = {"authorization", "content-type"},
  exposedHeaders = {
    "Access-Control-Allow-Origin",
    "Access-Control-Allow-Methods",
    "Access-Control-Allow-Headers"
  })
@Validated
public class GadmResource implements GadmService {

  private final GadmService service;

  public GadmResource(GadmService service) {
    this.service = service;
  }

  // "q" parameter used on several methods.
  @Target({METHOD, FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Parameter(
    name = "q",
    description = "Query for (sub)divisions matching a wildcard.",
    schema = @Schema(implementation = String.class),
    in = ParameterIn.QUERY)
  public @interface QParameter {}

  // Common successful response
  @Target({METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @ApiResponse(
    responseCode = "200",
    description = "List of GADM regions.")
  public @interface ApiResponseGadmRegionList {}

  // Common error response
  @Target({METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @ApiResponse(
    responseCode = "404",
    description = "GADM region unknown.",
    content = @Content)
  public @interface ApiResponseGadmRegionUnknown {}

  @Tag(name = "GADM regions")
  @Operation(
    operationId = "getGadmRegion",
    summary = "Details for a GADM region",
    description = "Details for a single GADM region."
  )
  @ApiResponse(
    responseCode = "200",
    description = "GADM region returned.")
  @ApiResponseGadmRegionUnknown
  @GetMapping("{gid}")
  @Override
  public GadmRegion get(@PathVariable("gid") String gid) {
    return service.get(gid);
  }

  @Tag(name = "GADM regions")
  @Operation(
    operationId = "getGadmSubdivisions",
    summary = "Subdivisions of a GADM region",
    description = "Lists sub-regions or divisions of a region."
  )
  @QParameter
  @Parameter(
    name = "gid",
    description = "GADM region.",
    in = ParameterIn.PATH)
  @ApiResponseGadmRegionList
  @ApiResponseGadmRegionUnknown
  @GetMapping("{gid}/subdivisions")
  @Override
  public Collection<GadmRegion> subdivisions(@PathVariable("gid") String gid, @RequestParam(value = "q", required = false) String q) {
    return service.subdivisions(gid, q);
  }

  @Tag(name = "GADM regions")
  @Operation(
    operationId = "gadmRegionBrowseLevel0",
    summary = "List all top-level GADM regions",
    description = "Lists GADM regions at the highest level."
  )
  @QParameter
  @ApiResponseGadmRegionList
  @GetMapping("browse")
  @Override
  public Collection<GadmRegion> listLevel0(@RequestParam(value = "q", required = false) String q) {
    return service.listLevel0(q);
  }

  @Tag(name = "GADM regions")
  @Operation(
    operationId = "gadmRegionBrowseLevel1",
    summary = "First-level subdivisions of a GADM region",
    description = "Lists first-level subdivisions of a top-level GADM region."
  )
  @QParameter
  @Parameter(
    name = "level0",
    description = "Top-level GADM region.",
    in = ParameterIn.PATH,
    example = "DNK")
  @ApiResponseGadmRegionList
  @ApiResponseGadmRegionUnknown
  @GetMapping("browse/{level0}")
  @Override
  public Collection<GadmRegion> listLevel1(@PathVariable("level0") String level0,
                                           @RequestParam(value = "q", required = false) String q) {
    return service.listLevel1( q, level0);
  }

  @Tag(name = "GADM regions")
  @Operation(
    operationId = "gadmRegionBrowseLevel2",
    summary = "Second-level subdivisions of a GADM region",
    description = "Lists second-level subdivisions of a first-level GADM subdivision."
  )
  @Parameter(
    name = "level0",
    description = "Top-level GADM region.",
    in = ParameterIn.PATH,
    example = "DNK")
  @Parameter(
    name = "level1",
    description = "Level 1 GADM region.",
    in = ParameterIn.PATH,
    example = "DNK.1_1")
  @QParameter
  @ApiResponseGadmRegionList
  @ApiResponseGadmRegionUnknown
  @GetMapping("browse/{level0}/{level1}")
  @Override
  public Collection<GadmRegion> listLevel2(@PathVariable("level0") String level0,
                                           @PathVariable("level1") String level1,
                                           @RequestParam(value = "q", required = false) String q) {
    return service.listLevel2(q, level0, level1);
  }

  @Tag(name = "GADM regions")
  @Operation(
    operationId = "gadmRegionBrowseLevel3",
    summary = "Third-level subdivisions of a GADM region",
    description = "Lists third-level subdivisions of a second-level GADM subdivision."
  )
  @QParameter
  @Parameter(
    name = "level0",
    description = "Top-level GADM region.",
    in = ParameterIn.PATH,
    example = "DNK")
  @Parameter(
    name = "level1",
    description = "Level 1 GADM region.",
    in = ParameterIn.PATH,
    example = "DNK.1_1")
  @Parameter(
    name = "level2",
    description = "Level 2 GADM region.",
    in = ParameterIn.PATH,
    example = "DNK.1.1_1")
  @ApiResponseGadmRegionList
  @ApiResponseGadmRegionUnknown
  @GetMapping("browse/{level0}/{level1}/{level2}")
  @Override
  public Collection<GadmRegion> listLevel3(@PathVariable("level0") String level0,
                                           @PathVariable("level1") String level1,
                                           @PathVariable("level2") String level2,
                                           @RequestParam(value = "q", required = false) String q) {
    return service.listLevel3(q, level0, level1, level2);
  }

  @Tag(name = "GADM regions")
  @Operation(
    operationId = "gadmRegionSearch",
    summary = "Search for GADM regions.",
    description = "Search for GADM regions. When parameters are used the results " +
      "are narrowed to results that are subdivisions of `gadmGid` at level `gadmLevel`."
  )
  @QParameter
  @Parameter(
    name = "gadmGid",
    description = "Limit to subdivisions of this GADM region.",
    example = "SLV.4_1"
  )
  @Parameter(
    name = "gadmLevel",
    description = "Limit to subdivisions at this level.",
    schema = @Schema(minimum = "1", maximum = "5"),
    example = "2"
  )
  @ApiResponseGadmRegionList
  @ApiResponseGadmRegionUnknown
  @GetMapping("search")
  @Override
  public PagingResponse<GadmRegion> search(@RequestParam(value = "q", required = false) String q,
                                           @RequestParam(value = "gadmLevel") Integer gadmLevel,
                                           @RequestParam(value = "gadmGid") String gadmGid,
                                           Pageable page) {
    return service.search(q, gadmLevel, gadmGid, page);
  }
}
