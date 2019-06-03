package com.westlake.air.propro.service;

import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.propro.domain.bean.aird.WindowRange;
import com.westlake.air.propro.domain.bean.score.PeptideFeature;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.domain.db.simple.TargetPeptide;
import com.westlake.air.propro.domain.params.LumsParams;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 10:05
 */
public interface ScoreService {

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
     * @param swathIndex 卷积数据对应的Swath Block索引,包含window rang
     * @param input    入参,必填参数包括
     *                 slopeIntercept iRT计算出的斜率和截距
     *                 libraryId 标准库ID
     *                 sigmaSpacing Sigma通常为30/8 = 6.25/Spacing通常为0.01
     *                 overviewId
     */
    void scoreForAll(List<AnalyseDataDO> dataList, SwathIndexDO swathIndex, LumsParams input);

    /**
     * 请确保调用本函数时传入的AnalyseDataDO已经解压缩
     * @param data
     * @param intensityMap 标准库中对应的PeptideRef组
     * @param ss
     * @return
     */
    PeptideFeature selectPeak(AnalyseDataDO data, HashMap<String, Float> intensityMap, SigmaSpacing ss);

    /**
     * 请确保调用本函数时传入的AnalyseDataDO已经解压缩
     * @param data
     * @param peptide
     * @param rtMap
     * @param input
     * @return
     */
    void scoreForOne(AnalyseDataDO data, TargetPeptide peptide, TreeMap<Float, MzIntensityPairs> rtMap, LumsParams input);
}
