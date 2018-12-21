package com.westlake.air.pecs.service.impl;

import com.westlake.air.pecs.constants.PositionType;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.constants.TaskStatus;
import com.westlake.air.pecs.dao.ExperimentDAO;
import com.westlake.air.pecs.dao.ScanIndexDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.params.LumsParams;
import com.westlake.air.pecs.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.*;
import com.westlake.air.pecs.domain.db.simple.SimpleScanIndex;
import com.westlake.air.pecs.domain.db.simple.TargetPeptide;
import com.westlake.air.pecs.domain.query.AnalyseDataQuery;
import com.westlake.air.pecs.domain.query.ExperimentQuery;
import com.westlake.air.pecs.domain.query.ScanIndexQuery;
import com.westlake.air.pecs.parser.AirdFileParser;
import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.service.*;
import com.westlake.air.pecs.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.*;

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
    PeptideService peptideService;
    @Autowired
    ScanIndexDAO scanIndexDAO;
    @Autowired
    MzXMLParser mzXMLParser;
    @Autowired
    AirdFileParser airdFileParser;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    TaskService taskService;
    @Autowired
    ScanIndexService scanIndexService;
    @Autowired
    LibraryService libraryService;
    @Autowired
    ScoreService scoreService;

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
            return ResultDO.buildError(ResultCode.UPDATE_ERROR);
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
        float ms2Interval = Math.round((ms2Indexes.get(1).getRt() - ms2Indexes.get(0).getRt()) * 100000) / 100000f;
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
            indexList = mzXMLParser.index(file, experimentDO, taskDO);

            taskDO.addLog("索引构建完毕,开始存储索引");
            taskService.update(taskDO);

            ResultDO resultDO = scanIndexService.insertAll(indexList, true);
            if (resultDO.isFailed()) {
                taskDO.addLog("索引存储失败" + resultDO.getMsgInfo());
                taskDO.finish(TaskStatus.FAILED.getName());
                taskService.update(taskDO);
                delete(experimentDO.getId());
                scanIndexService.deleteAllByExperimentId(experimentDO.getId());
            } else {
                taskDO.addLog("索引存储成功");
                taskDO.finish(TaskStatus.SUCCESS.getName());
                taskService.update(taskDO);
            }

        } catch (Exception e) {
            taskDO.addLog("索引存储失败:" + e.getMessage());
            taskDO.finish(TaskStatus.FAILED.getName());
            taskService.update(taskDO);
            e.printStackTrace();
        }
    }

    @Override
    public ResultDO<AnalyseOverviewDO> extract(LumsParams lumsParams) {
        ResultDO<AnalyseOverviewDO> resultDO = new ResultDO(true);
        logger.info("基本条件检查开始");
        ResultDO checkResult = ConvolutionUtil.checkExperiment(lumsParams.getExperimentDO());
        if (checkResult.isFailed()) {
            logger.error("条件检查失败:" + checkResult.getMsgInfo());
            return checkResult;
        }

        File file = (File) checkResult.getModel();
        RandomAccessFile raf = null;
        ResultDO<LibraryDO> libRes = libraryService.getById(lumsParams.getLibraryId());
        if (libRes.isFailed()) {
            return ResultDO.buildError(ResultCode.LIBRARY_NOT_EXISTED);
        }
        AnalyseOverviewDO overviewDO = createOverview(lumsParams);
        overviewDO.setLibraryPeptideCount(libRes.getModel().getTotalTargetCount().intValue());
        analyseOverviewService.insert(overviewDO);
        lumsParams.setOverviewId(overviewDO.getId());
        int totalNumber = 0;
        try {
            raf = new RandomAccessFile(file, "r");
            totalNumber = extract(raf, overviewDO.getId(), lumsParams);
            overviewDO.setTotalPeptideCount(totalNumber);
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            FileUtil.close(raf);
        }

        analyseOverviewService.update(overviewDO);
        resultDO.setModel(overviewDO);
        return resultDO;
    }

    @Override
    public ResultDO<AnalyseDataDO> extractOne(ExperimentDO exp, PeptideDO peptide, Float rtExtractorWindow, Float mzExtractorWindow) {
        ResultDO checkResult = ConvolutionUtil.checkExperiment(exp);
        if (checkResult.isFailed()) {
            logger.error("条件检查失败:" + checkResult.getMsgInfo());
            return checkResult;
        }

        File file = (File) checkResult.getModel();
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            //Step1.获取窗口信息.
            ScanIndexDO scanIndexDO = scanIndexService.getSwathIndex(exp.getId(), peptide.getMz().floatValue());
            //Step2.获取该窗口内的谱图Map,key值代表了RT
            TreeMap<Float, MzIntensityPairs> rtMap = airdFileParser.parseSwathBlockValues(raf, scanIndexDO);
            TargetPeptide tp = new TargetPeptide(peptide);
            Double rt = peptide.getRt();
            if (rtExtractorWindow == -1) {
                tp.setRtStart(-1);
                tp.setRtEnd(99999);
            } else {
                Double targetRt = (rt - exp.getIntercept()) / exp.getSlope();
                tp.setRtStart(targetRt.floatValue() - rtExtractorWindow / 2);
                tp.setRtEnd(targetRt.floatValue() + rtExtractorWindow / 2);
            }

            AnalyseDataDO dataDO = extractForOne(tp, rtMap, mzExtractorWindow, rtExtractorWindow, null);
            if (dataDO == null) {
                return ResultDO.buildError(ResultCode.ANALYSE_DATA_ARE_ALL_ZERO);
            }
            ResultDO<AnalyseDataDO> resultDO = new ResultDO<AnalyseDataDO>(true);
            resultDO.setModel(dataDO);
            return resultDO;
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            FileUtil.close(raf);
        }
        return null;
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

        List<WindowRang> rangs = exp.getWindowRangs();
        List<AnalyseDataDO> finalList = new ArrayList<>();

        HashMap<Float, ScanIndexDO> swathMap = scanIndexService.getSwathIndexList(exp.getId());

        try {
            raf = new RandomAccessFile(file, "r");
            for (WindowRang rang : rangs) {
                //Step2.获取标准库的目标肽段片段的坐标
                //key为rt
                TreeMap<Float, MzIntensityPairs> rtMap;
                List<TargetPeptide> coordinates = peptideService.buildMS2Coordinates(iRtLibraryId, SlopeIntercept.create(), -1, rang.getMzStart(), rang.getMzEnd());
                if (coordinates.size() == 0) {
                    logger.warn("No Coordinates Found,Rang:" + rang.getMzStart() + ":" + rang.getMzEnd());
                    continue;
                }
                //Step3.提取指定原始谱图
                rtMap = airdFileParser.parseSwathBlockValues(raf, swathMap.get(rang.getMzStart()));

                //Step4.卷积并且存储数据
                extractForIrt(finalList, coordinates, rtMap, null, mzExtractWindow, -1f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtil.close(raf);
        }

        return finalList;
    }

    @Override
    public ResultDO<SlopeIntercept> convAndIrt(ExperimentDO experimentDO, String iRtLibraryId, Float mzExtractWindow, SigmaSpacing sigmaSpacing) {
        try {
            logger.info("开始卷积数据");
            long start = System.currentTimeMillis();
            List<AnalyseDataDO> dataList = extractIrt(experimentDO, iRtLibraryId, mzExtractWindow);

            logger.info("卷积完毕,耗时:" + (System.currentTimeMillis() - start));
            start = System.currentTimeMillis();
            ResultDO resultDO = scoreService.computeIRt(dataList, iRtLibraryId, sigmaSpacing);
            logger.info("计算完毕,耗时:" + (System.currentTimeMillis() - start));
            return resultDO;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 卷积MS2图谱并且输出最终结果,不返回最终的卷积结果以减少内存的使用
     *
     * @param raf
     * @param overviewId
     * @param lumsParams
     */
    private int extract(RandomAccessFile raf, String overviewId, LumsParams lumsParams) {

        //Step1.获取窗口信息.
        logger.info("获取Swath窗口信息");
        List<WindowRang> rangs = lumsParams.getExperimentDO().getWindowRangs();
        HashMap<Float, ScanIndexDO> swathMap = scanIndexService.getSwathIndexList(lumsParams.getExperimentDO().getId());

        //按窗口开始扫描.如果一共有N个窗口,则一共分N个批次进行扫描卷积
        logger.info("总计有窗口:" + rangs.size() + "个,开始进行MS2卷积计算");
        int count = 1;
        int totalPeptideNum = 0;
        try {
            for (WindowRang rang : rangs) {
                long start = System.currentTimeMillis();
                int number = doExtract(raf, swathMap.get(rang.getMzStart()), rang, overviewId, lumsParams);
                logger.info("第" + count + "轮数据卷积完毕,扫描肽段:" + number + "个,耗时:" + (System.currentTimeMillis() - start) + "毫秒");
                count++;
                totalPeptideNum = totalPeptideNum + number;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return totalPeptideNum;
    }

    /**
     * 返回卷积到的数目
     *
     * @param raf
     * @param lumsParams
     * @param swathIndex
     * @param rang
     * @param overviewId
     * @return
     * @throws Exception
     */
    private int doExtract(RandomAccessFile raf, ScanIndexDO swathIndex, WindowRang rang, String overviewId, LumsParams lumsParams) throws Exception {
        List<TargetPeptide> coordinates;
        TreeMap<Float, MzIntensityPairs> rtMap;
        //Step2.获取标准库的目标肽段片段的坐标
        coordinates = peptideService.buildMS2Coordinates(lumsParams.getLibraryId(), lumsParams.getSlopeIntercept(), lumsParams.getRtExtractWindow(), rang.getMzStart(), rang.getMzEnd());
        if (coordinates.isEmpty()) {
            logger.warn("No Coordinates Found,Rang:" + rang.getMzStart() + ":" + rang.getMzEnd());
            return 0;
        }
        //Step3.提取指定原始谱图
        long start = System.currentTimeMillis();
        rtMap = airdFileParser.parseSwathBlockValues(raf, swathIndex);
        logger.info("IO及解码耗时:" + (System.currentTimeMillis() - start));
        if (lumsParams.isUseEpps()) {
            return eppsAndInsert(coordinates, rtMap, overviewId, lumsParams);
        } else {
            return extractAndInsert(coordinates, rtMap, overviewId, lumsParams.getRtExtractWindow(), lumsParams.getMzExtractWindow());
        }
    }

    private TreeMap<Float, MzIntensityPairs> parseSpectrum(RandomAccessFile raf, List<SimpleScanIndex> indexes, ExperimentDO experimentDO) {
        long start = System.currentTimeMillis();

        TreeMap<Float, MzIntensityPairs> rtMap = new TreeMap<>();

        for (SimpleScanIndex index : indexes) {
            MzIntensityPairs mzIntensityPairs = mzXMLParser.parseValue(raf, index.getPosStart(PositionType.MZXML), index.getPosEnd(PositionType.MZXML), experimentDO.getCompressionType(), experimentDO.getPrecision());
            rtMap.put(index.getRt(), mzIntensityPairs);
        }
        logger.info("解析" + indexes.size() + "条XML谱图文件总计耗时:" + (System.currentTimeMillis() - start));

        return rtMap;
    }

    /**
     * 最终的卷积结果需要落盘数据库,一般用于正式卷积的计算
     *
     * @param coordinates
     * @param rtMap
     * @param overviewId
     * @param rtExtractWindow
     * @param mzExtractWindow
     * @return
     */
    private int extractAndInsert(List<TargetPeptide> coordinates, TreeMap<Float, MzIntensityPairs> rtMap, String overviewId, Float rtExtractWindow, Float mzExtractWindow) {
        List<AnalyseDataDO> dataList = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (TargetPeptide ms : coordinates) {
            AnalyseDataDO dataDO = extractForOne(ms, rtMap, mzExtractWindow, rtExtractWindow, overviewId);
            if (dataDO == null) {
                continue;
            }
            //存储数据库前先进行压缩处理
            AnalyseDataUtil.compress(dataDO);
            dataList.add(dataDO);
        }
        logger.info("纯卷积耗时:" + (System.currentTimeMillis() - start));
        analyseDataService.insertAll(dataList, false);
        return dataList.size();
    }

    /**
     * 最终的卷积结果需要落盘数据库,一般用于正式卷积的计算
     *
     * @param coordinates
     * @param rtMap
     * @param overviewId
     * @param lumsParams
     * @return
     */
    private int eppsAndInsert(List<TargetPeptide> coordinates, TreeMap<Float, MzIntensityPairs> rtMap, String overviewId, LumsParams lumsParams) {
        List<AnalyseDataDO> dataList = new ArrayList<>();
        long start = System.currentTimeMillis();

        for (TargetPeptide tp : coordinates) {
            //Step1. 常规卷积,卷积结果不进行压缩处理
            AnalyseDataDO dataDO = extractForOne(tp, rtMap, lumsParams.getMzExtractWindow(), lumsParams.getRtExtractWindow(), overviewId);
            if (dataDO == null) {
                continue;
            }
            //Step2. 常规选峰及打分
            scoreService.scoreForOne(dataDO, tp, rtMap, lumsParams);
            AnalyseDataUtil.compress(dataDO);
            dataList.add(dataDO);
        }
        logger.info("卷积+选峰+打分耗时:" + (System.currentTimeMillis() - start));
        analyseDataService.insertAll(dataList, false);
        return dataList.size();
    }

    /**
     * 需要传入最终结果集的List对象
     * 最终的卷积结果存储在内存中不落盘,一般用于iRT的计算
     * 由于是直接在内存中的,所以卷积的结果不进行压缩
     *
     * @param finalList
     * @param coordinates
     * @param rtMap
     * @param overviewId
     * @param mzExtractWindow
     * @param rtExtractWindow
     */
    private void extractForIrt(List<AnalyseDataDO> finalList, List<TargetPeptide> coordinates, TreeMap<Float, MzIntensityPairs> rtMap, String overviewId, Float mzExtractWindow, Float rtExtractWindow) {
        for (TargetPeptide ms : coordinates) {
            AnalyseDataDO dataDO = extractForOne(ms, rtMap, mzExtractWindow, rtExtractWindow, overviewId);
            if (dataDO == null) {
                continue;
            }
            finalList.add(dataDO);
        }
    }

    private AnalyseDataDO extractForOne(PeptideDO peptide, TreeMap<Float, MzIntensityPairs> rtMap, LumsParams lumsParams, String overviewId) {

        TargetPeptide tp = peptide.toTargetPeptide();
        SlopeIntercept slopeIntercept = lumsParams.getSlopeIntercept();

        if (lumsParams.getRtExtractWindow() != -1) {
            float iRt = (tp.getRt() - slopeIntercept.getIntercept().floatValue()) / slopeIntercept.getSlope().floatValue();
            tp.setRtStart(iRt - lumsParams.getRtExtractWindow() / 2.0f);
            tp.setRtEnd(iRt + lumsParams.getRtExtractWindow() / 2.0f);
        } else {
            tp.setRtStart(-1);
            tp.setRtEnd(99999);
        }

        return extractForOne(tp, rtMap, lumsParams.getMzExtractWindow(), lumsParams.getRtExtractWindow(), overviewId);
    }

    private AnalyseDataDO extractForOne(TargetPeptide tp, TreeMap<Float, MzIntensityPairs> rtMap, Float mzExtractWindow, Float rtExtractWindow, String overviewId) {
        float mzStart = 0;
        float mzEnd = -1;

        //所有的碎片共享同一个RT数组
        ArrayList<Float> rtList = new ArrayList<>();
        for (Float rt : rtMap.keySet()) {
            if (rtExtractWindow != -1 && rt > tp.getRtEnd()) {
                break;
            }
            if (rtExtractWindow == -1 || (rt >= tp.getRtStart() && rt <= tp.getRtEnd())) {
                rtList.add(rt);
            }
        }

        Float[] rtArray = new Float[rtList.size()];
        rtList.toArray(rtArray);

        AnalyseDataDO dataDO = new AnalyseDataDO();
        dataDO.setPeptideId(tp.getId());
        dataDO.setRtArray(rtArray);
        dataDO.setOverviewId(overviewId);
        dataDO.setPeptideRef(tp.getPeptideRef());
        dataDO.setProteinName(tp.getProteinName());
        dataDO.setIsDecoy(tp.getIsDecoy());
        dataDO.setRt(tp.getRt());
        dataDO.setMz(tp.getMz());

        boolean isHit = false;
        for (FragmentInfo fi : tp.getFragmentMap().values()) {
            mzStart = fi.getMz().floatValue() - mzExtractWindow / 2;
            mzEnd = fi.getMz().floatValue() + mzExtractWindow / 2;

            //由于本函数极其注重性能,因此为了避免下面的拆箱装箱操作,在本处会预备两种类型的数组
            Float[] intArray = new Float[rtArray.length];
            boolean isAllZero = true;
            for (int i = 0; i < rtArray.length; i++) {
                MzIntensityPairs pairs = rtMap.get(rtArray[i]);
                Float[] pairMzArray = pairs.getMzArray();
                Float[] pairIntensityArray = pairs.getIntensityArray();
                float acc = ConvolutionUtil.accumulation(pairMzArray, pairIntensityArray, mzStart, mzEnd);
                if (acc != 0) {
                    isAllZero = false;
                }
                intArray[i] = acc;
            }

            dataDO.getMzMap().put(fi.getCutInfo(), fi.getMz().floatValue());
            if (isAllZero) {
                dataDO.getIntensityMap().put(fi.getCutInfo(), null);
            } else {
                isHit = true;
                dataDO.getIntensityMap().put(fi.getCutInfo(), intArray);
            }
        }

        //如果所有的片段均没有卷积到结果,则直接返回null
        if (!isHit) {
            return null;
        }

        return dataDO;
    }

    private AnalyseOverviewDO createOverview(LumsParams input) {
        //创建实验初始化概览数据
        AnalyseOverviewDO overviewDO = new AnalyseOverviewDO();
        overviewDO.setExpId(input.getExperimentDO().getId());
        overviewDO.setExpName(input.getExperimentDO().getName());

        if (input.getLibraryId() != null) {
            String name = libraryService.getNameById(input.getLibraryId());
            overviewDO.setLibraryId(input.getLibraryId());
            overviewDO.setLibraryName(name);
            overviewDO.setName(input.getExperimentDO().getName() + "-" + name + "-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        }

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

    private AnalyseOverviewDO createOverview(LumsParams input, String fatherOverviewId) {
        //创建实验初始化概览数据
        AnalyseOverviewDO overviewDO = new AnalyseOverviewDO();
        overviewDO.setExpId(input.getExperimentDO().getId());
        overviewDO.setExpName(input.getExperimentDO().getName());

        if (input.getLibraryId() != null) {
            String name = libraryService.getNameById(input.getLibraryId());
            overviewDO.setLibraryId(input.getLibraryId());
            overviewDO.setLibraryName(name);
            overviewDO.setName(input.getExperimentDO().getName() + "-" + name + "-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        } else {
            overviewDO.setName(input.getExperimentDO().getName() + "- Farther Overview Id:" + fatherOverviewId + "-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        }

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
