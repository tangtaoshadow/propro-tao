package com.westlake.air.swathplatform.service.impl;

import com.westlake.air.swathplatform.constants.ResultCode;
import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.db.TransitionDO;
import com.westlake.air.swathplatform.dao.TransitionDAO;
import com.westlake.air.swathplatform.repository.TransitionRepo;
import com.westlake.air.swathplatform.service.TransitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 20:02
 */
@Service("transitionService")
public class TransitionServiceImpl implements TransitionService {

    public final Logger logger = LoggerFactory.getLogger(TransitionServiceImpl.class);

    @Autowired
    TransitionRepo transitionRepo;

    @Autowired
    TransitionDAO transitionDAO;

    @Override
    public Page<TransitionDO> findAll(PageRequest pageRequest) {
        return transitionRepo.findAll(pageRequest);
    }

    @Override
    public ResultDO insert(TransitionDO transitionDO) {
        try {
            transitionRepo.insert(transitionDO);
            return new ResultDO(true);
        } catch (Exception e) {
            ResultDO resultDO = new ResultDO();
            return resultDO.setErrorResult(ResultCode.INSERT_ERROR.getMessage(), e.getMessage());
        }
    }

    @Override
    public ResultDO insertAll(List<TransitionDO> transitions) {
        try {
            transitionRepo.insert(transitions);
            return new ResultDO(true);
        } catch (Exception e) {
            ResultDO resultDO = new ResultDO();
            return resultDO.setErrorResult(ResultCode.INSERT_ERROR.getMessage(), e.getMessage());
        }
    }

    @Override
    public ResultDO deleteAllByLibraryId(String libraryId) {
        try {
            transitionDAO.deleteAllByLibraryId(libraryId);
            return new ResultDO(true);
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.DELETE_ERROR);
        }
    }

    @Override
    public ResultDO<TransitionDO> getById(String id) {
        try {
            TransitionDO transitionDO = transitionDAO.getById(id);
            if (transitionDO == null) {
                return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
            } else {
                return new ResultDO(true);
            }
        } catch (Exception e) {
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.QUERY_ERROR.getCode(), e.getMessage());
            return resultDO;
        }
    }

    @Override
    public Integer countByProteinName(String libraryId) {
        return transitionDAO.countByProteinName(libraryId);
    }

    @Override
    public Integer countByPeptideSequence(String libraryId) {
        return transitionDAO.countByPeptideSequence(libraryId);
    }

    @Override
    public Integer countByTransitionName(String libraryId) {
        return transitionDAO.countByTransitionName(libraryId);
    }

    @Override
    public ResultDO<List<TransitionDO>> findAllByLibraryIdAndPeptideGroupLabel(String libraryId, String peptideGroupLabel) {
        try {
            List<TransitionDO> transitions = transitionRepo.getAllByLibraryIdAndPeptideGroupLabel(libraryId, peptideGroupLabel);
            ResultDO resultDO = new ResultDO(true);
            resultDO.setModel(transitions);
            return resultDO;
        } catch (Exception e) {
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.QUERY_ERROR.getCode(), e.getMessage());
            return resultDO;
        }
    }

    @Override
    public ResultDO<List<TransitionDO>> findAllByLibraryIdAndPeptideSequence(String libraryId, String peptideSequence) {
        try {
            List<TransitionDO> transitions = transitionRepo.getAllByLibraryIdAndPeptideSequence(libraryId, peptideSequence);
            ResultDO resultDO = new ResultDO(true);
            resultDO.setModel(transitions);
            return resultDO;
        } catch (Exception e) {
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.QUERY_ERROR.getCode(), e.getMessage());
            return resultDO;
        }
    }

    @Override
    public ResultDO<List<TransitionDO>> findAllByLibraryIdAndProteinName(String libraryId, String proteinName) {
        try {
            List<TransitionDO> transitions = transitionRepo.getAllByLibraryIdAndProteinName(libraryId, proteinName);
            ResultDO resultDO = new ResultDO(true);
            resultDO.setModel(transitions);
            return resultDO;
        } catch (Exception e) {
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.QUERY_ERROR.getCode(), e.getMessage());
            return resultDO;
        }
    }

    @Override
    public Page<TransitionDO> findAllByLibraryIdAndIsDecoy(String libraryId, Boolean isDecoy, PageRequest pageRequest) {
        try {
            Page<TransitionDO> page = transitionRepo.getAllByLibraryIdAndIsDecoy(libraryId, isDecoy, pageRequest);
            return page;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }
}
