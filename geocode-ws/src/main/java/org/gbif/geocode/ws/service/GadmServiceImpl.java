package org.gbif.geocode.ws.service;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.geocode.api.model.GadmRegion;
import org.gbif.geocode.api.service.GadmService;
import org.gbif.geocode.ws.persistence.mapper.GadmMapper;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class GadmServiceImpl implements GadmService {


  private final GadmMapper mapper;

  public GadmServiceImpl(GadmMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Collection<GadmRegion> listLevel0(String query) {
    return mapper.listLevel0(query);
  }

  @Override
  public Collection<GadmRegion> listLevel1(String query, String gid0) {
    return mapper.listLevel1(query, gid0);
  }

  @Override
  public Collection<GadmRegion> listLevel2(String query, String gid0, String gid1) {
    return mapper.listLevel2(query, gid0, gid1);
  }

  @Override
  public Collection<GadmRegion> listLevel3(String query, String gid0, String gid1, String gid2) {
    return mapper.listLevel3(query, gid0, gid1, gid2);
  }

  @Override
  public PagingResponse<GadmRegion> search(String query, Integer level, String gid, Pageable page
  ) {
    List<GadmRegion> results = mapper.search(query, level, gid, page);
    long count = mapper.searchCount(query, level, gid);
    return new PagingResponse<>(page, count, results);
  }
}
