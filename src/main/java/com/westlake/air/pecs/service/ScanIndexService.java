package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.SimpleScanIndex;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.domain.query.ScanIndexQuery;

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

    List<SimpleScanIndex> getSimpleAll(ScanIndexQuery query);

    ResultDO insert(ScanIndexDO scanIndexDO);

    ResultDO insertAll(List<ScanIndexDO> scanIndexDOList, boolean isDeleteOld);

    ResultDO deleteAllByExperimentId(String experimentId);

    ResultDO<ScanIndexDO> getById(String id);
}
