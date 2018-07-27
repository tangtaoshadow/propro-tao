package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.TargetTransition;
import com.westlake.air.pecs.domain.bean.WindowRang;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.domain.query.ExperimentQuery;

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
     * @param expId
     * @param rtExtractWindow
     * @param mzExtractWindow
     * @param buildType       0:解压缩MS1和MS2; 1:解压缩MS1; 2:解压缩MS2
     * @return
     * @throws IOException
     */
    ResultDO extract(String expId, String creator, float rtExtractWindow, float mzExtractWindow, int buildType) throws IOException;

    /**
     * 解压缩MS1数据的话会把所有的索引全部读入到内存中进行卷积
     * PECS在进行MS1谱图解析时的几个特点:
     * 1.全光谱读取
     * 2.由于是全光谱解析,因此解析过后的RTArray都是一样的,因此只存储一份从而减少内存的开销
     * 3.
     *
     * @param raf
     * @param exp
     * @param rtExtractWindow
     * @param mzExtractWindow
     * @return
     * @throws IOException
     */
    void extractMS1(RandomAccessFile raf, ExperimentDO exp, String overviewId, float rtExtractWindow, float mzExtractWindow) throws IOException;

    /**
     * 进行MS2数据卷积
     * @param raf
     * @param exp
     * @param overviewId
     * @param rtExtractWindow
     * @param mzExtractWindow
     * @throws IOException
     */
    void extractMS2(RandomAccessFile raf, ExperimentDO exp, String overviewId, float rtExtractWindow, float mzExtractWindow) throws IOException;

    /**
     * 获取一个实验的Swath Windows窗口信息
     * @param expId
     * @return
     */
    List<WindowRang> getWindows(String expId);
}
