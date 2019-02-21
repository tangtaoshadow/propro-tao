package com.westlake.air.propro.service.impl;

import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.dao.AnalyseDataDAO;
import com.westlake.air.propro.dao.AnalyseOverviewDAO;
import com.westlake.air.propro.dao.LibraryDAO;
import com.westlake.air.propro.dao.PeptideDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.AnalyseDataDO;
import com.westlake.air.propro.domain.db.AnalyseOverviewDO;
import com.westlake.air.propro.domain.db.simple.MatchedPeptide;
import com.westlake.air.propro.domain.db.simple.SimpleScores;
import com.westlake.air.propro.domain.query.AnalyseDataQuery;
import com.westlake.air.propro.service.AnalyseDataService;
import com.westlake.air.propro.utils.AnalyseDataUtil;
import com.westlake.air.propro.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.RandomAccessFile;
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
    public List<SimpleScores> getSimpleScoresByOverviewId(String overviewId) {
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
        List<AnalyseDataDO> datas = analyseDataDAO.getAll(query);
        if (datas.size() == 1) {
            return datas.get(0);
        } else {
            return null;
        }
    }

    @Override
    public Long count(AnalyseDataQuery query) {
        return analyseDataDAO.count(query);
    }

    @Override
    public ResultDO<List<AnalyseDataDO>> getList(AnalyseDataQuery query) {
        List<AnalyseDataDO> dataList = analyseDataDAO.getList(query);
        long totalCount = analyseDataDAO.count(query);
        ResultDO<List<AnalyseDataDO>> resultDO = new ResultDO<>(true);
        resultDO.setModel(dataList);
        resultDO.setTotalNum(totalCount);
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
    public ResultDO<AnalyseDataDO> getByIdWithConvolutionData(String id) {
        RandomAccessFile raf = null;
        try {
            AnalyseDataDO analyseDataDO = analyseDataDAO.getById(id);
            if (analyseDataDO == null) {
                return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
            } else {
                AnalyseOverviewDO overview = analyseOverviewDAO.getById(analyseDataDO.getOverviewId());
                if (overview == null) {
                    return ResultDO.buildError(ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED);
                }
                if (overview.getAircPath() == null || overview.getAircPath().isEmpty()) {
                    return ResultDO.buildError(ResultCode.AIRC_FILE_PATH_NOT_EXISTED);
                }
                File file = new File(overview.getAircPath());
                if (!file.exists()) {
                    return ResultDO.buildError(ResultCode.AIRC_FILE_NOT_EXISTED);
                }
                if (analyseDataDO.getPosDeltaList().size() != (analyseDataDO.getMzMap().size() + 1)) {
                    return ResultDO.buildError(ResultCode.POSITION_DELTA_LIST_LENGTH_NOT_EQUAL_TO_MZMAP_PLUS_ONE);
                }
                raf = new RandomAccessFile(file, "r");
                ResultDO<AnalyseDataDO> result = AnalyseDataUtil.readConvDataFromFile(analyseDataDO, raf);
                return result;
            }
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.QUERY_ERROR);
        } finally {
            FileUtil.close(raf);
        }
    }

    @Override
    public ResultDO<List<AnalyseDataDO>> getListWithConvolutionData(AnalyseDataQuery query) {
        RandomAccessFile raf = null;
        try {
            List<AnalyseDataDO> dataList = analyseDataDAO.getList(query);

            AnalyseOverviewDO overview = analyseOverviewDAO.getById(query.getOverviewId());
            if (overview == null) {
                return ResultDO.buildError(ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED);
            }
            if (overview.getAircPath() == null || overview.getAircPath().isEmpty()) {
                return ResultDO.buildError(ResultCode.AIRC_FILE_PATH_NOT_EXISTED);
            }
            File file = new File(overview.getAircPath());
            if (!file.exists()) {
                return ResultDO.buildError(ResultCode.AIRC_FILE_NOT_EXISTED);
            }

            raf = new RandomAccessFile(file, "r");
            for (AnalyseDataDO analyseDataDO : dataList) {
                ResultDO<AnalyseDataDO> res = AnalyseDataUtil.readConvDataFromFile(analyseDataDO, raf);
                if(res.isFailed()){
                    logger.error(res.getMsgInfo());
                }
            }

            ResultDO<List<AnalyseDataDO>> resultDO = new ResultDO<>(true);
            resultDO.setModel(dataList);
            return resultDO;

        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.QUERY_ERROR);
        } finally {
            FileUtil.close(raf);
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
    public ResultDO<AnalyseDataDO> getMS2Data(String overviewId, String peptideRef, Boolean isDecoy) {
        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setIsDecoy(isDecoy);
        query.setOverviewId(overviewId);
        query.setPeptideRef(peptideRef);
        List<AnalyseDataDO> dataList = analyseDataDAO.getAll(query);
        ResultDO<AnalyseDataDO> resultDO = new ResultDO<>();
        if (dataList == null || dataList.size() == 0) {
            return ResultDO.buildError(ResultCode.ANALYSE_DATA_NOT_EXISTED);
        }

        resultDO.setSuccess(true);
        resultDO.setModel(dataList.get(0));
        return resultDO;
    }
}