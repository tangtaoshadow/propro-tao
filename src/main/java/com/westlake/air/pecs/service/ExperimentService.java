package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.query.ExperimentQuery;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;


/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 09:36
 */
public interface ExperimentService {

    ResultDO<List<ExperimentDO>> getList(ExperimentQuery query);

    List<ExperimentDO> getAll();

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
     *
     * @param experimentDO
     * @param libraryId
     * @param slopeIntercept iRT求得的斜率和截距
     * @param creator
     * @param rtExtractWindow
     * @param mzExtractWindow
     * @param buildType 0代表同时卷积MS1和MS2,1代表卷积MS1,2代表卷积MS2
     * @param taskDO
     * @return
     */
    ResultDO extract(ExperimentDO experimentDO, String libraryId, SlopeIntercept slopeIntercept, String creator, float rtExtractWindow, float mzExtractWindow, int buildType, TaskDO taskDO);

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
     * @param sigma
     * @param space
     * @return
     */
    ResultDO<SlopeIntercept> convAndComputeIrt(ExperimentDO experimentDO, String iRtLibraryId, Float mzExtractWindow, Float sigma, Float space);
}
