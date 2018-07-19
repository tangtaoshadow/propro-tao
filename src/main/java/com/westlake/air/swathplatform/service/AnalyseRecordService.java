package com.westlake.air.swathplatform.service;

import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.db.AnalyseRecordDO;
import com.westlake.air.swathplatform.domain.query.AnalyseRecordQuery;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:38
 */
public interface AnalyseRecordService {

    List<AnalyseRecordDO> getAllByExpId(String expId);

    Long count(AnalyseRecordQuery query);

    ResultDO<List<AnalyseRecordDO>> getList(AnalyseRecordQuery targetQuery);

    ResultDO insert(AnalyseRecordDO recordDO);

    ResultDO delete(String id);

    ResultDO deleteAllByExpId(String expId);

    ResultDO<AnalyseRecordDO> getById(String id);
}
