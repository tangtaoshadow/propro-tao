package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.LibraryCoordinate;
import com.westlake.air.pecs.domain.bean.TransitionGroup;
import com.westlake.air.pecs.domain.db.simple.Peptide;
import com.westlake.air.pecs.domain.db.simple.Protein;
import com.westlake.air.pecs.domain.db.simple.TargetTransition;
import com.westlake.air.pecs.domain.db.TransitionDO;
import com.westlake.air.pecs.domain.query.PageQuery;
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

    ResultDO insert(TransitionDO transitionDO);

    ResultDO insertAll(List<TransitionDO> transitions, boolean isDeleteOld);

    ResultDO deleteAllByLibraryId(String libraryId);

    ResultDO deleteAllDecoyByLibraryId(String libraryId);

    ResultDO<TransitionDO> getById(String id);

    Double[] getRTRange(String libraryId);

    ResultDO<List<Protein>> getProteinList(TransitionQuery query);

    ResultDO<List<Peptide>> getPeptideList(TransitionQuery query);

    Long countByProteinName(String libraryId);

    Long countByPeptideRef(String libraryId);

    LibraryCoordinate buildCoordinates(String libraryId, float rtExtractionWindows);

    List<TargetTransition> buildMS1Coordinates(String libraryId, float rtExtractionWindows);

    List<TargetTransition> buildMS2Coordinates(String libraryId, float rtExtractionWindows,float precursorMzStart, float precursorMzEnd);

}
