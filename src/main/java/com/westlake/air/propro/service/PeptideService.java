package com.westlake.air.propro.service;

import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.aird.WindowRange;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.db.simple.Protein;
import com.westlake.air.propro.domain.db.simple.TargetPeptide;
import com.westlake.air.propro.domain.query.PeptideQuery;

import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 19:56
 */
public interface PeptideService {

    List<PeptideDO> getAllByLibraryId(String libraryId);

    PeptideDO getByLibraryIdAndPeptideRefAndIsDecoy(String libraryId, String peptideRef, boolean isDecoy);

    TargetPeptide getTargetPeptideByDataRef(String libraryId, String peptideRef, boolean isDecoy);

    List<PeptideDO> getAllByLibraryIdAndIsDecoy(String libraryId, boolean isDecoy);

    List<PeptideDO> getAllByLibraryIdAndProteinNameAndIsDecoy(String libraryId, String proteinName, boolean isDecoy);

    Long count(PeptideQuery query);

    ResultDO<List<PeptideDO>> getList(PeptideQuery peptideQuery);

    List<PeptideDO> getAll(PeptideQuery peptideQuery);

    ResultDO insert(PeptideDO peptideDO);

    ResultDO update(PeptideDO peptideDO);

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
    Long countByUniqueProteinName(String libraryId);

    /**
     * 创建MS2的坐标系
     *
     * @param library
     * @param rtExtractionWindows
     * @param range
     * @return
     */
    List<TargetPeptide> buildMS2Coordinates(LibraryDO library, SlopeIntercept slopeIntercept, float rtExtractionWindows, WindowRange range, Float[] rtRange, String type, boolean uniqueCheck, Boolean noDecoy);
}

