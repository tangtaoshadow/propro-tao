package com.westlake.air.pecs.service.impl;

import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.dao.AnalyseDataDAO;
import com.westlake.air.pecs.dao.AnalyseOverviewDAO;
import com.westlake.air.pecs.dao.LibraryDAO;
import com.westlake.air.pecs.dao.TransitionDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.simple.Peptide;
import com.westlake.air.pecs.domain.db.simple.TransitionGroup;
import com.westlake.air.pecs.domain.query.AnalyseDataQuery;
import com.westlake.air.pecs.domain.query.PageQuery;
import com.westlake.air.pecs.service.AnalyseDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
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
    public List<TransitionGroup> getTransitionGroup(List<AnalyseDataDO> dataList) {
        HashMap<String, TransitionGroup> groupMap = new HashMap<>();
        HashMap<String, TransitionGroup> groupMapDecoy = new HashMap<>();

        for (AnalyseDataDO data : dataList) {
            if(data.getIsDecoy()){
                if (groupMapDecoy.get(data.getPeptideRef()) == null) {
                    TransitionGroup group = new TransitionGroup(data.getProteinName(), data.getPeptideRef(), Double.parseDouble(data.getRt().toString()), data.getIsDecoy(), data.getUnimodMap());
                    group.addData(data);
                    groupMapDecoy.put(data.getPeptideRef(), group);
                } else {
                    TransitionGroup group = groupMapDecoy.get(data.getPeptideRef());
                    group.addData(data);
                }
            }else{
                if (groupMap.get(data.getPeptideRef()) == null) {
                    TransitionGroup group = new TransitionGroup(data.getProteinName(), data.getPeptideRef(), Double.parseDouble(data.getRt().toString()), data.getIsDecoy(), data.getUnimodMap());
                    group.addData(data);
                    groupMap.put(data.getPeptideRef(), group);
                } else {
                    TransitionGroup group = groupMap.get(data.getPeptideRef());
                    group.addData(data);
                }
            }

        }
        List<TransitionGroup> groups = new ArrayList<>(groupMap.values());
        groups.addAll(groupMapDecoy.values());
        return groups;
    }

    @Override
    public List<TransitionGroup> getTransitionGroup(AnalyseOverviewDO overviewDO) {
        int pageSize = 1000;
        List<Peptide> peptides = transitionDAO.getPeptideList(overviewDO.getLibraryId());
        HashMap<String, TransitionGroup> groupMap = new HashMap<>();
        HashMap<String, TransitionGroup> groupMapDecoy = new HashMap<>();
        for (Peptide peptide : peptides) {
            if(peptide.getIsDecoy()){
                groupMapDecoy.put(peptide.getPeptideRef(), new TransitionGroup(peptide.getProteinName(), peptide.getPeptideRef(), peptide.getRt(), peptide.getIsDecoy(), peptide.getUnimodMap()));
            }else{
                groupMap.put(peptide.getPeptideRef(), new TransitionGroup(peptide.getProteinName(), peptide.getPeptideRef(), peptide.getRt(), peptide.getIsDecoy(), peptide.getUnimodMap()));
            }
        }

        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setOverviewId(overviewDO.getId());
        query.setMsLevel(2);
        query.setPageSize(pageSize);
        long totalCount = count(query);
        long totalPage = totalCount % pageSize == 0 ? totalCount / pageSize : (totalCount / pageSize + 1);
        for (int i = 1; i <= totalPage; i++) {
            long start1 = System.currentTimeMillis();
            query.setPageNo(i);
            List<AnalyseDataDO> dataList = analyseDataDAO.getList(query);
            logger.info("获取1000个卷积结果耗时:" + (System.currentTimeMillis() - start1));
            for (AnalyseDataDO data : dataList) {
                TransitionGroup group = null;
                if(data.getIsDecoy()){
                    group = groupMapDecoy.get(data.getPeptideRef());
                }else{
                    group = groupMap.get(data.getPeptideRef());
                }

                if (group != null) {
                    group.addData(data);
                } else {
                    logger.error("PeptideRef " + data.getPeptideRef() + " not found in Library");
                }
            }
            logger.info("第" + i + "批数据处理完毕,一共有" + totalPage + "批数据");
        }
        List<TransitionGroup> groups = new ArrayList<>(groupMap.values());
        groups.addAll(groupMapDecoy.values());
        return groups;
    }

    @Override
    public List<TransitionGroup> getIrtTransitionGroup(String overviewId, String iRtlibraryId) {

        List<Peptide> peptides = transitionDAO.getPeptideList(iRtlibraryId);
        List<TransitionGroup> groups = new ArrayList<>();

        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setOverviewId(overviewId);
        for (Peptide peptide : peptides) {
            //获取改组的目标卷积对象列表
            List<String> cutInfos = transitionDAO.getTransitionCutInfos(iRtlibraryId, peptide.getPeptideRef());

            //根据标准库ID和PeptideRef获取实际卷积所得的对象列表
            query.setPeptideRef(peptide.getPeptideRef());
            query.setMsLevel(2);
            List<AnalyseDataDO> dataList = analyseDataDAO.getAll(query);
            //开始比对结果,组成最终的HashMap
            HashMap<String, AnalyseDataDO> dataMap = new HashMap<>();
            for (String cutInfo : cutInfos) {
                dataMap.put(cutInfo, null);
            }
            for (AnalyseDataDO dataDO : dataList) {
                //以库里面的cutInfos为准,这里做这个contains判断是因为有时候标准库和iRT校准库中同一个Peptide对应的Transition不同,当出现不同的时候,
                //卷积数据中的数据和Library可能会不一一匹配,因此需要舍弃掉卷积结果中的无关数据,所以有这层判断
                if (dataMap.containsKey(dataDO.getCutInfo())) {
                    dataMap.put(dataDO.getCutInfo(), dataDO);
                }
            }

            TransitionGroup group = new TransitionGroup();
            group.setPeptideRef(peptide.getPeptideRef());
            group.setProteinName(peptide.getProteinName());
            group.setRt(peptide.getRt());
            group.setDataMap(dataMap);
            group.setUnimodMap(peptide.getUnimodMap());
            groups.add(group);
        }

        return groups;
    }

    @Override
    public List<TransitionGroup> getIrtTransitionGroup(List<AnalyseDataDO> dataList, String iRtlibraryId) {
        List<Peptide> peptides = transitionDAO.getPeptideList(iRtlibraryId);

        HashMap<String, TransitionGroup> groupMap = new HashMap<>();
        //由于校准库中没有伪肽段,所以不需要像getTransitionGroup接口一样对伪肽段做单独的处理
        for (Peptide peptide : peptides) {
            groupMap.put(peptide.getPeptideRef(), new TransitionGroup(peptide.getProteinName(), peptide.getPeptideRef(), peptide.getRt(), peptide.getIsDecoy(), peptide.getUnimodMap()));
        }

        for (AnalyseDataDO data : dataList) {
            TransitionGroup group = groupMap.get(data.getPeptideRef());
            if (group != null) {
                group.addData(data);
            } else {
                logger.error("PeptideRef " + data.getPeptideRef() + " not found in Library");
            }
        }

        return new ArrayList<>(groupMap.values());
    }
}
