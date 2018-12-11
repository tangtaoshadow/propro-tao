package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.SwathParams;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.*;
import com.westlake.air.pecs.domain.query.ExperimentQuery;

import java.io.File;
import java.util.List;


/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 09:36
 */
public interface ExperimentService {

    ResultDO<List<ExperimentDO>> getList(ExperimentQuery query);

    List<ExperimentDO> getAll();

    List<ExperimentDO> getSimpleAll();

    ResultDO insert(ExperimentDO experimentDO);

    ResultDO update(ExperimentDO experimentDO);

    ResultDO delete(String id);

    ResultDO<ExperimentDO> getById(String id);

    ResultDO<ExperimentDO> getByName(String name);

    /**
     * 获取一个实验的Swath Windows窗口信息
     * @param expId
     * @return
     */
    List<WindowRang> getWindows(String expId);

    void uploadFile(ExperimentDO experimentDO, File file, TaskDO taskDO);

    /**
     * 卷积的核心函数,最终返回卷积到的Peptide数目
     * 目前只支持MS2的卷积
     * @param swathParams
     * @return
     */
    ResultDO<AnalyseOverviewDO> extract(SwathParams swathParams);

    /**
     * 实时卷积某一个PeptideRef的图谱,卷积的rtWindows默认为-1,即全时间段卷积
     * @param exp
     * @param peptide
     * @return
     */
    ResultDO<AnalyseDataDO> extractOne(ExperimentDO exp, PeptideDO peptide, Float rtExtractorWindow);

    /**
     * 卷积iRT校准库的数据
     * @param experimentDO
     * @param iRtLibraryId
     * @param mzExtractWindow
     * @return
     */
    List<AnalyseDataDO> extractIrt(ExperimentDO experimentDO, String iRtLibraryId, float mzExtractWindow);

    /**
     * 卷积并且求出iRT
     * @param experimentDO
     * @param iRtLibraryId
     * @param mzExtractWindow
     * @param sigmaSpacing
     * @return
     */
    ResultDO<SlopeIntercept> convAndIrt(ExperimentDO experimentDO, String iRtLibraryId, Float mzExtractWindow, SigmaSpacing sigmaSpacing);


}
