package com.westlake.air.swathplatform.service.impl;

import com.westlake.air.swathplatform.constants.ResultCode;
import com.westlake.air.swathplatform.dao.AnalyseRecordDAO;
import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.db.AnalyseRecordDO;
import com.westlake.air.swathplatform.domain.query.AnalyseRecordQuery;
import com.westlake.air.swathplatform.service.AnalyseRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:40
 */
@Service("analyseRecordService")
public class AnalyseRecordServiceImpl implements AnalyseRecordService {

    public final Logger logger = LoggerFactory.getLogger(AnalyseRecordServiceImpl.class);

    @Autowired
    AnalyseRecordDAO analyseRecordDAO;

    @Override
    public List<AnalyseRecordDO> getAllByExpId(String expId) {
        return analyseRecordDAO.getAllByExperimentId(expId);
    }

    @Override
    public Long count(AnalyseRecordQuery query) {
        return analyseRecordDAO.count(query);
    }

    @Override
    public ResultDO<List<AnalyseRecordDO>> getList(AnalyseRecordQuery targetQuery) {
        List<AnalyseRecordDO> dataList = analyseRecordDAO.getList(targetQuery);
        long totalCount = analyseRecordDAO.count(targetQuery);
        ResultDO<List<AnalyseRecordDO>> resultDO = new ResultDO<>(true);
        resultDO.setModel(dataList);
        resultDO.setTotalNum(totalCount);
        resultDO.setPageSize(targetQuery.getPageSize());
        return resultDO;
    }

    @Override
    public ResultDO insert(AnalyseRecordDO recordDO) {
        try {
            analyseRecordDAO.insert(recordDO);
            return ResultDO.build(recordDO);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.INSERT_ERROR.getCode(), e.getMessage());
            return resultDO;
        }
    }

    @Override
    public ResultDO delete(String id) {
        if (id == null || id.isEmpty()) {
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        try {
            analyseRecordDAO.delete(id);
            return new ResultDO(true);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.DELETE_ERROR.getCode(), e.getMessage());
            return resultDO;
        }
    }

    @Override
    public ResultDO deleteAllByExpId(String expId) {
        if (expId == null || expId.isEmpty()) {
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        try {
            analyseRecordDAO.deleteAllByExperimentId(expId);
            return new ResultDO(true);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.DELETE_ERROR.getCode(), e.getMessage());
            return resultDO;
        }
    }

    @Override
    public ResultDO<AnalyseRecordDO> getById(String id) {
        try {
            AnalyseRecordDO analyseRecordDO = analyseRecordDAO.getById(id);
            if (analyseRecordDO == null) {
                return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
            } else {
                ResultDO<AnalyseRecordDO> resultDO = new ResultDO<>(true);
                resultDO.setModel(analyseRecordDO);
                return resultDO;
            }
        } catch (Exception e) {
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.QUERY_ERROR.getCode(), e.getMessage());
            return resultDO;
        }
    }
}
