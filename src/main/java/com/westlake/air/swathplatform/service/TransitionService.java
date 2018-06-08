package com.westlake.air.swathplatform.service;

import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.db.TransitionDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import javax.xml.transform.Result;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 19:56
 */
public interface TransitionService {

    Page<TransitionDO> findAll(PageRequest pageRequest);

    ResultDO insert(TransitionDO transitionDO);

    ResultDO insertAll(List<TransitionDO> transitions);

    ResultDO deleteAllByLibraryId(String libraryId);

    ResultDO<TransitionDO> getById(String id);

    Integer countByProteinName(String libraryId);

    Integer countByPeptideSequence(String libraryId);

    Integer countByTransitionName(String libraryId);

    ResultDO<List<TransitionDO>> findAllByLibraryIdAndPeptideGroupLabel(String libraryId, String peptideGroupLabel);

    ResultDO<List<TransitionDO>> findAllByLibraryIdAndPeptideSequence(String libraryId, String peptideSequence);

    ResultDO<List<TransitionDO>> findAllByLibraryIdAndProteinName(String libraryId, String proteinName);

    Page<TransitionDO> findAllByLibraryIdAndIsDecoy(String libraryId, Boolean isDecoy, PageRequest pageRequest);

}
