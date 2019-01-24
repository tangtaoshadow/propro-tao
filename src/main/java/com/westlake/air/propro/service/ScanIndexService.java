package com.westlake.air.propro.service;

import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.propro.domain.db.simple.SimpleScanIndex;
import com.westlake.air.propro.domain.db.ScanIndexDO;
import com.westlake.air.propro.domain.query.ScanIndexQuery;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;


/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 09:36
 */
public interface ScanIndexService {

    List<ScanIndexDO> getAllByExperimentId(String experimentId);

    Long count(ScanIndexQuery query);

    ResultDO<List<ScanIndexDO>> getList(ScanIndexQuery query);

    MzIntensityPairs getNearestSpectrumByRt(TreeMap<Float, MzIntensityPairs> rtMap, Double rt);

    List<ScanIndexDO> getAll(ScanIndexQuery query);

    List<SimpleScanIndex> getSimpleAll(ScanIndexQuery query);

    List<SimpleScanIndex> getSimpleList(ScanIndexQuery query);

    ResultDO insert(ScanIndexDO scanIndexDO);

    ResultDO update(ScanIndexDO scanIndexDO);

    ResultDO insertAll(List<ScanIndexDO> scanIndexDOList, boolean isDeleteOld);

    ResultDO deleteAllByExperimentId(String experimentId);

    ResultDO deleteAllSwathIndexByExperimentId(String experimentId);

    ResultDO<ScanIndexDO> getById(String id);

    /**
     * 获取某一个实验的所有SwathScanIndex的Map,SwathIndex是一种特殊的索引,每一个SwathScanIndex中存储了一个同一个Swath窗口(比如前体Mz是400-425)中的所有时间段的卷积图谱.
     * @param expId
     * @return key为前体的mz左区间,即mzStart
     */
    HashMap<Float, ScanIndexDO> getSwathIndexList(String expId);

    /**
     * 获取某一个指定前体mz所属的Swath窗口的索引信息
     * @param expId
     * @param targetPrecursorMz
     * @return
     */
    ScanIndexDO getSwathIndex(String expId, Float targetPrecursorMz);
}
