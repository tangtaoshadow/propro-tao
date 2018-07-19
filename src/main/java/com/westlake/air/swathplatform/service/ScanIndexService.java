package com.westlake.air.swathplatform.service;

import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.db.LibraryDO;
import com.westlake.air.swathplatform.domain.db.ScanIndexDO;
import com.westlake.air.swathplatform.domain.query.LibraryQuery;
import com.westlake.air.swathplatform.domain.query.ScanIndexQuery;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 09:36
 */
public interface ScanIndexService {

    List<ScanIndexDO> getAllByExperimentId(String experimentId);

    Long count(ScanIndexQuery query);

    ResultDO<List<ScanIndexDO>> getList(ScanIndexQuery query);

    List<ScanIndexDO> getAll(ScanIndexQuery query);

    ResultDO insert(ScanIndexDO scanIndexDO);

    ResultDO insertAll(List<ScanIndexDO> scanIndexDOList, boolean isDeleteOld);

    ResultDO deleteAllByExperimentId(String experimentId);

    ResultDO<ScanIndexDO> getById(String id);
}
