package org.gbif.geocode.ws.persistence.mapper;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.geocode.api.model.GadmRegion;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GadmMapper {

  GadmRegion get(@Param("gid") String gid);

  Collection<GadmRegion> subdivisions(@Param("gid") String gid, @Nullable @Param("query") String query);

  Collection<GadmRegion> listLevel0(@Nullable @Param("query") String query);

  Collection<GadmRegion> listLevel1(@Nullable @Param("query") String query, @Param("gid0") String gid0);

  Collection<GadmRegion> listLevel2(@Nullable @Param("query") String query, @Param("gid0") String gid0, @Param("gid1") String gid1);

  Collection<GadmRegion> listLevel3(@Nullable @Param("query") String query, @Param("gid0") String gid0, @Param("gid1") String gid1,
                                    @Param("gid2") String gid2);

  List<GadmRegion> search(@Nullable @Param("query") String query, @Param("level") Integer level, @Nullable @Param("gid") String gid,
                          @Nullable @Param("page") Pageable page);

  long searchCount(@Nullable @Param("query") String query, @Param("level") Integer level, @Nullable @Param("gid") String gid);

}
