package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.SwathInput;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.ScoreDistribution;
import com.westlake.air.pecs.domain.db.ScoresDO;
import com.westlake.air.pecs.domain.db.TaskDO;
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

    /**
     * 在某个卷积使用中查询某个ScoreType的分布范围
     * @param overviewId
     * @return
     */
    ResultDO<List<ScoreDistribution>> generateScoreRangesByOverviewId(String overviewId);

    HashMap<String, ScoresDO> getAllMapByOverviewId(String overviewId);

    ResultDO insert(ScoresDO scoresDO);

    ResultDO update(ScoresDO scoresDO);

    ResultDO delete(String id);

    ResultDO deleteAllByOverviewId(String overviewId);

    ResultDO<ScoresDO> getById(String id);

    ResultDO<ScoresDO> getByPeptideRef(String peptideRef);

    /**
     * 从一个已经卷积完毕的数据集中求出iRT
     *
     * @param overviewId
     * @param iRtLibraryId
     * @param sigmaSpacing Sigma通常为30/8 = 6.25/Spacing通常为0.01
     * @param taskDO
     * @return
     */
    ResultDO<SlopeIntercept> computeIRt(String overviewId, String iRtLibraryId, SigmaSpacing sigmaSpacing, TaskDO taskDO);

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
     * @param dataList 卷积后的数据
     * @param input 入参,必填参数包括
     *   slopeIntercept iRT计算出的斜率和截距
     *   libraryId 标准库ID
     *   sigmaSpacing Sigma通常为30/8 = 6.25/Spacing通常为0.01
     *   overviewId
     */
    List<ScoresDO> score(List<AnalyseDataDO> dataList, SwathInput input);

    /**
     * Generate the tsv format file for pyprophet
     * @param overviewId
     * @return
     */
    ResultDO exportForPyProphet(String overviewId);
}
