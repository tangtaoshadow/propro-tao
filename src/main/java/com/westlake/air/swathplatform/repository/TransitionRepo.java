package com.westlake.air.swathplatform.repository;

import com.westlake.air.swathplatform.domain.db.TransitionDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-04 21:54
 */
public interface TransitionRepo extends MongoRepository<TransitionDO, String> {

    TransitionDO getById(String id);

    void deleteAllByLibraryId(String libraryId);

    List<TransitionDO> getAllByLibraryIdAndTransitionGroupId(String libraryId, String transitionGroupId);

    List<TransitionDO> getAllByLibraryIdAndPeptideSequence(String libraryId, String peptideSequence);

    List<TransitionDO> getAllByLibraryIdAndProteinName(String libraryId, String proteinName);

    Page<TransitionDO> getAllByLibraryIdAndIsDecoy(String libraryId, Boolean isDecoy, Pageable page);

}
