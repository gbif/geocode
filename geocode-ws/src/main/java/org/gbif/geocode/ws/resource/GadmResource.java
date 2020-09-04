package org.gbif.geocode.ws.resource;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.geocode.api.model.GadmRegion;
import org.gbif.geocode.api.service.GadmService;

import java.util.Collection;

import javax.annotation.Nullable;
import javax.validation.constraints.Max;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "geocode/gadm", produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin(
  allowedHeaders = {"authorization", "content-type"},
  exposedHeaders = {
    "Access-Control-Allow-Origin",
    "Access-Control-Allow-Methods",
    "Access-Control-Allow-Headers"
  })
public class GadmResource {


  private final GadmService service;


  public GadmResource(GadmService service) {
    this.service = service;
  }

  @GetMapping
  public Collection<GadmRegion> getLevel0(@Nullable @RequestParam(value = "q", required = false) String q) {
    return service.listLevel0(q);
  }

  @GetMapping("{level0}")
  public Collection<GadmRegion> getLevel1(@PathVariable("level0") String level0,
                                          @Nullable @RequestParam(value = "q", required = false) String q) {
    return service.listLevel1( q, level0);
  }

  @GetMapping("{level0}/{level1}")
  public Collection<GadmRegion> getLevel2(@PathVariable("level0") String level0,
                                          @PathVariable("level1") String level1,
                                          @Nullable @RequestParam(value = "q", required = false) String q) {
    return service.listLevel2(q, level0, level1);
  }

  @GetMapping("{level0}/{level1}/{level2}")
  public Collection<GadmRegion> getLevel3(@PathVariable("level0") String level0,
                                          @PathVariable("level1") String level1,
                                          @PathVariable("level2") String level2,
                                          @Nullable @RequestParam(value = "q", required = false) String q) {
    return service.listLevel3(q, level0, level1, level2);
  }

  @GetMapping("search")
  public PagingResponse<GadmRegion> search(@Nullable @RequestParam(value = "q", required = false) String q,
                                           @Max(value = 2, message = "Only levels 0, 1 and 2 are supported")
                                           @Nullable @RequestParam(value = "gadmLevel", defaultValue = "0") int gadmLevel,
                                           @Nullable @RequestParam(value = "gadmGid") String gadmGid,
                                           @Nullable Pageable page
                                           ) {
    return service.search(q, gadmLevel, gadmGid, page);
  }
}
