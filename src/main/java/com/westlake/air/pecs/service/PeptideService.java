package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.PeptideDO;
import com.westlake.air.pecs.domain.db.simple.IntensityGroup;
import com.westlake.air.pecs.domain.db.simple.Protein;
import com.westlake.air.pecs.domain.db.simple.TargetPeptide;
import com.westlake.air.pecs.domain.query.PeptideQuery;

import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 19:56
 */
public interface PeptideService {

    List<PeptideDO> getAllByLibraryId(String libraryId);

    List<PeptideDO> getAllByLibraryIdAndIsDecoy(String libraryId, boolean isDecoy);

    Long count(PeptideQuery query);

    ResultDO<List<PeptideDO>> getList(PeptideQuery peptideQuery);

    ResultDO insert(PeptideDO peptideDO);

    ResultDO insertAll(List<PeptideDO> peptides, boolean isDeleteOld);

    ResultDO deleteAllByLibraryId(String libraryId);

    ResultDO deleteAllDecoyByLibraryId(String libraryId);

    ResultDO<PeptideDO> getById(String id);

    /**
     * 获取某一个标准库中所有的Transition的RT的取值范围
     *
     * @param libraryId
     * @return
     */
    Double[] getRTRange(String libraryId);

    ResultDO<List<Protein>> getProteinList(PeptideQuery query);

    /**
     * 计算不同蛋白质的数目
     *
     * @param libraryId
     * @return
     */
    Long countByProteinName(String libraryId);

    /**
     * 计算不同肽段的数目
     *
     * @param libraryId
     * @return
     */
    Long countByPeptideRef(String libraryId);

    /**
     * 创建MS1的坐标系
     *
     * @param libraryId
     * @param rtExtractionWindows
     * @return
     */
    List<TargetPeptide> buildMS1Coordinates(String libraryId, SlopeIntercept slopeIntercept, float rtExtractionWindows);

    /**
     * 创建MS2的坐标系
     *
     * @param libraryId
     * @param rtExtractionWindows
     * @param precursorMzStart
     * @param precursorMzEnd
     * @return
     */
    List<TargetPeptide> buildMS2Coordinates(String libraryId, SlopeIntercept slopeIntercept, float rtExtractionWindows, float precursorMzStart, float precursorMzEnd);

    /**
     * 根据LibraryId获取按照PeptideRef+IsDecoy进行分组的Intensity Map
     *
     * @param libraryId
     * @return
     */
    HashMap<String, IntensityGroup> getIntensityGroupMap(String libraryId);
}

