package org.gbif.geocode.ws.resource;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.geocode.api.model.GadmRegion;
import org.gbif.geocode.api.service.GadmService;

import java.util.Collection;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
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
@Validated
public class GadmResource implements GadmService {


  private final GadmService service;


  public GadmResource(GadmService service) {
    this.service = service;
  }

  @GetMapping("{gid}")
  @Override
  public GadmRegion get(@PathVariable("gid") String gid) {
    return service.get(gid);
  }

  @GetMapping("{gid}/subdivisions")
  @Override
  public Collection<GadmRegion> subdivisions(@PathVariable("gid") String gid, @RequestParam(value = "q", required = false) String q) {
    return service.subdivisions(gid, q);
  }

  @GetMapping("browse")
  @Override
  public Collection<GadmRegion> listLevel0(@RequestParam(value = "q", required = false) String q) {
    return service.listLevel0(q);
  }

  @GetMapping("browse/{level0}")
  @Override
  public Collection<GadmRegion> listLevel1(@PathVariable("level0") String level0,
                                           @RequestParam(value = "q", required = false) String q) {
    return service.listLevel1( q, level0);
  }

  @GetMapping("browse/{level0}/{level1}")
  @Override
  public Collection<GadmRegion> listLevel2(@PathVariable("level0") String level0,
                                           @PathVariable("level1") String level1,
                                           @RequestParam(value = "q", required = false) String q) {
    return service.listLevel2(q, level0, level1);
  }

  @GetMapping("browse/{level0}/{level1}/{level2}")
  @Override
  public Collection<GadmRegion> listLevel3(@PathVariable("level0") String level0,
                                           @PathVariable("level1") String level1,
                                           @PathVariable("level2") String level2,
                                           @RequestParam(value = "q", required = false) String q) {
    return service.listLevel3(q, level0, level1, level2);
  }

  @GetMapping("search")
  @Override
  public PagingResponse<GadmRegion> search(@RequestParam(value = "q", required = false) String q,
                                           @RequestParam(value = "gadmLevel") Integer gadmLevel,
                                           @RequestParam(value = "gadmGid") String gadmGid,
                                           Pageable page) {
    return service.search(q, gadmLevel, gadmGid, page);
  }
}
