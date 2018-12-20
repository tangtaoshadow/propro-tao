package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.pecs.domain.bean.score.FeatureByPep;
import com.westlake.air.pecs.domain.db.*;
import com.westlake.air.pecs.domain.db.simple.TargetPeptide;
import com.westlake.air.pecs.domain.params.LumsParams;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.simple.MatchedPeptide;
import com.westlake.air.pecs.domain.db.simple.SimpleScores;
import com.westlake.air.pecs.domain.query.ScoresQuery;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 10:05
 */
public interface ScoresService {

    Long count(ScoresQuery query);

    ResultDO<List<ScoresDO>> getList(ScoresQuery targetQuery);

    List<MatchedPeptide> getAllMatchedPeptides(String overviewId);

    List<ScoresDO> getAllByOverviewId(String overviewId);

    List<SimpleScores> getSimpleAllByOverviewId(String overviewId);

    HashMap<String, ScoresDO> getAllMapByOverviewId(String overviewId);

    ResultDO insert(ScoresDO scoresDO);

    ResultDO insertAll(List<ScoresDO> scoresList);

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
     * 打分,调用本函数前最好确认是否已经删除了已有的打分数据,一份分析报告中只对应一份打分数据
     *
     * @param dataList 卷积后的数据,使用本函数时必须保证传入的dataList的前体的mz均在同一个Swath窗口内,否则会报错
     * @param rang 卷积数据对应的window rang
     * @param input    入参,必填参数包括
     *                 slopeIntercept iRT计算出的斜率和截距
     *                 libraryId 标准库ID
     *                 sigmaSpacing Sigma通常为30/8 = 6.25/Spacing通常为0.01
     *                 overviewId
     */
    List<ScoresDO> scoreForAll(List<AnalyseDataDO> dataList, WindowRang rang, ScanIndexDO swathIndex, LumsParams input);

    /**
     * 请确保调用本函数时传入的AnalyseDataDO已经解压缩
     * @param data
     * @param intensityMap 标准库中对应的PeptideRef组
     * @param ss
     * @return
     */
    FeatureByPep selectPeak(AnalyseDataDO data, HashMap<String, Float> intensityMap, SigmaSpacing ss);

    /**
     * 请确保调用本函数时传入的AnalyseDataDO已经解压缩
     * @param data
     * @param peptide
     * @param rtMap
     * @param input
     * @return
     */
    ScoresDO scoreForOne(AnalyseDataDO data, TargetPeptide peptide, TreeMap<Float, MzIntensityPairs> rtMap, LumsParams input);

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
