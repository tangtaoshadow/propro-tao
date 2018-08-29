package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.SwathInput;
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
     * 卷积但是不返回卷积结果
     * @param swathInput
     * @return
     */
    ResultDO extract(SwathInput swathInput);

    /**
     * 优化入参数目,卷积完毕后所有卷积结果加在到内存中并且返回
     * @param swathInput
     * @return
     */
    ResultDO<List<AnalyseDataDO>> extractWithList(SwathInput swathInput);

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
