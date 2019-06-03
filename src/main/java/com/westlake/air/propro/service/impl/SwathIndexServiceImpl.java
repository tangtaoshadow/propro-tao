package com.westlake.air.propro.service.impl;

import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.dao.SwathIndexDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.propro.domain.db.SwathIndexDO;
import com.westlake.air.propro.domain.query.SwathIndexQuery;
import com.westlake.air.propro.service.SwathIndexService;
import com.westlake.air.propro.utils.ConvolutionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.TreeMap;

@Service("swathIndexService")
public class SwathIndexServiceImpl implements SwathIndexService {

    public final Logger logger = LoggerFactory.getLogger(SwathIndexServiceImpl.class);

    @Autowired
    SwathIndexDAO swathIndexDAO;

    @Override
    public List<SwathIndexDO> getAllByExpId(String expId) {
        return swathIndexDAO.getAllByExpId(expId);
    }

    @Override
    public List<SwathIndexDO> getAllMS2ByExpId(String expId) {
        return swathIndexDAO.getAllMS2ByExpId(expId);
    }

    @Override
    public Long count(SwathIndexQuery query) {
        return swathIndexDAO.count(query);
    }

    @Override
    public ResultDO<List<SwathIndexDO>> getList(SwathIndexQuery query) {
        List<SwathIndexDO> indexList = swathIndexDAO.getList(query);
        long totalCount = swathIndexDAO.count(query);
        ResultDO<List<SwathIndexDO>> resultDO = new ResultDO<>(true);
        resultDO.setModel(indexList);
        resultDO.setTotalNum(totalCount);
        resultDO.setPageSize(query.getPageSize());
        return resultDO;
    }

    @Override
    public MzIntensityPairs getNearestSpectrumByRt(TreeMap<Float, MzIntensityPairs> rtMap, Double rt) {
        Float[] fArray = new Float[rtMap.keySet().size()];
        rtMap.keySet().toArray(fArray);
        int leftIndex = ConvolutionUtil.findLeftIndex(fArray, rt.floatValue());
        int finalIndex = leftIndex;
        try{
            if((fArray[leftIndex] - rt) > (fArray[leftIndex+1] - rt)){
                finalIndex = leftIndex + 1;
            }
        }catch (Exception e){
            //leftIndex == fArray.length-1的时候数组溢出,直接返回finalIndex
            return rtMap.get(fArray[finalIndex]);
        }
        return rtMap.get(fArray[finalIndex]);
    }

    @Override
    public List<SwathIndexDO> getAll(SwathIndexQuery query) {
        return swathIndexDAO.getAll(query);
    }

    @Override
    public SwathIndexDO getSwathIndex(String expId, Float mz) {
        SwathIndexQuery query = new SwathIndexQuery(expId, 2);
        query.setMz(mz);
        return swathIndexDAO.getOne(query);
    }

    @Override
    public ResultDO insert(SwathIndexDO swathIndexDO) {
        try {
            swathIndexDAO.insert(swathIndexDO);
            return new ResultDO(true);
        } catch (Exception e) {
            ResultDO resultDO = new ResultDO();
            return resultDO.setErrorResult(ResultCode.INSERT_ERROR.getMessage(), e.getMessage());
        }
    }

    @Override
    public ResultDO update(SwathIndexDO swathIndexDO) {
        try {
            swathIndexDAO.update(swathIndexDO);
            return new ResultDO(true);
        } catch (Exception e) {
            ResultDO resultDO = new ResultDO();
            return resultDO.setErrorResult(ResultCode.UPDATE_ERROR.getMessage(), e.getMessage());
        }
    }

    @Override
    public ResultDO insertAll(List<SwathIndexDO> swathIndexList, boolean isDeleteOld) {
        if (swathIndexList == null || swathIndexList.size() == 0) {
            return ResultDO.buildError(ResultCode.OBJECT_CANNOT_BE_NULL);
        }
        try {
            if (isDeleteOld) {
                swathIndexDAO.deleteAllByExpId(swathIndexList.get(0).getExpId());
            }
            swathIndexDAO.insert(swathIndexList);
            return new ResultDO(true);
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.INSERT_ERROR);
        }
    }

    @Override
    public ResultDO deleteAllByExpId(String expId) {
        try {
            swathIndexDAO.deleteAllByExpId(expId);
            return new ResultDO(true);
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.DELETE_ERROR);
        }
    }

    @Override
    public SwathIndexDO getById(String id) {
        try {
            return swathIndexDAO.getById(id);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return null;
        }
    }
}
