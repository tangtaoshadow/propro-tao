package com.westlake.air.swathplatform.service.impl;

import com.westlake.air.swathplatform.constants.ResultCode;
import com.westlake.air.swathplatform.dao.ConvolutionDataDAO;
import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.db.ConvolutionDataDO;
import com.westlake.air.swathplatform.domain.query.ConvolutionDataQuery;
import com.westlake.air.swathplatform.service.ConvolutionDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:10
 */
public class ConvolutionDataServiceImpl implements ConvolutionDataService {

    public final Logger logger = LoggerFactory.getLogger(ConvolutionDataServiceImpl.class);

    @Autowired
    ConvolutionDataDAO convolutionDataDAO;

    @Override
    public List<ConvolutionDataDO> getAllByExpId(String expId) {
        return convolutionDataDAO.getAllByExperimentId(expId);
    }

    @Override
    public Long count(ConvolutionDataQuery query) {
        return convolutionDataDAO.count(query);
    }

    @Override
    public ResultDO<List<ConvolutionDataDO>> getList(ConvolutionDataQuery convQuery) {
        List<ConvolutionDataDO> dataList = convolutionDataDAO.getList(convQuery);
        long totalCount = convolutionDataDAO.count(convQuery);
        ResultDO<List<ConvolutionDataDO>> resultDO = new ResultDO<>(true);
        resultDO.setModel(dataList);
        resultDO.setTotalNum(totalCount);
        resultDO.setPageSize(convQuery.getPageSize());
        return resultDO;
    }

    @Override
    public ResultDO insert(ConvolutionDataDO convData) {

        try {
            convolutionDataDAO.insert(convData);
            return ResultDO.build(convData);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.INSERT_ERROR.getCode(), e.getMessage());
            return resultDO;
        }
    }

    @Override
    public ResultDO insertAll(List<ConvolutionDataDO> convList, boolean isDeleteOld) {
        if (convList == null || convList.size() == 0) {
            return ResultDO.buildError(ResultCode.OBJECT_CANNOT_BE_NULL);
        }
        try {
            if (isDeleteOld) {
                convolutionDataDAO.deleteAllByExpId(convList.get(0).getExpId());
            }
            convolutionDataDAO.insert(convList);
            return new ResultDO(true);
        } catch (Exception e) {
            ResultDO resultDO = new ResultDO();
            return resultDO.setErrorResult(ResultCode.INSERT_ERROR.getMessage(), e.getMessage());
        }
    }

    @Override
    public ResultDO delete(String id) {
        if (id == null || id.isEmpty()) {
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        try {
            convolutionDataDAO.delete(id);
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
            convolutionDataDAO.deleteAllByExpId(expId);
            return new ResultDO(true);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.DELETE_ERROR.getCode(), e.getMessage());
            return resultDO;
        }
    }

    @Override
    public ResultDO<ConvolutionDataDO> getById(String id) {
        try {
            ConvolutionDataDO convolutionDataDO = convolutionDataDAO.getById(id);
            if (convolutionDataDO == null) {
                return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
            } else {
                ResultDO<ConvolutionDataDO> resultDO = new ResultDO<>(true);
                resultDO.setModel(convolutionDataDO);
                return resultDO;
            }
        } catch (Exception e) {
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.QUERY_ERROR.getCode(), e.getMessage());
            return resultDO;
        }
    }
}
