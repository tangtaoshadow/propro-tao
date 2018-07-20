package com.westlake.air.pecs.service.impl;

import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.dao.ScanIndexDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.SimpleScanIndex;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.domain.query.ScanIndexQuery;
import com.westlake.air.pecs.service.ScanIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-11 20:23
 */
@Service("scanIndexService")
public class ScanIndexServiceImpl implements ScanIndexService {

    public final Logger logger = LoggerFactory.getLogger(TransitionServiceImpl.class);

    @Autowired
    ScanIndexDAO scanIndexDAO;

    @Override
    public List<ScanIndexDO> getAllByExperimentId(String experimentId) {
        return scanIndexDAO.getAllByExperimentId(experimentId);
    }

    @Override
    public Long count(ScanIndexQuery query) {
        return scanIndexDAO.count(query);
    }

    @Override
    public ResultDO<List<ScanIndexDO>> getList(ScanIndexQuery query) {
        List<ScanIndexDO> indexList = scanIndexDAO.getList(query);
        long totalCount = scanIndexDAO.count(query);
        ResultDO<List<ScanIndexDO>> resultDO = new ResultDO<>(true);
        resultDO.setModel(indexList);
        resultDO.setTotalNum(totalCount);
        resultDO.setPageSize(query.getPageSize());
        return resultDO;
    }

    @Override
    public List<ScanIndexDO> getAll(ScanIndexQuery query) {
        return scanIndexDAO.getAll(query);
    }

    @Override
    public List<SimpleScanIndex> getSimpleAll(ScanIndexQuery query) {
        return scanIndexDAO.getSimpleAll(query);
    }

    @Override
    public ResultDO insert(ScanIndexDO scanIndexDO) {
        try {
            scanIndexDAO.insert(scanIndexDO);
            return new ResultDO(true);
        } catch (Exception e) {
            ResultDO resultDO = new ResultDO();
            return resultDO.setErrorResult(ResultCode.INSERT_ERROR.getMessage(), e.getMessage());
        }
    }

    @Override
    public ResultDO insertAll(List<ScanIndexDO> scanIndexes, boolean isDeleteOld) {
        if (scanIndexes == null || scanIndexes.size() == 0) {
            return ResultDO.buildError(ResultCode.OBJECT_CANNOT_BE_NULL);
        }
        try {
            if (isDeleteOld) {
                scanIndexDAO.deleteAllByExperimentId(scanIndexes.get(0).getExperimentId());
            }
            scanIndexDAO.insert(scanIndexes);
            return new ResultDO(true);
        } catch (Exception e) {
            ResultDO resultDO = new ResultDO();
            return resultDO.setErrorResult(ResultCode.INSERT_ERROR.getMessage(), e.getMessage());
        }
    }

    @Override
    public ResultDO deleteAllByExperimentId(String experimentId) {
        try {
            scanIndexDAO.deleteAllByExperimentId(experimentId);
            return new ResultDO(true);
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.DELETE_ERROR);
        }
    }

    @Override
    public ResultDO<ScanIndexDO> getById(String id) {
        try {
            ScanIndexDO scanIndexDO = scanIndexDAO.getById(id);
            if (scanIndexDO == null) {
                return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
            } else {
                ResultDO<ScanIndexDO> resultDO = new ResultDO<>(true);
                resultDO.setModel(scanIndexDO);
                return resultDO;
            }
        } catch (Exception e) {
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.QUERY_ERROR.getCode(), e.getMessage());
            return resultDO;
        }
    }
}
