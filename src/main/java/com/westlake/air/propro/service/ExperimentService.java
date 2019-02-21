package com.westlake.air.propro.service;

import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.WindowRange;
import com.westlake.air.propro.domain.params.LumsParams;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.domain.query.ExperimentQuery;

import java.io.File;
import java.util.List;


/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 09:36
 */
public interface ExperimentService {

    ResultDO<List<ExperimentDO>> getList(ExperimentQuery query);

    List<ExperimentDO> getAll(ExperimentQuery query);

    List<ExperimentDO> getAllByProjectName(String projectName);

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
    List<WindowRange> getWindows(String expId);
    List<WindowRange> getPrmWindows(String expId);

    void uploadFile(ExperimentDO experimentDO, File file, TaskDO taskDO);

    void uploadAirdFile(ExperimentDO experimentDO, String airdFilePath, TaskDO taskDO);

    /**
     * 卷积的核心函数,最终返回卷积到的Peptide数目
     * 目前只支持MS2的卷积
     * @param lumsParams
     * useEpps: true: 将卷积,选峰及打分合并在一个步骤中执行,可以完整的省去一次IO读取及解析,大大提升分析速度
     * 需要experimentDO,libraryId,rtExtractionWindow,mzExtractionWindow,SlopeIntercept
     * @return
     */
    ResultDO<AnalyseOverviewDO> extract(LumsParams lumsParams);

    /**
     * 实时卷积某一个PeptideRef的图谱,卷积的rtWindows默认为-1,即全时间段卷积
     * @param exp
     * @param peptide
     * @return
     */
    ResultDO<AnalyseDataDO> extractOne(ExperimentDO exp, PeptideDO peptide, Float rtExtractorWindow, Float mzExtractorWindow);

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
