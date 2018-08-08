package com.westlake.air.pecs.service.impl;

import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.test.dao.AnalyseDataDAO;
import com.westlake.air.pecs.test.dao.AnalyseOverviewDAO;
import com.westlake.air.pecs.test.dao.LibraryDAO;
import com.westlake.air.pecs.test.dao.TransitionDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.simple.Peptide;
import com.westlake.air.pecs.domain.db.simple.TransitionGroup;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.query.AnalyseDataQuery;
import com.westlake.air.pecs.service.AnalyseDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:10
 */
@Service("analyseDataService")
public class AnalyseDataServiceImpl implements AnalyseDataService {

    public final Logger logger = LoggerFactory.getLogger(AnalyseDataServiceImpl.class);

    @Autowired
    AnalyseDataDAO analyseDataDAO;
    @Autowired
    AnalyseOverviewDAO analyseOverviewDAO;
    @Autowired
    LibraryDAO libraryDAO;
    @Autowired
    TransitionDAO transitionDAO;

    @Override
    public List<AnalyseDataDO> getAllByOverviewId(String overviewId) {
        return analyseDataDAO.getAllByOverviewId(overviewId);
    }

    @Override
    public Long count(AnalyseDataQuery query) {
        return analyseDataDAO.count(query);
    }

    @Override
    public ResultDO<List<AnalyseDataDO>> getList(AnalyseDataQuery targetQuery) {
        List<AnalyseDataDO> dataList = analyseDataDAO.getList(targetQuery);
        long totalCount = analyseDataDAO.count(targetQuery);
        ResultDO<List<AnalyseDataDO>> resultDO = new ResultDO<>(true);
        resultDO.setModel(dataList);
        resultDO.setTotalNum(totalCount);
        resultDO.setPageSize(targetQuery.getPageSize());
        return resultDO;
    }

    @Override
    public ResultDO insert(AnalyseDataDO dataDO) {
        try {
            analyseDataDAO.insert(dataDO);
            return ResultDO.build(dataDO);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.INSERT_ERROR);
        }
    }

    @Override
    public ResultDO insertAll(List<AnalyseDataDO> dataList, boolean isDeleteOld) {
        if (dataList == null || dataList.size() == 0) {
            return ResultDO.buildError(ResultCode.OBJECT_CANNOT_BE_NULL);
        }
        try {
            if (isDeleteOld) {
                analyseDataDAO.deleteAllByOverviewId(dataList.get(0).getOverviewId());
            }
            analyseDataDAO.insert(dataList);
            return new ResultDO(true);
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.INSERT_ERROR);
        }
    }

    @Override
    public ResultDO delete(String id) {
        if (id == null || id.isEmpty()) {
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        try {
            analyseDataDAO.delete(id);
            return new ResultDO(true);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.DELETE_ERROR);
        }
    }

    @Override
    public ResultDO deleteAllByOverviewId(String overviewId) {
        if (overviewId == null || overviewId.isEmpty()) {
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        try {
            analyseDataDAO.deleteAllByOverviewId(overviewId);
            return new ResultDO(true);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.DELETE_ERROR);
        }
    }

    @Override
    public ResultDO<AnalyseDataDO> getById(String id) {
        try {
            AnalyseDataDO analyseDataDO = analyseDataDAO.getById(id);
            if (analyseDataDO == null) {
                return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
            } else {
                ResultDO<AnalyseDataDO> resultDO = new ResultDO<>(true);
                resultDO.setModel(analyseDataDO);
                return resultDO;
            }
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.QUERY_ERROR);
        }
    }

    @Override
    public ResultDO<AnalyseDataDO> getMS1Data(String overviewId, String peptideRef) {
        try {
            AnalyseDataDO analyseDataDO = analyseDataDAO.getMS1Data(overviewId, peptideRef);
            if (analyseDataDO == null) {
                return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
            } else {
                ResultDO<AnalyseDataDO> resultDO = new ResultDO<>(true);
                resultDO.setModel(analyseDataDO);
                return resultDO;
            }
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.QUERY_ERROR);
        }
    }

    @Override
    public ResultDO<AnalyseDataDO> getMS2Data(String overviewId, String peptideRef, String cutInfo) {
        try {
            AnalyseDataDO analyseDataDO = analyseDataDAO.getMS2Data(overviewId, peptideRef, cutInfo);
            if (analyseDataDO == null) {
                return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
            } else {
                ResultDO<AnalyseDataDO> resultDO = new ResultDO<>(true);
                resultDO.setModel(analyseDataDO);
                return resultDO;
            }
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.QUERY_ERROR);
        }
    }

    @Override
    public ResultDO<List<TransitionGroup>> getTransitionGroup(String overviewId, String vlibraryId) {
        LibraryDO libraryDO = libraryDAO.getById(vlibraryId);
        if (libraryDO.getType().equals(LibraryDO.TYPE_STANDARD)) {
            return ResultDO.buildError(ResultCode.ONLY_SUPPORT_VERIFY_LIBRARY_SEARCH);
        }

        List<Peptide> peptides = transitionDAO.getPeptideList(vlibraryId);
        List<TransitionGroup> groups = new ArrayList<>();

        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setOverviewId(overviewId);
        query.setPageSize(10000);
        for (Peptide peptide : peptides) {
            query.setPeptideRef(peptide.getPeptideRef());
            List<AnalyseDataDO> dataList = analyseDataDAO.getList(query);
            TransitionGroup group = new TransitionGroup();
            group.setPeptideRef(peptide.getPeptideRef());
            group.setProteinName(peptide.getProteinName());
            group.setRt(peptide.getRt());
            group.setDataList(dataList);
            groups.add(group);
        }
        ResultDO<List<TransitionGroup>> resultDO = new ResultDO<>(true);
        resultDO.setModel(groups);
        resultDO.setTotalNum(libraryDO.getPeptideCount());
        resultDO.setPageSize(query.getPageSize());
        return resultDO;
    }
}
