package com.westlake.air.propro.service.impl;

import com.westlake.air.propro.constants.enums.ResultCode;
import com.westlake.air.propro.dao.AnalyseDataDAO;
import com.westlake.air.propro.dao.AnalyseOverviewDAO;
import com.westlake.air.propro.dao.LibraryDAO;
import com.westlake.air.propro.dao.PeptideDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.AnalyseDataRT;
import com.westlake.air.propro.domain.bean.score.SimpleFeatureScores;
import com.westlake.air.propro.domain.db.AnalyseDataDO;
import com.westlake.air.propro.domain.db.simple.MatchedPeptide;
import com.westlake.air.propro.domain.db.simple.PeptideScores;
import com.westlake.air.propro.domain.query.AnalyseDataQuery;
import com.westlake.air.propro.service.AnalyseDataService;
import com.westlake.air.propro.utils.AnalyseUtil;
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
    PeptideDAO peptideDAO;

    @Override
    public List<AnalyseDataDO> getAllByOverviewId(String overviewId) {
        return analyseDataDAO.getAllByOverviewId(overviewId);
    }

    @Override
    public List<PeptideScores> getSimpleScoresByOverviewId(String overviewId) {
        return analyseDataDAO.getSimpleScoresByOverviewId(overviewId);
    }

    @Override
    public List<MatchedPeptide> getAllSuccessMatchedPeptides(String overviewId) {
        AnalyseDataQuery query = new AnalyseDataQuery(overviewId);
        query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_SUCCESS);
        return analyseDataDAO.getAllMatchedPeptide(query);
    }

    @Override
    public AnalyseDataDO getByOverviewIdAndPeptideRefAndIsDecoy(String overviewId, String peptideRef, Boolean isDecoy) {
        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setOverviewId(overviewId);
        query.setPeptideRef(peptideRef);
        query.setIsDecoy(isDecoy);
        return analyseDataDAO.getOne(query);
    }

    @Override
    public Long count(AnalyseDataQuery query) {
        return analyseDataDAO.count(query);
    }

    @Override
    public ResultDO<List<AnalyseDataDO>> getList(AnalyseDataQuery query) {
        List<AnalyseDataDO> dataList = analyseDataDAO.getList(query);
        //暂时去除count功能,以免造成性能损耗
        ResultDO<List<AnalyseDataDO>> resultDO = new ResultDO<>(true);
        resultDO.setModel(dataList);
        resultDO.setTotalNum(100000);
        resultDO.setPageSize(query.getPageSize());
        return resultDO;
    }

    @Override
    public List<AnalyseDataDO> getAll(AnalyseDataQuery query) {
        return analyseDataDAO.getAll(query);
    }

    @Override
    public ResultDO insert(AnalyseDataDO dataDO) {
        try {
            dataDO.setDataRef(AnalyseUtil.getDataRef(dataDO.getOverviewId(), dataDO.getPeptideRef(), dataDO.getIsDecoy()));
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
            for (AnalyseDataDO dataDO : dataList) {
                dataDO.setDataRef(AnalyseUtil.getDataRef(dataDO.getOverviewId(), dataDO.getPeptideRef(), dataDO.getIsDecoy()));
            }
            analyseDataDAO.insert(dataList);
            return new ResultDO(true);
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.INSERT_ERROR);
        }
    }

    @Override
    public ResultDO update(AnalyseDataDO dataDO) {
        if (dataDO.getId() == null) {
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }

        try {
            analyseDataDAO.update(dataDO);
            return ResultDO.build(dataDO);
        } catch (Exception e) {
            logger.warn(e.getMessage());
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
    public List<AnalyseDataRT> getRtList(AnalyseDataQuery query) {
        return analyseDataDAO.getRtList(query);
    }

    @Override
    public void updateMulti(String overviewId, List<SimpleFeatureScores> simpleFeatureScoresList) {
        analyseDataDAO.updateMulti(overviewId, simpleFeatureScoresList);
    }

    /**
     * 将数组中的FDR小于指定值的伪肽段删除,同时将数据库中对应的伪肽段也删除
     * @param overviewId
     * @param simpleFeatureScoresList
     * @param fdr
     */
    @Override
    public void removeMultiDecoy(String overviewId, List<SimpleFeatureScores> simpleFeatureScoresList, Double fdr) {
        List<SimpleFeatureScores> decoyNeedToRemove = new ArrayList<>();
        for (int i = simpleFeatureScoresList.size() - 1; i >= 0; i--) {
            //如果是伪肽段,并且fdr为空或者fdr小于指定的值,那么删除它
            if(simpleFeatureScoresList.get(i).getIsDecoy() &&
                    (simpleFeatureScoresList.get(i).getFdr() == null || simpleFeatureScoresList.get(i).getFdr() > fdr)){
                decoyNeedToRemove.add(simpleFeatureScoresList.get(i));
                simpleFeatureScoresList.remove(i);
            }
        }

        logger.info("删除数据:"+decoyNeedToRemove.size()+"条");
        if(decoyNeedToRemove.size() != 0){
            analyseDataDAO.deleteMulti(overviewId, decoyNeedToRemove);
        }
    }
}
