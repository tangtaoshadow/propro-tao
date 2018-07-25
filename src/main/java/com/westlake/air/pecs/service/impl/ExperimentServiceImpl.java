package com.westlake.air.pecs.service.impl;

import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.dao.ExperimentDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.LibraryCoordinate;
import com.westlake.air.pecs.domain.bean.MzIntensityPairs;
import com.westlake.air.pecs.domain.bean.SimpleScanIndex;
import com.westlake.air.pecs.domain.bean.TargetTransition;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.query.ExperimentQuery;
import com.westlake.air.pecs.domain.query.ScanIndexQuery;
import com.westlake.air.pecs.parser.MzXmlParser;
import com.westlake.air.pecs.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 09:45
 */
@Service("experimentService")
public class ExperimentServiceImpl implements ExperimentService {

    public final Logger logger = LoggerFactory.getLogger(ExperimentServiceImpl.class);

    public static int SPECTRUM_READ_PER_TIME = 100;

    @Autowired
    ExperimentDAO experimentDAO;

    @Autowired
    TransitionService transitionService;

    @Autowired
    ScanIndexService scanIndexService;

    @Autowired
    MzXmlParser mzXmlParser;

    @Autowired
    AnalyseDataService analyseDataService;

    @Autowired
    AnalyseOverviewService analyseOverviewService;

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
    public ResultDO insert(ExperimentDO experimentDO) {
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

    /**
     * @param expId
     * @param rtExtractWindow
     * @param mzExtractWindow
     * @param buildType       0:解压缩MS1和MS2; 1:解压缩MS1; 2:解压缩MS2
     * @return
     * @throws IOException
     */
    @Override
    public ResultDO extract(String expId, float rtExtractWindow, float mzExtractWindow, int buildType) {

        ResultDO resultDO = new ResultDO(true);
        //基本条件检查
        logger.info("基本条件检查开始");
        ExperimentDO experimentDO = experimentDAO.getById(expId);
        ResultDO checkResult = checkExperiment(experimentDO);
        if (checkResult.isFailured()) {
            return checkResult;
        }

        File file = (File) checkResult.getModel();
        RandomAccessFile raf = null;

        //创建实验初始化概览数据
        AnalyseOverviewDO overviewDO = new AnalyseOverviewDO();
        overviewDO.setExpId(expId);
        overviewDO.setLibraryId(experimentDO.getLibraryId());
        analyseOverviewService.insert(overviewDO);

        try {
            raf = new RandomAccessFile(file, "r");

            //构建卷积坐标(耗时操作)
            if (buildType == 0) {
                LibraryCoordinate lc = transitionService.buildMS(experimentDO.getLibraryId(), rtExtractWindow);
                extractMS1(raf, expId, overviewDO.getId(), lc.getMs1List(), rtExtractWindow, mzExtractWindow);
                extractMS2(raf, expId, overviewDO.getId(), lc.getMs2List(), rtExtractWindow, mzExtractWindow);
            } else if (buildType == 1) {
                List<TargetTransition> targetTransitions = transitionService.buildMS1(experimentDO.getLibraryId(), rtExtractWindow);
                extractMS1(raf, expId, overviewDO.getId(), targetTransitions, rtExtractWindow, mzExtractWindow);
            } else if (buildType == 2) {
                List<TargetTransition> targetTransitions = transitionService.buildMS2(experimentDO.getLibraryId(), rtExtractWindow);
                extractMS2(raf, expId, overviewDO.getId(), targetTransitions, rtExtractWindow, mzExtractWindow);
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        return resultDO;
    }

    /**
     * 解压缩MS1数据的话会把所有的索引全部读入到内存中进行卷积
     * PECS在进行MS1谱图解析时的几个特点:
     * 1.全光谱读取
     * 2.由于是全光谱解析,因此解析过后的RTArray都是一样的,因此只存储一份从而减少内存的开销
     * 3.
     * @param raf
     * @param expId
     * @param coordinates
     * @param rtExtractWindow
     * @param mzExtractWindow
     * @return
     * @throws IOException
     */
    @Override
    public void extractMS1(RandomAccessFile raf, String expId, String overviewId, List<TargetTransition> coordinates, float rtExtractWindow, float mzExtractWindow) throws IOException {

        //用于存储最终的卷积结果
        List<AnalyseDataDO> dataList = new ArrayList<>();
        //用于存储从XML中解压缩出来的数据
        TreeMap<Float, MzIntensityPairs> rtMap = new TreeMap<>();

        ScanIndexQuery query = new ScanIndexQuery();
        query.setExperimentId(expId);
        query.setMsLevel(1);

        long totalCount = scanIndexService.count(query);

        logger.info("MS1 坐标总计" + coordinates.size() + "条");
        logger.info("MS1 实验光谱数" + totalCount + "条");

        //如果MS1的实验数据不存在,则跳过
        if (totalCount == 0) {
            return;
        }
        if (coordinates.size() == 0) {
            logger.info("坐标系为空");
            return;
        }

        List<SimpleScanIndex> indexes = scanIndexService.getSimpleAll(query);

        //如果MS1存在,则进行MS1的光谱扫描
        float mzStart = 0;
        float mzEnd = -1;

        long start = System.currentTimeMillis();
        long start2 = System.currentTimeMillis();
        logger.info("开始创建MS1图谱");
        int k = 0;
        for (SimpleScanIndex index : indexes) {
            rtMap.put(index.getRt(), mzXmlParser.parseOne(raf, index));
            k++;
            if (k % 100 == 0) {
                logger.info("解析100个光谱耗时:" + (System.currentTimeMillis() - start2));
                start2 = System.currentTimeMillis();
            }
        }
        logger.info("解析XML文件总计耗时:" + (System.currentTimeMillis() - start));
        int logCountForMS1Target = 0;
        start = System.currentTimeMillis();
        for (TargetTransition ms1 : coordinates) {

            //设置mz卷积窗口
            mzStart = ms1.getPrecursorMz() - mzExtractWindow / 2;
            mzEnd = ms1.getPrecursorMz() + mzExtractWindow / 2;

            Float[] rtArray = new Float[rtMap.size()];
            rtMap.keySet().toArray(rtArray);
            Float[] intensityArray = new Float[rtMap.size()];

            int i = 0;
            for (Float rt : rtMap.keySet()) {
                MzIntensityPairs pairs = rtMap.get(rt);
                Float[] pairMzArray = pairs.getMzArray();
                Float[] pairIntensityArray = pairs.getIntensityArray();
                intensityArray[i] = evolution(pairMzArray, pairIntensityArray, mzStart, mzEnd);
                i++;
            }

            AnalyseDataDO dataDO = new AnalyseDataDO();
            dataDO.setMz(ms1.getPrecursorMz());
            dataDO.setRtArray(rtArray);
            dataDO.setIntensityArray(intensityArray);
            dataDO.setMsLevel(1);
            dataDO.setOverviewId(overviewId);
            dataList.add(dataDO);

            //每隔1000条数据落库一次,以减少对内存的依赖
            logCountForMS1Target++;
            if (logCountForMS1Target % 10000 == 0) {
                logger.info("已扫描MS1目标:" + logCountForMS1Target + "条,累计耗时:" + (System.currentTimeMillis() - start));
//                analyseDataService.insertAll(dataList, false);
                logger.info("数据存入数据库成功");
                dataList.clear();
            }
        }
//        analyseDataService.insertAll(dataList, false);
        logger.info("全部数据处理完毕");
    }

    @Override
    public void extractMS2(RandomAccessFile raf, String expId, String overviewId, List<TargetTransition> coordinates, float rtExtractWindow, float mzExtractWindow) {

//        query.setMsLevel(2);
//        Long ms2Count = scanIndexService.count(query);
//        logger.info("MS1 Target总计数目" + lc.getMs2List().size() + "条");
//
//        int logCountForMS2Target = 0;
//
//        //如果MS2存在,则进行MS2的光谱扫描
//        for (TargetTransition ms2 : lc.getMs2List()) {
//            logCountForMS2Target++;
//            if (logCountForMS2Target % 100 == 0) {
//                logger.info("已扫描MS2目标:" + logCountForMS2Target + "条,累计耗时:" + (System.currentTimeMillis() - start));
//            }
//
//            if (rtExtractWindow > 0) {
//                query.setRtStart(ms2.getRtStart());
//                query.setRtEnd(ms2.getRtEnd());
//            }
//            List<ScanIndexDO> indexes = scanIndexService.getAll(query);
//
//            if (indexes == null || indexes.size() == 0) {
//                logger.info("MS2未扫描到相关数据:" + "|" + ms2.getId());
//                continue;
//            }
//            logger.info("MS2扫描到相关数据:" + indexes.size() + "条");
//            //设置mz卷积窗口
//            mzStart = ms2.getProductMz() - mzExtractWindow / 2.0;
//            mzEnd = ms2.getProductMz() + mzExtractWindow / 2.0;
//
//            TreeMap<Double, Double> parseMap = null;
//            TreeMap<Double, Double> mzResultMap = new TreeMap<>();
//            long startParse = System.currentTimeMillis();
//            for (ScanIndexDO index : indexes) {
//                parseMap = mzXmlParser.parseOne(raf, index);
//                double intensity = 0;
//                for (Double key : parseMap.keySet()) {
//                    if (key >= mzStart && key <= mzEnd) {
//                        intensity += parseMap.get(key);
//                    }
//                }
//                mzResultMap.put(index.getRt(), intensity);
//            }
//            logger.info("Parse" + indexes.size() + "个耗时:" + (System.currentTimeMillis() - startParse) + "毫秒");
//
//            ms2Map.put(ms2.getProductMz(), mzResultMap);
//        }
    }

    private ResultDO<File> checkExperiment(ExperimentDO experimentDO) {
        if (experimentDO == null) {
            return ResultDO.buildError(ResultCode.EXPERIMENT_NOT_EXISTED);
        }
        if (experimentDO.getFileLocation() == null || experimentDO.getFileLocation().isEmpty()) {
            return ResultDO.buildError(ResultCode.FILE_NOT_SET);
        }
        if (experimentDO.getLibraryId() == null || experimentDO.getLibraryId().isEmpty()) {
            return ResultDO.buildError(ResultCode.LIBRARY_NAME_CANNOT_BE_EMPTY);
        }
        File file = new File(experimentDO.getFileLocation());
        if (!file.exists()) {
            return ResultDO.buildError(ResultCode.FILE_NOT_EXISTED);
        }

        ResultDO<File> resultDO = new ResultDO(true);
        resultDO.setModel(file);
        return resultDO;
    }

    /**
     * 计算 mz在[mzStart, mzEnd]范围对应的intensity和
     *
     * @param mzArray        从小到到已经排好序
     * @param intensityArray
     * @param mzStart
     * @param mzEnd
     * @return
     */
    public static Float evolution(Float[] mzArray, Float[] intensityArray, Float mzStart, Float mzEnd) {
        Float result = 0f;
        int start = findIndex(mzArray, mzStart);
        int end = findIndex(mzArray, mzEnd) - 1;
        for (int index = start; index <= end; index++) {
            result += intensityArray[index];
        }
        return result;
    }

    // 找到从小到大排序的第一个大于目标值的索引
    public static int findIndex(Float[] array, Float target) {
        int pStart = 0, pEnd = array.length - 1;
        while (pStart <= pEnd) {
            int tmp = (pStart + pEnd) / 2;
            if (target < array[tmp]) {
                pEnd = tmp - 1;
            } else if (target > array[tmp]) {
                pStart = tmp + 1;
            } else {
                return tmp;
            }
        }
        return pStart;
    }
}
