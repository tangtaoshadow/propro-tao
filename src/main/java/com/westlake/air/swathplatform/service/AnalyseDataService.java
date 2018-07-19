package com.westlake.air.swathplatform.service;

import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.db.AnalyseDataDO;
import com.westlake.air.swathplatform.domain.query.AnalyseDataQuery;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:02
 */
public interface AnalyseDataService {

    List<AnalyseDataDO> getAllByRecordId(String expId);

    Long count(AnalyseDataQuery query);

    ResultDO<List<AnalyseDataDO>> getList(AnalyseDataQuery convQuery);

    ResultDO insert(AnalyseDataDO convData);

    ResultDO insertAll(List<AnalyseDataDO> convList, boolean isDeleteOld);

    ResultDO delete(String id);

    ResultDO deleteAllByExpId(String expId);

    ResultDO<AnalyseDataDO> getById(String id);
}
