package com.westlake.air.pecs.service.impl;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.dao.AnalyseDataDAO;
import com.westlake.air.pecs.dao.AnalyseOverviewDAO;
import com.westlake.air.pecs.dao.ExperimentDAO;
import com.westlake.air.pecs.dao.ScanIndexDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.SwathInput;
import com.westlake.air.pecs.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.*;
import com.westlake.air.pecs.domain.db.simple.SimpleScanIndex;
import com.westlake.air.pecs.domain.db.simple.TargetTransition;
import com.westlake.air.pecs.domain.query.ExperimentQuery;
import com.westlake.air.pecs.domain.query.ScanIndexQuery;
import com.westlake.air.pecs.parser.BaseExpParser;
import com.westlake.air.pecs.parser.MzMLParser;
import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.service.*;
import com.westlake.air.pecs.utils.ConvolutionUtil;
import com.westlake.air.pecs.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

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
    ScanIndexDAO scanIndexDAO;
    @Autowired
    MzXMLParser mzXMLParser;
    @Autowired
    MzMLParser mzMLParser;
    @Autowired
    AnalyseDataDAO analyseDataDAO;
    @Autowired
    AnalyseOverviewDAO analyseOverviewDAO;
    @Autowired
    TaskService taskService;
    @Autowired
    ScanIndexService scanIndexService;
    @Autowired
    LibraryService libraryService;
    @Autowired
    ScoresService scoresService;

    @Override
    public List<ExperimentDO> getAll() {
        return experimentDAO.getAll();
    }

    @Override
    public List<ExperimentDO> getSimpleAll() {
        return experimentDAO.getSimpleAll();
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
            return ResultDO.buildError(ResultCode.INSERT_ERROR);
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
            return ResultDO.buildError(ResultCode.INSERT_ERROR);
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
            return ResultDO.buildError(ResultCode.DELETE_ERROR);
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
            return ResultDO.buildError(ResultCode.QUERY_ERROR);
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
            return ResultDO.buildError(ResultCode.QUERY_ERROR);
        }
    }

    @Override
    public List<WindowRang> getWindows(String expId) {
        ScanIndexQuery query = new ScanIndexQuery();
        query.setPageSize(1);
        query.setMsLevel(1);
        query.setExperimentId(expId);
        List<ScanIndexDO> indexes = scanIndexDAO.getList(query);
        if (indexes == null || indexes.size() == 0) {
            return null;
        }
        //得到任意的一个MS1
        ScanIndexDO ms1Index = indexes.get(0);
        //根据这个MS1获取他下面所有的MS2
        query.setMsLevel(2);
        query.setParentNum(ms1Index.getNum());
        List<ScanIndexDO> ms2Indexes = scanIndexDAO.getAll(query);
        if (ms2Indexes == null || ms2Indexes.size() <= 1) {
            return null;
        }

        List<WindowRang> windowRangs = new ArrayList<>();
        float ms2Interval = ms2Indexes.get(1).getRt() - ms2Indexes.get(0).getRt();
        for (int i = 0; i < ms2Indexes.size(); i++) {
            WindowRang rang = new WindowRang();
            rang.setMzStart(ms2Indexes.get(i).getPrecursorMzStart());
            rang.setMzEnd(ms2Indexes.get(i).getPrecursorMzEnd());
            rang.setMs2Interval(ms2Interval);
            windowRangs.add(rang);
        }
        return windowRangs;
    }

    @Override
    public void uploadFile(ExperimentDO experimentDO, File file, TaskDO taskDO) {
        try {
            List<ScanIndexDO> indexList = null;
            //传入不同的文件类型会调用不同的解析层
            if (experimentDO.getFileType().equals(Constants.EXP_SUFFIX_MZXML)) {
                indexList = mzXMLParser.index(file, experimentDO.getId(), experimentDO.getOverlap(), taskDO);
            } else if (experimentDO.getFileType().equals(Constants.EXP_SUFFIX_MZML)) {
                indexList = mzMLParser.index(file, experimentDO.getId(), experimentDO.getOverlap(), taskDO);
            }

            taskDO.addLog("索引构建完毕,开始存储索引");
            taskService.update(taskDO);

            ResultDO resultDO = scanIndexService.insertAll(indexList, true);
            if (resultDO.isFailed()) {
                taskDO.addLog("索引存储失败" + resultDO.getMsgInfo());
                taskDO.finish(TaskDO.STATUS_FAILED);
                taskService.update(taskDO);
                delete(experimentDO.getId());
                scanIndexService.deleteAllByExperimentId(experimentDO.getId());
            } else {
                taskDO.addLog("索引存储成功");
                taskDO.finish(TaskDO.STATUS_SUCCESS);
                taskService.update(taskDO);
            }

        } catch (Exception e) {
            taskDO.addLog("索引存储失败:" + e.getMessage());
            taskDO.finish(TaskDO.STATUS_FAILED);
            taskService.update(taskDO);
            e.printStackTrace();
        }
    }

    @Override
    public ResultDO extract(SwathInput swathInput) {
        ResultDO resultDO = new ResultDO(true);
        logger.info("基本条件检查开始");
        ResultDO checkResult = ConvolutionUtil.checkExperiment(swathInput.getExperimentDO());
        if (checkResult.isFailed()) {
            logger.info("条件检查失败:" + checkResult.getMsgInfo());
            return checkResult;
        }

        File file = (File) checkResult.getModel();
        RandomAccessFile raf = null;

        ResultDO<LibraryDO> libRes = libraryService.getById(swathInput.getLibraryId());
        if (libRes.isFailed()) {
            return ResultDO.buildError(ResultCode.LIBRARY_NOT_EXISTED);
        }
        //创建实验初始化概览数据
        AnalyseOverviewDO overviewDO = createOverview(swathInput);
        analyseOverviewDAO.insert(overviewDO);

        try {
            raf = new RandomAccessFile(file, "r");

            if (swathInput.getBuildType() == 0) {
                extractMS1(raf, overviewDO.getId(), swathInput);
                extractMS2(raf, overviewDO.getId(), swathInput);
            } else if (swathInput.getBuildType() == 1) {
                extractMS1(raf, overviewDO.getId(), swathInput);
            } else if (swathInput.getBuildType() == 2) {
                extractMS2(raf, overviewDO.getId(), swathInput);
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

    @Override
    public ResultDO<List<AnalyseDataDO>> extractWithList(SwathInput swathInput) {
        ResultDO<List<AnalyseDataDO>> resultDO = new ResultDO(true);
        logger.info("基本条件检查开始");
        ResultDO checkResult = ConvolutionUtil.checkExperiment(swathInput.getExperimentDO());
        if (checkResult.isFailed()) {
            logger.error("条件检查失败:" + checkResult.getMsgInfo());
            return checkResult;
        }

        File file = (File) checkResult.getModel();
        RandomAccessFile raf = null;
        ResultDO<LibraryDO> libRes = libraryService.getById(swathInput.getLibraryId());
        if (libRes.isFailed()) {
            return ResultDO.buildError(ResultCode.LIBRARY_NOT_EXISTED);
        }
        List<AnalyseDataDO> dataList = new ArrayList<>();
        AnalyseOverviewDO overviewDO = createOverview(swathInput);
        analyseOverviewDAO.insert(overviewDO);

        try {
            raf = new RandomAccessFile(file, "r");
            dataList = extractMS2WithList(raf, overviewDO.getId(), swathInput);

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

        resultDO.setModel(dataList);
        return resultDO;
    }

    @Override
    public List<AnalyseDataDO> extractIrt(ExperimentDO exp, String iRtLibraryId, float mzExtractWindow) {

        ResultDO checkResult = ConvolutionUtil.checkExperiment(exp);
        if (checkResult.isFailed()) {
            logger.error(checkResult.getMsgInfo());
            return null;
        }

        File file = (File) checkResult.getModel();
        RandomAccessFile raf = null;

        List<WindowRang> rangs = getWindows(exp.getId());
        List<AnalyseDataDO> finalList = new ArrayList<>();
        try {
            raf = new RandomAccessFile(file, "r");
            for (WindowRang rang : rangs) {

                List<TargetTransition> coordinates;
                TreeMap<Float, MzIntensityPairs> rtMap;
                //Step2.获取标准库的目标肽段片段的坐标
                coordinates = transitionService.buildMS2Coordinates(iRtLibraryId, SlopeIntercept.create(), -1, rang.getMzStart(), rang.getMzEnd());
                if (coordinates.size() == 0) {
                    logger.warn("No Coordinates Found,Rang:" + rang.getMzStart() + ":" + rang.getMzEnd());
                    continue;
                }
                //Step3.获取指定索引列表
                ScanIndexQuery query = new ScanIndexQuery(exp.getId(), 2);
                query.setPrecursorMzStart(rang.getMzStart());
                query.setPrecursorMzEnd(rang.getMzEnd());
                List<SimpleScanIndex> indexes = scanIndexService.getSimpleAll(query);
                //Step4.提取指定原始谱图
                rtMap = parseSpectrum(raf, indexes, getParser(exp.getFileType()));
                //Step5.卷积并且存储数据
                convolute(finalList, coordinates, rtMap, null, mzExtractWindow, -1f, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return finalList;
    }

    @Override
    public ResultDO<SlopeIntercept> convAndIrt(ExperimentDO experimentDO, String iRtLibraryId, Float mzExtractWindow, SigmaSpacing sigmaSpacing) {
        try {
            logger.info("开始卷积数据");
            long start = System.currentTimeMillis();
            List<AnalyseDataDO> dataList = extractIrt(experimentDO, iRtLibraryId, mzExtractWindow);
//            List<AnalyseDataDO> dataList = FileUtil.getAnalyseDataList(getClass().getClassLoader().getResource("data/conv.json").getPath());         //这边先读取本地已经卷积好的iRT数据

            logger.info("卷积完毕,耗时:" + (System.currentTimeMillis() - start));
            start = System.currentTimeMillis();
            ResultDO resultDO = scoresService.computeIRt(dataList, iRtLibraryId, sigmaSpacing);
            logger.info("计算完毕,耗时:" + (System.currentTimeMillis() - start));
            return resultDO;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void extractMS1(RandomAccessFile raf, String overviewId, SwathInput swathInput) {

        //Step1.获取标准库目标卷积片段
        List<TargetTransition> coordinates = transitionService.buildMS1Coordinates(swathInput.getLibraryId(), swathInput.getSlopeIntercept(), swathInput.getRtExtractWindow());
        if (coordinates == null || coordinates.size() == 0) {
            return;
        }
        //Step2.获取指定索引列表
        List<SimpleScanIndex> indexes = scanIndexService.getSimpleAll(new ScanIndexQuery(swathInput.getExperimentDO().getId(), 1));
        //Step4.提取指定原始谱图
        TreeMap<Float, MzIntensityPairs> rtMap = parseSpectrum(raf, indexes, getParser(swathInput.getExperimentDO().getFileType()));
        //Step5.卷积并且存储数据
        convoluteAndInsert(coordinates, rtMap, overviewId, swathInput.getRtExtractWindow(), swathInput.getMzExtractWindow(), true);
    }

    private void extractMS2(RandomAccessFile raf, String overviewId, SwathInput swathInput) {

        //Step1.获取窗口信息.
        logger.info("获取Swath窗口信息");
        List<WindowRang> rangs = getWindows(swathInput.getExperimentDO().getId());

        //按窗口开始扫描.如果一共有N个窗口,则一共分N个批次进行扫描卷积
        logger.info("总计有窗口:" + rangs.size() + "个,开始进行MS2卷积计算");
        int count = 1;
        try {
            for (WindowRang rang : rangs) {

                long start = System.currentTimeMillis();
                List<TargetTransition> coordinates;
                TreeMap<Float, MzIntensityPairs> rtMap;
                //Step2.获取标准库的目标肽段片段的坐标
                coordinates = transitionService.buildMS2Coordinates(swathInput.getLibraryId(), swathInput.getSlopeIntercept(), swathInput.getRtExtractWindow(), rang.getMzStart(), rang.getMzEnd());
                if (coordinates.isEmpty()) {
                    logger.warn("No Coordinates Found,Rang:" + rang.getMzStart() + ":" + rang.getMzEnd());
                    continue;
                }
                //Step3.获取指定索引列表
                ScanIndexQuery query = new ScanIndexQuery(swathInput.getExperimentDO().getId(), 2);
                query.setPrecursorMzStart(rang.getMzStart());
                query.setPrecursorMzEnd(rang.getMzEnd());
                List<SimpleScanIndex> indexes = scanIndexService.getSimpleAll(query);
                //Step4.提取指定原始谱图
                rtMap = parseSpectrum(raf, indexes, getParser(swathInput.getExperimentDO().getFileType()));
                //Step5.卷积并且存储数据
                convoluteAndInsert(coordinates, rtMap, overviewId, swathInput.getRtExtractWindow(), swathInput.getMzExtractWindow(), false);

                logger.info("第" + count + "轮数据卷积完毕,耗时:" + (System.currentTimeMillis() - start) + "毫秒");
                count++;
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * 卷积MS2图谱并且输出最终结果
     *
     * @param raf
     * @param overviewId
     * @param swathInput
     */
    private List<AnalyseDataDO> extractMS2WithList(RandomAccessFile raf, String overviewId, SwathInput swathInput) {

        //Step1.获取窗口信息.
        logger.info("获取Swath窗口信息");
        List<WindowRang> rangs = getWindows(swathInput.getExperimentDO().getId());
        List<AnalyseDataDO> totalList = new ArrayList<>();
        //按窗口开始扫描.如果一共有N个窗口,则一共分N个批次进行扫描卷积
        logger.info("总计有窗口:" + rangs.size() + "个,开始进行MS2卷积计算");
        int count = 1;
        try {
            for (WindowRang rang : rangs) {

                long start = System.currentTimeMillis();
                List<TargetTransition> coordinates;
                TreeMap<Float, MzIntensityPairs> rtMap;
                //Step2.获取标准库的目标肽段片段的坐标
                coordinates = transitionService.buildMS2Coordinates(swathInput.getLibraryId(), swathInput.getSlopeIntercept(), swathInput.getRtExtractWindow(), rang.getMzStart(), rang.getMzEnd());
                if (coordinates.isEmpty()) {
                    logger.warn("No Coordinates Found,Rang:" + rang.getMzStart() + ":" + rang.getMzEnd());
                    continue;
                }
                //Step3.获取指定索引列表
                ScanIndexQuery query = new ScanIndexQuery(swathInput.getExperimentDO().getId(), 2);
                query.setPrecursorMzStart(rang.getMzStart());
                query.setPrecursorMzEnd(rang.getMzEnd());
                List<SimpleScanIndex> indexes = scanIndexService.getSimpleAll(query);
                //Step4.提取指定原始谱图
                rtMap = parseSpectrum(raf, indexes, getParser(swathInput.getExperimentDO().getFileType()));
                //Step5.卷积数据
                convolute(totalList, coordinates, rtMap, overviewId, swathInput.getMzExtractWindow(), swathInput.getRtExtractWindow(), false);
                //Step6.存储数据
                analyseDataDAO.insert(totalList);
                logger.info("第" + count + "轮数据卷积完毕,耗时:" + (System.currentTimeMillis() - start) + "毫秒");
                count++;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return totalList;
    }

    private TreeMap<Float, MzIntensityPairs> parseSpectrum(RandomAccessFile raf, List<SimpleScanIndex> indexes, BaseExpParser baseExpParser) {
        long start = System.currentTimeMillis();

        TreeMap<Float, MzIntensityPairs> rtMap = new TreeMap<>();

        for (SimpleScanIndex index : indexes) {
            MzIntensityPairs mzIntensityPairs = baseExpParser.parseOne(raf, index.getStart(), index.getEnd());
            rtMap.put(index.getRt(), mzIntensityPairs);
        }
        logger.info("解析" + indexes.size() + "条XML谱图文件总计耗时:" + (System.currentTimeMillis() - start));

        return rtMap;
    }

    private void convoluteAndInsert(List<TargetTransition> coordinates, TreeMap<Float, MzIntensityPairs> rtMap, String overviewId, Float rtExtractWindow, Float mzExtractWindow, boolean isMS1) {
        List<AnalyseDataDO> dataList = new ArrayList<>();
        for (TargetTransition ms : coordinates) {
            AnalyseDataDO dataDO = convForOne(isMS1, ms, rtMap, mzExtractWindow, rtExtractWindow, overviewId);
            dataList.add(dataDO);
        }
        analyseDataDAO.insert(dataList);
    }

    /**
     * 需要传入最终结果集的List对象
     *
     * @param finalList
     * @param coordinates
     * @param rtMap
     * @param overviewId
     * @param mzExtractWindow
     * @param rtExtractWindow
     * @param isMS1
     */
    private void convolute(List<AnalyseDataDO> finalList, List<TargetTransition> coordinates, TreeMap<Float, MzIntensityPairs> rtMap, String overviewId, Float mzExtractWindow, Float rtExtractWindow, boolean isMS1) {
        for (TargetTransition ms : coordinates) {
            AnalyseDataDO dataDO = convForOne(isMS1, ms, rtMap, mzExtractWindow, rtExtractWindow, overviewId);
            finalList.add(dataDO);
        }
    }

    private BaseExpParser getParser(String fileType) {
        //默认返回MzXMLParser
        if (fileType == null) {
            return mzXMLParser;
        }
        if (fileType.equals(Constants.EXP_SUFFIX_MZXML)) {
            return mzXMLParser;
        } else {
            return mzMLParser;
        }
    }

    private AnalyseDataDO convForOne(boolean isMS1, TargetTransition ms, TreeMap<Float, MzIntensityPairs> rtMap, Float mzExtractWindow, Float rtExtractWindow, String overviewId) {
        float mzStart = 0;
        float mzEnd = -1;
        //设置mz卷积窗口
        if (isMS1) {
            mzStart = ms.getPrecursorMz() - mzExtractWindow / 2;
            mzEnd = ms.getPrecursorMz() + mzExtractWindow / 2;
        } else {
            mzStart = ms.getProductMz() - mzExtractWindow / 2;
            mzEnd = ms.getProductMz() + mzExtractWindow / 2;
        }

        ArrayList<Float> rtList = new ArrayList<>();
        ArrayList<Float> intList = new ArrayList<>();

        //本参数用于检测是否在全谱图上检测到信号
        boolean isHit = false;
        for (Float rt : rtMap.keySet()) {
            if (rtExtractWindow != -1 && rt > ms.getRtEnd()) {
                break;
            }
            if (rtExtractWindow == -1 || (rt >= ms.getRtStart() && rt <= ms.getRtEnd())) {
                MzIntensityPairs pairs = rtMap.get(rt);
                Float[] pairMzArray = pairs.getMzArray();
                Float[] pairIntensityArray = pairs.getIntensityArray();
                Float acc = ConvolutionUtil.accumulation(pairMzArray, pairIntensityArray, mzStart, mzEnd);
                if (acc != 0) {
                    isHit = true; //如果本次的统计数据不为0,首先确认总信号是命中状态
                }
                rtList.add(rt);
                intList.add(acc);
            }
        }

        AnalyseDataDO dataDO = new AnalyseDataDO();
        dataDO.setTransitionId(ms.getId());
        if (isMS1) {
            dataDO.setMz(ms.getPrecursorMz());
            dataDO.setMsLevel(1);
        } else {
            dataDO.setMz(ms.getProductMz());
            dataDO.setMsLevel(2);
        }
        if (isHit) {
            Float[] rtArray = new Float[rtList.size()];
            Float[] intArray = new Float[intList.size()];
            rtList.toArray(rtArray);
            intList.toArray(intArray);
            dataDO.setRtArray(rtArray);
            dataDO.setIntensityArray(intArray);
            dataDO.setIsHit(true);
        } else {
            dataDO.setIsHit(false);
        }

        dataDO.setOverviewId(overviewId);
        dataDO.setAnnotations(ms.getAnnotations());
        dataDO.setCutInfo(ms.getCutInfo());
        dataDO.setPeptideRef(ms.getPeptideRef());
        dataDO.setProteinName(ms.getProteinName());
        dataDO.setIsDecoy(ms.getIsDecoy());
        dataDO.setRt(ms.getRt());
        dataDO.setUnimodMap(ms.getUnimodMap());
        return dataDO;
    }

    private AnalyseOverviewDO createOverview(SwathInput input) {
        //创建实验初始化概览数据
        AnalyseOverviewDO overviewDO = new AnalyseOverviewDO();
        String name = libraryService.getNameById(input.getLibraryId());
        overviewDO.setExpId(input.getExperimentDO().getId());
        overviewDO.setName(input.getExperimentDO().getName() + "-" + name + "-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        overviewDO.setExpName(input.getExperimentDO().getName());
        overviewDO.setLibraryId(input.getLibraryId());
        overviewDO.setLibraryName(name);
        overviewDO.setCreator(input.getCreator());
        overviewDO.setCreateDate(new Date());
        overviewDO.setRtExtractWindow(input.getRtExtractWindow());
        overviewDO.setMzExtractWindow(input.getMzExtractWindow());
        if (input.getSlopeIntercept() != null) {
            overviewDO.setSlope(input.getSlopeIntercept().getSlope());
            overviewDO.setIntercept(input.getSlopeIntercept().getIntercept());
        }

        return overviewDO;
    }
}
