package com.westlake.air.propro.service.impl;

import com.google.common.collect.Ordering;
import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.dao.LibraryDAO;
import com.westlake.air.propro.dao.PeptideDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.aird.WindowRange;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.db.simple.Protein;
import com.westlake.air.propro.domain.db.simple.TargetPeptide;
import com.westlake.air.propro.domain.query.PeptideQuery;
import com.westlake.air.propro.service.ExperimentService;
import com.westlake.air.propro.service.TaskService;
import com.westlake.air.propro.service.PeptideService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 20:02
 */
@Service("peptideService")
public class PeptideServiceImpl implements PeptideService {

    public final Logger logger = LoggerFactory.getLogger(PeptideServiceImpl.class);

    @Autowired
    PeptideDAO peptideDAO;
    @Autowired
    LibraryDAO libraryDAO;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    TaskService taskService;

    @Override
    public List<PeptideDO> getAllByLibraryId(String libraryId) {
        return peptideDAO.getAllByLibraryId(libraryId);
    }

    @Override
    public PeptideDO getByLibraryIdAndPeptideRefAndIsDecoy(String libraryId, String peptideRef, boolean isDecoy) {
        return peptideDAO.getByLibraryIdAndPeptideRefAndIsDecoy(libraryId, peptideRef, isDecoy);
    }

    @Override
    public List<PeptideDO> getAllByLibraryIdAndIsDecoy(String libraryId, boolean isDecoy) {
        return peptideDAO.getAllByLibraryIdAndIsDecoy(libraryId, isDecoy);
    }
    @Override
    public List<PeptideDO> getAllByLibraryIdAndProteinNameAndIsDecoy(String libraryId, String proteinName, boolean isDecoy) {
        return peptideDAO.getAllByLibraryIdAndProteinNameAndIsDecoy(libraryId, proteinName, isDecoy);
    }

    @Override
    public Long count(PeptideQuery query) {
        return peptideDAO.count(query);
    }

    @Override
    public ResultDO<List<PeptideDO>> getList(PeptideQuery query) {

        List<PeptideDO> peptideDOList = peptideDAO.getList(query);
        long totalCount = peptideDAO.count(query);
        ResultDO<List<PeptideDO>> resultDO = new ResultDO<>(true);
        resultDO.setModel(peptideDOList);
        resultDO.setTotalNum(totalCount);
        resultDO.setPageSize(query.getPageSize());
        return resultDO;
    }

    @Override
    public List<PeptideDO> getAll(PeptideQuery query) {
        List<PeptideDO> peptides = peptideDAO.getAll(query);
        return peptides;
    }

    @Override
    public ResultDO insert(PeptideDO peptideDO) {
        try {
            peptideDAO.insert(peptideDO);
            return new ResultDO(true);
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.INSERT_ERROR);
        }
    }

    @Override
    public ResultDO update(PeptideDO peptideDO) {
        try {
            peptideDAO.update(peptideDO);
            return ResultDO.build(peptideDO);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.UPDATE_ERROR);
        }
    }

    /**
     * 这边的代码由于时间问题写的比较简陋,先删除原有的关联数据,再插入新的关联数据,未做事务处理
     *
     * @param peptides
     * @param isDeleteOld
     * @return
     */
    @Override
    public ResultDO insertAll(List<PeptideDO> peptides, boolean isDeleteOld) {
        if (peptides == null || peptides.size() == 0) {
            return ResultDO.buildError(ResultCode.OBJECT_CANNOT_BE_NULL);
        }
        try {
            if (isDeleteOld) {
                peptideDAO.deleteAllByLibraryId(peptides.get(0).getLibraryId());
            }
            peptideDAO.insert(peptides);
            return new ResultDO(true);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return ResultDO.buildError(ResultCode.INSERT_ERROR);
        }
    }

    @Override
    public ResultDO deleteAllByLibraryId(String libraryId) {
        try {
            peptideDAO.deleteAllByLibraryId(libraryId);
            return new ResultDO(true);
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.DELETE_ERROR);
        }
    }

    @Override
    public ResultDO deleteAllDecoyByLibraryId(String libraryId) {
        try {
            peptideDAO.deleteAllDecoyByLibraryId(libraryId);
            return new ResultDO(true);
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.DELETE_ERROR);
        }
    }

    @Override
    public ResultDO<PeptideDO> getById(String id) {
        try {
            PeptideDO peptideDO = peptideDAO.getById(id);
            if (peptideDO == null) {
                return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
            } else {
                ResultDO<PeptideDO> resultDO = new ResultDO<>(true);
                resultDO.setModel(peptideDO);
                return resultDO;
            }
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.QUERY_ERROR);
        }
    }

    @Override
    public Double[] getRTRange(String libraryId) {
        Double[] range = new Double[2];
        PeptideQuery query = new PeptideQuery(libraryId);
        query.setPageSize(1);
        query.setOrderBy(Sort.Direction.ASC);
        query.setSortColumn("rt");
        List<PeptideDO> descList = peptideDAO.getList(query);
        if (descList != null && descList.size() == 1) {
            range[0] = descList.get(0).getRt();
        }
        query.setOrderBy(Sort.Direction.DESC);
        List<PeptideDO> ascList = peptideDAO.getList(query);
        if (ascList != null && ascList.size() == 1) {
            range[1] = ascList.get(0).getRt();
        }
        return range;
    }

    @Override
    public ResultDO<List<Protein>> getProteinList(PeptideQuery query) {
        LibraryDO libraryDO = libraryDAO.getById(query.getLibraryId());
        List<Protein> proteins = peptideDAO.getProteinList(query);
        ResultDO<List<Protein>> resultDO = new ResultDO<>(true);
        resultDO.setModel(proteins);
        resultDO.setTotalNum(libraryDO.getProteinCount());
        resultDO.setPageSize(query.getPageSize());
        return resultDO;
    }

    @Override
    public Long countByProteinName(String libraryId) {
        return peptideDAO.countByProteinName(libraryId);
    }

    @Override
    public Long countByUniqueProteinName(String libraryId) {
        return peptideDAO.countByUniqueProteinName(libraryId);
    }

    @Override
    public Long countByPeptideRef(String libraryId) {
        return peptideDAO.countByPeptideRef(libraryId);
    }

    @Override
    public List<TargetPeptide> buildMS1Coordinates(String libraryId, SlopeIntercept slopeIntercept, float rtExtractionWindows) {
        PeptideQuery query = new PeptideQuery(libraryId);
        List<TargetPeptide> targetList = peptideDAO.getTPAll(query);

        for (TargetPeptide targetPeptide : targetList) {
            targetPeptide.setRtStart((targetPeptide.getRt() - slopeIntercept.getIntercept().floatValue()) / slopeIntercept.getSlope().floatValue() - rtExtractionWindows / 2.0f);
            targetPeptide.setRtEnd((targetPeptide.getRt() - slopeIntercept.getIntercept().floatValue()) / slopeIntercept.getSlope().floatValue() + rtExtractionWindows / 2.0f);
        }
        List<TargetPeptide> list = sortMS1Coordinates(targetList);
        return list;
    }

    @Override
    public List<TargetPeptide> buildMS2Coordinates(LibraryDO library, SlopeIntercept slopeIntercept, float rtExtractionWindows, WindowRange mzRange, Float[] rtRange, String type, boolean uniqueCheck) {

        long start = System.currentTimeMillis();
        PeptideQuery query = new PeptideQuery(library.getId());
        float precursorMz = mzRange.getMz();
        if(type.equals(Constants.EXP_TYPE_PRM)){
            //TODO: PRM
            query.setMzStart(precursorMz - 0.0006d);
            query.setMzEnd(precursorMz + 0.0006d);
        }else {
            query.setMzStart((double) mzRange.getStart());
            query.setMzEnd((double) mzRange.getEnd());
        }
        if(uniqueCheck){
            query.setIsUnique(true);
        }
        if(library.getType().equals(LibraryDO.TYPE_IRT)){
            query.setIsDecoy(false);
        }

        List<TargetPeptide> targetList = peptideDAO.getTPAll(query);
        if (!targetList.isEmpty() && targetList.size() != 2 && type.equals(Constants.EXP_TYPE_PRM)){
            //PRM模式下, rtRange不为空;
            TargetPeptide bestTarget = null, bestDecoy = null;
            float mzDistance = Float.MAX_VALUE;
            for (TargetPeptide peptide: targetList){
                if (rtExtractionWindows != -1){
                    float iRt = (peptide.getRt() - slopeIntercept.getIntercept().floatValue()) / slopeIntercept.getSlope().floatValue();
                    if (iRt < rtRange[0] - 30 || iRt > rtRange[1] + 30){
                        continue;
                    }
                }
                float tempMzDistance = Math.abs(peptide.getMz() - precursorMz);
                if (tempMzDistance <= mzDistance){
                    if (peptide.getIsDecoy()){
                        bestDecoy = peptide;
                    }else {
                        bestTarget = peptide;
                    }
                    mzDistance = tempMzDistance;
                }
            }
            targetList.clear();
            if (bestTarget != null){
                targetList.add(bestTarget);
            }
            if (bestDecoy != null){
                targetList.add(bestDecoy);
            }
            if (mzDistance >= 0.0002f && bestTarget!=null){
                System.out.println("Coordinate: " + bestTarget.getPeptideRef() + " " + mzDistance);
            }
        }
        long readDB = System.currentTimeMillis() - start;
        if (rtExtractionWindows != -1) {
            for (TargetPeptide targetPeptide : targetList) {
                float iRt = (targetPeptide.getRt() - slopeIntercept.getIntercept().floatValue()) / slopeIntercept.getSlope().floatValue();
                targetPeptide.setRtStart(iRt - rtExtractionWindows / 2.0f);
                targetPeptide.setRtEnd(iRt + rtExtractionWindows / 2.0f);
            }
        } else {
            for (TargetPeptide targetPeptide : targetList) {
                targetPeptide.setRtStart(-1);
                targetPeptide.setRtEnd(99999);
            }
        }

        logger.info("构建卷积MS2坐标,读取标准库耗时:" + readDB);
        return targetList;
    }

    @Override
    public HashMap<String, TargetPeptide> getTPMap(PeptideQuery query) {
        List<TargetPeptide> tps = peptideDAO.getTPAll(query);
        HashMap<String, TargetPeptide> hashMap = new HashMap<>();
        for (TargetPeptide peptide : tps) {
            hashMap.put(peptide.getPeptideRef() + "_" + peptide.getIsDecoy(), peptide);
        }

        return hashMap;
    }

    private List<TargetPeptide> sortMS1Coordinates(List<TargetPeptide> targetList) {
        //存储set中从而过滤出MS1
        HashSet<TargetPeptide> targetSet = new HashSet<>(targetList);
        Ordering<TargetPeptide> ordering = Ordering.from(new Comparator<TargetPeptide>() {
            @Override
            public int compare(TargetPeptide o1, TargetPeptide o2) {
                if (o1.getMz() > o2.getMz()) {
                    return 1;
                } else if (o1.getMz() == o2.getMz()) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });

        return ordering.sortedCopy(targetSet);
    }

}
