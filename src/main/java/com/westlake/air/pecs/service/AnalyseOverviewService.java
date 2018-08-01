package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import com.westlake.air.pecs.domain.query.AnalyseOverviewQuery;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:38
 */
public interface AnalyseOverviewService {

    List<AnalyseOverviewDO> getAllByExpId(String expId);

    Long count(AnalyseOverviewQuery query);

    ResultDO<List<AnalyseOverviewDO>> getList(AnalyseOverviewQuery targetQuery);

    ResultDO insert(AnalyseOverviewDO recordDO);

    ResultDO delete(String id);

    ResultDO deleteAllByExpId(String expId);

    ResultDO<AnalyseOverviewDO> getById(String id);

    ResultDO<AnalyseOverviewDO> getFirstByExpId(String expId);
}
