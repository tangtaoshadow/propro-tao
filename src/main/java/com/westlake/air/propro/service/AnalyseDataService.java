package com.westlake.air.propro.service;

import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.AnalyseDataDO;
import com.westlake.air.propro.domain.db.simple.MatchedPeptide;
import com.westlake.air.propro.domain.db.simple.SimpleScores;
import com.westlake.air.propro.domain.query.AnalyseDataQuery;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:02
 */
public interface AnalyseDataService {

    List<AnalyseDataDO> getAllByOverviewId(String overviewId);

    List<SimpleScores> getSimpleScoresByOverviewId(String overviewId);

    List<MatchedPeptide> getAllSuccessMatchedPeptides(String overviewId);

    AnalyseDataDO getByOverviewIdAndPeptideRefAndIsDecoy(String overviewId,String peptideRef,Boolean isDecoy);

    Long count(AnalyseDataQuery query);

    ResultDO<List<AnalyseDataDO>> getList(AnalyseDataQuery query);

    List<AnalyseDataDO> getAll(AnalyseDataQuery query);

    ResultDO insert(AnalyseDataDO dataDO);

    ResultDO insertAll(List<AnalyseDataDO> convList, boolean isDeleteOld);

    ResultDO update(AnalyseDataDO dataDO);

    ResultDO delete(String id);

    ResultDO deleteAllByOverviewId(String overviewId);

    ResultDO<AnalyseDataDO> getById(String id);

    ResultDO<AnalyseDataDO> getByIdWithConvolutionData(String id);

    ResultDO<List<AnalyseDataDO>> getListWithConvolutionData(AnalyseDataQuery query);

    /**
     * 获取MS1的卷积卷积信息
     *
     * @param overviewId
     * @param peptideRef
     * @return
     */
    ResultDO<AnalyseDataDO> getMS1Data(String overviewId, String peptideRef);

    /**
     * 获取MS2的卷积组数据,唯一值
     *
     * @param overviewId
     * @param peptideRef
     * @return
     */
    ResultDO<AnalyseDataDO> getMS2Data(String overviewId, String peptideRef, Boolean isDecoy);
}
