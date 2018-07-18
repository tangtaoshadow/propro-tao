package com.westlake.air.swathplatform.service.impl;

import com.westlake.air.swathplatform.constants.ResultCode;
import com.westlake.air.swathplatform.dao.ExperimentDAO;
import com.westlake.air.swathplatform.dao.LibraryDAO;
import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.bean.LibraryCoordinate;
import com.westlake.air.swathplatform.domain.bean.TargetTransition;
import com.westlake.air.swathplatform.domain.db.ExperimentDO;
import com.westlake.air.swathplatform.domain.db.LibraryDO;
import com.westlake.air.swathplatform.domain.query.ExperimentQuery;
import com.westlake.air.swathplatform.domain.query.LibraryQuery;
import com.westlake.air.swathplatform.domain.query.ScanIndexQuery;
import com.westlake.air.swathplatform.service.ExperimentService;
import com.westlake.air.swathplatform.service.LibraryService;
import com.westlake.air.swathplatform.service.ScanIndexService;
import com.westlake.air.swathplatform.service.TransitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 09:45
 */
@Service("experimentService")
public class ExperimentServiceImpl implements ExperimentService {

    public final Logger logger = LoggerFactory.getLogger(ExperimentServiceImpl.class);

    @Autowired
    ExperimentDAO experimentDAO;

    @Autowired
    TransitionService transitionService;

    @Autowired
    ScanIndexService scanIndexService;

    @Override
    public List<ExperimentDO> getAll() {
        return experimentDAO.getAll();
    }

    @Override
    public ResultDO<List<ExperimentDO>> getList(ExperimentQuery query) {
        List<ExperimentDO> libraryDOS = experimentDAO.getList(query);
        long totalCount = experimentDAO.count(query);
        ResultDO<List<ExperimentDO>> resultDO = new ResultDO<>(true);
        resultDO.setModel(libraryDOS);
        resultDO.setTotalNum(totalCount);
        resultDO.setPageSize(query.getPageSize());
        return resultDO;
    }

    @Override
    public ResultDO save(ExperimentDO experimentDO) {
        if (experimentDO.getName() == null || experimentDO.getName().isEmpty()) {
            return ResultDO.buildError(ResultCode.LIBRARY_NAME_CANNOT_BE_EMPTY);
        }
        try {
            experimentDO.setCreateDate(new Date());
            experimentDO.setLastModifiedDate(new Date());
            experimentDAO.insert(experimentDO);
            return ResultDO.build(experimentDO);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.INSERT_ERROR.getCode(), e.getMessage());
            return resultDO;
        }
    }

    @Override
    public ResultDO update(ExperimentDO experimentDO) {
        if (experimentDO.getId() == null || experimentDO.getId().isEmpty()) {
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        if (experimentDO.getName() == null || experimentDO.getName().isEmpty()) {
            return ResultDO.buildError(ResultCode.LIBRARY_NAME_CANNOT_BE_EMPTY);
        }

        try {
            experimentDO.setLastModifiedDate(new Date());
            experimentDAO.update(experimentDO);
            return ResultDO.build(experimentDO);
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
            experimentDAO.delete(id);
            return new ResultDO(true);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.DELETE_ERROR.getCode(), e.getMessage());
            return resultDO;
        }
    }

    @Override
    public ResultDO<ExperimentDO> getById(String id) {
        if (id == null || id.isEmpty()) {
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }

        try {
            ExperimentDO experimentDO = experimentDAO.getById(id);
            if (experimentDO == null) {
                return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
            } else {
                return ResultDO.build(experimentDO);
            }
        } catch (Exception e) {
            logger.warn(e.getMessage());
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.QUERY_ERROR.getCode(), e.getMessage());
            return resultDO;
        }
    }

    @Override
    public ResultDO<ExperimentDO> getByName(String name) {
        if (name == null || name.isEmpty()) {
            return ResultDO.buildError(ResultCode.LIBRARY_NAME_CANNOT_BE_EMPTY);
        }

        try {
            ExperimentDO experimentDO = experimentDAO.getByName(name);
            if (experimentDO == null) {
                return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
            } else {
                return ResultDO.build(experimentDO);
            }
        } catch (Exception e) {
            logger.warn(e.getMessage());
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(ResultCode.QUERY_ERROR.getCode(), e.getMessage());
            return resultDO;
        }
    }

    @Override
    public void extract(String expId, String libraryId, double rt_extract_window) {
        //构建卷积坐标
        LibraryCoordinate lc = transitionService.buildMS(libraryId, rt_extract_window);

        ScanIndexQuery query = new ScanIndexQuery();
        query.setExperimentId(expId);
        query.setMsLevel(1);
        Long ms1Count = scanIndexService.count(query);
        //如果MS1存在,则进行MS1的光谱扫描
        if(ms1Count > 0){
            for(TargetTransition ms1 : lc.getMs1List()){

            }
        }

        query.setMsLevel(2);
        Long ms2Count = scanIndexService.count(query);
        //如果MS2存在,则进行MS2的光谱扫描
        for(TargetTransition ms2 : lc.getMs2List()){

        }
    }
}
