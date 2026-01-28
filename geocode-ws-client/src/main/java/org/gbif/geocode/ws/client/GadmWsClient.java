package org.gbif.geocode.ws.client;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.geocode.api.model.GadmRegion;
import org.gbif.geocode.api.service.GadmService;

import java.util.Collection;
import jakarta.annotation.Nullable;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping(value = "geocode/gadm",  produces = MediaType.APPLICATION_JSON_VALUE)
public interface GadmWsClient extends GadmService {

  @GetMapping
  @Override
  Collection<GadmRegion> listLevel0(@Nullable @RequestParam(value = "q", required = false) String query);

  @GetMapping("{level0}")
  @Override
  Collection<GadmRegion> listLevel1(@PathVariable("level0") String level0,
                                          @Nullable @RequestParam(value = "q", required = false) String query);

  @GetMapping("{level0}/{level1}")
  @Override
  Collection<GadmRegion> listLevel2(@PathVariable("level0") String level0,
                                   @PathVariable("level1") String level1,
                                   @Nullable @RequestParam(value = "q", required = false) String query);

  @GetMapping("{level0}/{level1}/{level2}")
  @Override
  Collection<GadmRegion> listLevel3(@PathVariable("level0") String level0,
                                   @PathVariable("level1") String level1,
                                   @PathVariable("level2") String level2,
                                   @Nullable @RequestParam(value = "q", required = false) String query);

  @GetMapping("search")
  @Override
  PagingResponse<GadmRegion> search(@Nullable @RequestParam(value = "q", required = false) String query,
                                    @Nullable @RequestParam(value = "gadmLevel") Integer gadmLevel,
                                    @Nullable @RequestParam(value = "gadmGid") String gadmGid,
                                    @Nullable Pageable page);
}
