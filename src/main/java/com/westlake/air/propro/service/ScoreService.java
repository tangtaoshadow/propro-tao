package com.westlake.air.propro.service;

import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.bean.irt.IrtResult;
import com.westlake.air.propro.domain.db.AnalyseDataDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.simple.TargetPeptide;
import com.westlake.air.propro.domain.params.LumsParams;

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
     * @param library
     * @param sigmaSpacing Sigma通常为30/8 = 6.25/Spacing通常为0.01
     * @return
     */
    ResultDO<IrtResult> computeIRt(List<AnalyseDataDO> dataList, LibraryDO library, SigmaSpacing sigmaSpacing) throws Exception;

    /**
     * 请确保调用本函数时传入的AnalyseDataDO已经解压缩
     * @param data
     * @param peptide
     * @param rtMap
     * @param input
     * @return
     */
    void scoreForOne(AnalyseDataDO data, TargetPeptide peptide, TreeMap<Float, MzIntensityPairs> rtMap, LumsParams input);

    /**
     * 仅Shape和Shape Weighted Score均高于99分的可以过
     * @param data
     * @param peptide
     * @param rtMap
     */
    void strictScoreForOne(AnalyseDataDO data, TargetPeptide peptide , TreeMap<Float, MzIntensityPairs> rtMap);
}
