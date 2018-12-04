package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.SwathInput;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.ScoreDistribution;
import com.westlake.air.pecs.domain.db.ScoresDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.db.simple.SimpleScores;
import com.westlake.air.pecs.domain.query.ScoresQuery;

import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 10:05
 */
public interface ScoresService {

    Long count(ScoresQuery query);

    ResultDO<List<ScoresDO>> getList(ScoresQuery targetQuery);

    List<ScoresDO> getAllByOverviewId(String overviewId);

    List<SimpleScores> getSimpleAllByOverviewId(String overviewId);

    HashMap<String, ScoresDO> getAllMapByOverviewId(String overviewId);

    ResultDO insert(ScoresDO scoresDO);

    ResultDO update(ScoresDO scoresDO);

    ResultDO delete(String id);

    ResultDO deleteAllByOverviewId(String overviewId);

    ResultDO<ScoresDO> getById(String id);

    ScoresDO getByPeptideRefAndIsDecoy(String overviewId, String peptideRef, Boolean isDecoy);

    /**
     * 从一个卷积结果列表中求出iRT
     *
     * @param dataList
     * @param iRtLibraryId
     * @param sigmaSpacing Sigma通常为30/8 = 6.25/Spacing通常为0.01
     * @return
     */
    ResultDO<SlopeIntercept> computeIRt(List<AnalyseDataDO> dataList, String iRtLibraryId, SigmaSpacing sigmaSpacing);

    /**
     * 打分
     *
     * @param dataList 卷积后的数据
     * @param input    入参,必填参数包括
     *                 slopeIntercept iRT计算出的斜率和截距
     *                 libraryId 标准库ID
     *                 sigmaSpacing Sigma通常为30/8 = 6.25/Spacing通常为0.01
     *                 overviewId
     */
    List<ScoresDO> score(List<AnalyseDataDO> dataList, SwathInput input);

    /**
     * Generate the tsv format file for pyprophet
     *
     * @param overviewId
     * @return
     */
    ResultDO exportForPyProphet(String overviewId, String spliter);

    /**
     * 生成某个ScoreType的子分数分布范围,包含分布区间和命中个数,命中个数按PeptideRef进行统计,取每一个PeptideRef的下属于该ScoreType的最高分
     *
     * @param overviewId
     * @return
     */
    ResultDO<List<ScoreDistribution>> buildScoreDistributions(String overviewId);
}
