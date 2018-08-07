package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.LibraryCoordinate;
import com.westlake.air.pecs.domain.db.simple.Peptide;
import com.westlake.air.pecs.domain.db.simple.Protein;
import com.westlake.air.pecs.domain.db.simple.TargetTransition;
import com.westlake.air.pecs.domain.db.TransitionDO;
import com.westlake.air.pecs.domain.query.TransitionQuery;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 19:56
 */
public interface TransitionService {

    List<TransitionDO> getAllByLibraryId(String libraryId);

    List<TransitionDO> getAllByLibraryIdAndIsDecoy(String libraryId,boolean isDecoy);

    Long count(TransitionQuery query);

    ResultDO<List<TransitionDO>> getList(TransitionQuery transitionQuery);

    List<Double> getIntensityList(String libraryId);

    ResultDO insert(TransitionDO transitionDO);

    ResultDO insertAll(List<TransitionDO> transitions, boolean isDeleteOld);

    ResultDO deleteAllByLibraryId(String libraryId);

    ResultDO deleteAllDecoyByLibraryId(String libraryId);

    ResultDO<TransitionDO> getById(String id);

    /**
     * 获取某一个标准库中所有的Transition的RT的取值范围
     * @param libraryId
     * @return
     */
    Double[] getRTRange(String libraryId);

    ResultDO<List<Protein>> getProteinList(TransitionQuery query);

    ResultDO<List<Peptide>> getPeptideList(TransitionQuery query);

    /**
     * 计算不同蛋白质的数目
     * @param libraryId
     * @return
     */
    Long countByProteinName(String libraryId);

    /**
     * 计算不同肽段的数目
     * @param libraryId
     * @return
     */
    Long countByPeptideRef(String libraryId);

    /**
     * 同时创建MS1和MS2的卷积坐标
     * @param libraryId
     * @param rtExtractionWindows
     * @return
     */
    LibraryCoordinate buildCoordinates(String libraryId, float rtExtractionWindows);

    /**
     * 创建MS1的坐标系
     * @param libraryId
     * @param rtExtractionWindows
     * @return
     */
    List<TargetTransition> buildMS1Coordinates(String libraryId, float rtExtractionWindows);

    /**
     * 创建MS2的坐标系
     * @param libraryId
     * @param rtExtractionWindows
     * @param precursorMzStart
     * @param precursorMzEnd
     * @return
     */
    List<TargetTransition> buildMS2Coordinates(String libraryId, float rtExtractionWindows,float precursorMzStart, float precursorMzEnd);

}
