package org.gbif.geocode.api.service;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.geocode.api.model.GadmRegion;

import java.util.Collection;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public interface GadmService {

  GadmRegion get(String gid);

  Collection<GadmRegion> subdivisions(String gid, @Nullable String query);

  Collection<GadmRegion> listLevel0(@Nullable String query);

  Collection<GadmRegion> listLevel1(@Nullable String query, String gid0);

  Collection<GadmRegion> listLevel2(@Nullable String query, String gid0, String gid1);

  Collection<GadmRegion> listLevel3(@Nullable String query, String gid0, String gid1, String gid2);

  PagingResponse<GadmRegion> search(@Nullable String query,
                                    @Max(value = 3, message = "Only levels 0, 1, 2 and 3 are supported")
                                    @Min(value = 0, message = "Only levels 0, 1, 2 and 3 are supported")
                                    @Nullable Integer level,
                                    @Nullable String gid,
                                    @Nullable Pageable page);

}
