package com.westlake.air.swathplatform.service.impl;

import com.westlake.air.swathplatform.constants.ResultCode;
import com.westlake.air.swathplatform.dao.ExperimentDAO;
import com.westlake.air.swathplatform.dao.LibraryDAO;
import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.bean.ExperimentResult;
import com.westlake.air.swathplatform.domain.bean.LibraryCoordinate;
import com.westlake.air.swathplatform.domain.bean.TargetTransition;
import com.westlake.air.swathplatform.domain.db.ExperimentDO;
import com.westlake.air.swathplatform.domain.db.LibraryDO;
import com.westlake.air.swathplatform.domain.db.ScanIndexDO;
import com.westlake.air.swathplatform.domain.query.ExperimentQuery;
import com.westlake.air.swathplatform.domain.query.LibraryQuery;
import com.westlake.air.swathplatform.domain.query.ScanIndexQuery;
import com.westlake.air.swathplatform.parser.MzXmlParser;
import com.westlake.air.swathplatform.service.ExperimentService;
import com.westlake.air.swathplatform.service.LibraryService;
import com.westlake.air.swathplatform.service.ScanIndexService;
import com.westlake.air.swathplatform.service.TransitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.pride.tools.mzxml_parser.MzXMLParsingException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

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

    @Autowired
    MzXmlParser mzXmlParser;

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
    public ResultDO<ExperimentResult> extract(String expId, double rtExtractWindow, double mzExtractWindow, int buildType) {

        ResultDO<ExperimentResult> resultDO = new ResultDO<ExperimentResult>();
        //基本条件检查
        logger.info("基本条件检查开始");
        ExperimentDO experimentDO = experimentDAO.getById(expId);
        ResultDO checkResult = checkExperiment(experimentDO);
        if (checkResult.isFailured()) {
            return checkResult;
        }

        File file = (File) checkResult.getModel();
        RandomAccessFile raf = null;

        //创建结果对象
        logger.info("创建结果对象");
        ExperimentResult experimentResult = new ExperimentResult();
        HashMap<Double, TreeMap<Double, Double>> ms1Map = null;
        HashMap<Double, TreeMap<Double, Double>> ms2Map = null;
        experimentResult.setLibraryId(experimentDO.getLibraryId());
        experimentResult.setExperimentId(expId);
        experimentResult.setRtExtractWindow(rtExtractWindow);

        try {
            raf = new RandomAccessFile(file, "r");

            //构建卷积坐标(耗时操作)
            logger.info("构建卷积坐标(耗时操作)");
            long start = System.currentTimeMillis();
            LibraryCoordinate lc = transitionService.buildMS(experimentDO.getLibraryId(), rtExtractWindow);
            logger.info("构建卷积坐标耗时:" + (System.currentTimeMillis() - start));

            if (buildType == 0) {
                ms1Map = extractMS1(raf, expId, lc.getMs1List(), rtExtractWindow, mzExtractWindow);
                ms2Map = extractMS2(raf, expId, lc.getMs2List(), rtExtractWindow, mzExtractWindow);
            } else if (buildType == 1) {
                ms1Map = extractMS1(raf, expId, lc.getMs1List(), rtExtractWindow, mzExtractWindow);
            } else if (buildType == 2) {
                ms2Map = extractMS2(raf, expId, lc.getMs2List(), rtExtractWindow, mzExtractWindow);
            }

            experimentResult.setMs1Map(ms1Map);
            experimentResult.setMs2Map(ms2Map);
        } catch (Exception e) {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        resultDO.setModel(experimentResult);
        return resultDO;
    }

    /**
     * 解压缩MS1数据的话会把所有的索引全部读入到内存中进行卷积
     *
     * @param raf
     * @param expId
     * @param coordinates
     * @param rtExtractWindow
     * @param mzExtractWindow
     * @return
     * @throws IOException
     */
    @Override
    public HashMap<Double, TreeMap<Double, Double>> extractMS1(RandomAccessFile raf, String expId, List<TargetTransition> coordinates, double rtExtractWindow, double mzExtractWindow) throws IOException {

        //用于存储最终的卷积结果
        HashMap<Double, TreeMap<Double, Double>> ms1Map = new HashMap<>();
        //用于存储从XML中解压缩出来的数据
        TreeMap<Double, TreeMap<Double, Double>> rtMap = new TreeMap<>();

        ScanIndexQuery query = new ScanIndexQuery();
        query.setExperimentId(expId);
        query.setMsLevel(1);

        List<ScanIndexDO> indexes = scanIndexService.getAll(query);
        logger.info("MS1 坐标总计" + coordinates.size() + "条");
        logger.info("MS1 实验光谱数" + indexes.size() + "条");

        //如果MS1的实验数据不存在,则跳过
        if (indexes.size() == 0) {
            return ms1Map;
        }

        //如果MS1存在,则进行MS1的光谱扫描
        double mzStart = 0;
        double mzEnd = -1;

        long start = System.currentTimeMillis();
        logger.info("开始创建MS1图谱");
        for (ScanIndexDO index : indexes) {
            rtMap.put(index.getRt(), mzXmlParser.parseOne(raf, index));
        }

        logger.info("解析XML文件总计耗时:" + (System.currentTimeMillis() - start));
        int logCountForMS1Target = 0;
        for (TargetTransition ms1 : coordinates) {

            //设置mz卷积窗口
            mzStart = ms1.getPrecursorMz() - mzExtractWindow / 2.0;
            mzEnd = ms1.getPrecursorMz() + mzExtractWindow / 2.0;

            TreeMap<Double, Double> mzResultMap = new TreeMap<>();
            for (Double rt : rtMap.keySet()) {
                Double intensity = 0d;
                TreeMap<Double, Double> kvMap = rtMap.get(rt);
                for (Double key : kvMap.keySet()) {
                    if (key >= mzStart && key <= mzEnd) {
                        intensity += kvMap.get(key);
                    }
                }
                mzResultMap.put(rt, intensity);
            }

            //每隔1000条数据落库一次,以减少对内存的依赖
            logCountForMS1Target++;
            if (logCountForMS1Target % 10000 == 0) {
                logger.info("已扫描MS1目标:" + logCountForMS1Target + "条,累计耗时:" + (System.currentTimeMillis() - start));
            }

            ms1Map.put(ms1.getPrecursorMz(), mzResultMap);
        }

        return ms1Map;
    }

    @Override
    public HashMap<Double, TreeMap<Double, Double>> extractMS2(RandomAccessFile raf, String expId, List<TargetTransition> coordinates, double rtExtractWindow, double mzExtractWindow) {

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

        return null;
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
}
