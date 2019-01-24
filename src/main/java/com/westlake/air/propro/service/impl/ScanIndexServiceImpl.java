package com.westlake.air.propro.service.impl;

import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.dao.ScanIndexDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.propro.domain.db.simple.SimpleScanIndex;
import com.westlake.air.propro.domain.db.ScanIndexDO;
import com.westlake.air.propro.domain.query.ScanIndexQuery;
import com.westlake.air.propro.parser.AirdFileParser;
import com.westlake.air.propro.service.ScanIndexService;
import com.westlake.air.propro.utils.ConvolutionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-11 20:23
 */
@Service("scanIndexService")
public class ScanIndexServiceImpl implements ScanIndexService {

    public final Logger logger = LoggerFactory.getLogger(PeptideServiceImpl.class);

    @Autowired
    ScanIndexDAO scanIndexDAO;
    @Autowired
    AirdFileParser airdFileParser;

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
    public List<ScanIndexDO> getAll(ScanIndexQuery query) {
        return scanIndexDAO.getAll(query);
    }

    @Override
    public List<SimpleScanIndex> getSimpleAll(ScanIndexQuery query) {
        return scanIndexDAO.getSimpleAll(query);
    }

    @Override
    public List<SimpleScanIndex> getSimpleList(ScanIndexQuery query) {
        return scanIndexDAO.getSimpleList(query);
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
    public ResultDO update(ScanIndexDO scanIndexDO) {
        try {
            scanIndexDAO.update(scanIndexDO);
            return new ResultDO(true);
        } catch (Exception e) {
            ResultDO resultDO = new ResultDO();
            return resultDO.setErrorResult(ResultCode.UPDATE_ERROR.getMessage(), e.getMessage());
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
            return ResultDO.buildError(ResultCode.INSERT_ERROR);
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
    public ResultDO deleteAllSwathIndexByExperimentId(String experimentId) {
        try {
            scanIndexDAO.deleteAllSwathIndexByExperimentId(experimentId);
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
            return ResultDO.buildError(ResultCode.QUERY_ERROR);
        }
    }

    @Override
    public HashMap<Float, ScanIndexDO> getSwathIndexList(String expId) {
        ScanIndexQuery query = new ScanIndexQuery(expId, 0);
        List<ScanIndexDO> indexes = scanIndexDAO.getAll(query);
        if (indexes == null || indexes.size() == 0) {
            return null;
        }
        HashMap<Float, ScanIndexDO> map = new HashMap<>();
        for (ScanIndexDO index : indexes) {
            map.put(index.getPrecursorMzStart(), index);
        }
        return map;
    }

    @Override
    public ScanIndexDO getSwathIndex(String expId, Float targetPrecursorMz) {
        ScanIndexQuery query = new ScanIndexQuery(expId, 0);
        query.setTargetPrecursorMz(targetPrecursorMz);
        ScanIndexDO index = scanIndexDAO.getOne(query);

        return index;
    }
}
