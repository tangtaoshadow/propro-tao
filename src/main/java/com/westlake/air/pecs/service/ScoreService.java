package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.SwathInput;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.TaskDO;

import java.util.List;

public interface ScoreService {

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
     * @param input 入参,包括
     *   slopeIntercept iRT计算出的斜率和截距
     *   libraryId 标准库ID
     *   sigmaSpacing Sigma通常为30/8 = 6.25/Spacing通常为0.01
     */
    void score(List<AnalyseDataDO> dataList, SwathInput input);

}
