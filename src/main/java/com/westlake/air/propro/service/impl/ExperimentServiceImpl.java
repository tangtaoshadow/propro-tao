package com.westlake.air.propro.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.PositionType;
import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.constants.TaskStatus;
import com.westlake.air.propro.dao.ConfigDAO;
import com.westlake.air.propro.dao.ExperimentDAO;
import com.westlake.air.propro.dao.ScanIndexDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.WindowRange;
import com.westlake.air.propro.domain.bean.compressor.AircInfo;
import com.westlake.air.propro.domain.bean.compressor.AirdInfo;
import com.westlake.air.propro.domain.params.LumsParams;
import com.westlake.air.propro.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.domain.db.simple.SimpleScanIndex;
import com.westlake.air.propro.domain.db.simple.TargetPeptide;
import com.westlake.air.propro.domain.query.ExperimentQuery;
import com.westlake.air.propro.domain.query.ScanIndexQuery;
import com.westlake.air.propro.algorithm.parser.AirdFileParser;
import com.westlake.air.propro.algorithm.parser.MzXMLParser;
import com.westlake.air.propro.service.*;
import com.westlake.air.propro.utils.*;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
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
    ExperimentService experimentService;
    @Autowired
    TaskService taskService;
    @Autowired
    ScanIndexService scanIndexService;
    @Autowired
    LibraryService libraryService;
    @Autowired
    ScoreService scoreService;
    @Autowired
    ConfigDAO configDAO;
    @Autowired
    ProjectService projectService;

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
    public long count(ExperimentQuery query) {
        return experimentDAO.count(query);
    }

    @Override
    public List<ExperimentDO> getAll(ExperimentQuery query) {
        return experimentDAO.getAll(query);
    }

    @Override
    public List<ExperimentDO> getAllByProjectName(String projectName) {
        ExperimentQuery query = new ExperimentQuery();
        query.setProjectName(projectName);
        return experimentDAO.getAll(query);
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
    public List<WindowRange> getWindows(String expId) {
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

        List<WindowRange> windowRangs = new ArrayList<>();
        float ms2Interval = Math.round((ms2Indexes.get(1).getRt() - ms2Indexes.get(0).getRt()) * 100000) / 100000f;
        for (int i = 0; i < ms2Indexes.size(); i++) {
            WindowRange rang = new WindowRange();
            rang.setStart(ms2Indexes.get(i).getPrecursorMzStart());
            rang.setEnd(ms2Indexes.get(i).getPrecursorMzEnd());
            rang.setMz(ms2Indexes.get(i).getPrecursorMz());
            rang.setInterval(ms2Interval);
            windowRangs.add(rang);
        }
        return windowRangs;
    }

    @Override
    public List<WindowRange> getPrmWindows(String expId) {
        ScanIndexQuery query = new ScanIndexQuery();
        query.setMsLevel(2);
        query.setExperimentId(expId);
        List<ScanIndexDO> ms2Indexes = scanIndexDAO.getAll(query);
        if (ms2Indexes == null || ms2Indexes.size() == 0) {
            return null;
        }

        List<WindowRange> windowRangs = new ArrayList<>();
        List<Float> precursorUniqueMz = new ArrayList<>();

        for(ScanIndexDO ms2Index : ms2Indexes){
            Float precursorMz = ms2Index.getPrecursorMz();
            if(precursorUniqueMz.contains(precursorMz)){
                continue;
            }
            precursorUniqueMz.add(precursorMz);
            WindowRange rang = new WindowRange();
            rang.setStart(ms2Index.getPrecursorMzStart());
            rang.setEnd(ms2Index.getPrecursorMzEnd());
            rang.setMz(ms2Index.getPrecursorMz());
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
    public void uploadAirdFile(ExperimentDO experimentDO, String airdFilePath, TaskDO taskDO) {
        if (!FileUtil.isAirdFile(airdFilePath)) {
            taskDO.addLog("Aird文件格式不正确");
            taskDO.finish(TaskStatus.FAILED.getName());
            taskService.update(taskDO);
            return;
        }

        experimentDO.setAirdPath(airdFilePath);

        String airdIndexPath = FileUtil.getAirdIndexFilePath(airdFilePath);
        experimentDO.setAirdIndexPath(airdIndexPath);
        try {
            String airdInfoJson = FileUtil.readFile(airdIndexPath);
            AirdInfo airdInfo = null;
            try {
                airdInfo = JSONObject.parseObject(airdInfoJson, AirdInfo.class);
            } catch (Exception e) {
                taskDO.addLog("索引文件内部格式异常,JSON转换错误");
                taskDO.finish(TaskStatus.FAILED.getName());
                taskService.update(taskDO);
                return;
            }
            experimentDO.setWindowRanges(airdInfo.getRangeList());
            experimentDO.setByteOrder(airdInfo.getByteOrder());
            experimentDO.setStrategies(airdInfo.getStrategies());
            experimentDO.setOverlap(airdInfo.getOverlap());
            experimentDO.setDescription("rawId:"+airdInfo.getRawId()+";"+experimentDO.getDescription());
            for (ScanIndexDO scanIndex : airdInfo.getScanIndexList()) {
                scanIndex.setExperimentId(experimentDO.getId());
            }
            for (ScanIndexDO swathIndex : airdInfo.getSwathIndexList()) {
                swathIndex.setExperimentId(experimentDO.getId());
            }

            scanIndexService.insertAll(airdInfo.getScanIndexList(), false);
            scanIndexService.insertAll(airdInfo.getSwathIndexList(), false);

            taskDO.addLog("索引存储成功");
            taskDO.finish(TaskStatus.SUCCESS.getName());
            taskService.update(taskDO);

        } catch (IOException e) {
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

        //准备读取Aird文件
        File file = (File) checkResult.getModel();
        RandomAccessFile raf = null;

        ResultDO<LibraryDO> libRes = libraryService.getById(lumsParams.getLibraryId());
        if (libRes.isFailed()) {
            return ResultDO.buildError(ResultCode.LIBRARY_NOT_EXISTED);
        }
        AnalyseOverviewDO overviewDO = createOverview(lumsParams);
        overviewDO.setLibraryPeptideCount(libRes.getModel().getTotalCount().intValue());
        analyseOverviewService.insert(overviewDO);

        //准备卷积结果输出文件及计算相关的路径
        String configAircPath = projectService.getByName(lumsParams.getExperimentDO().getProjectName()).getModel().getAircPath();
        String fileParent = "";
        if (configAircPath != null && !configAircPath.isEmpty()) {
            fileParent = configAircPath;
        } else {
            fileParent = file.getParent();
        }
        String fileNameWithoutSuffix = file.getName().replace(".aird", "");
        String aircFilePath = fileParent + fileNameWithoutSuffix + "-" + overviewDO.getId() + Constants.SUFFIX_AIRUS_CONVOLUTION;
        String aircIndexPath = fileParent + fileNameWithoutSuffix + "-" + overviewDO.getId() + Constants.SUFFIX_AIRUS_CONVOLUTION_INFO;
        File aircFile = new File(aircFilePath);
        File aircIndexFile = new File(aircIndexPath);
        FileWriter fwInfo = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;

        lumsParams.setOverviewId(overviewDO.getId());

        try {
            raf = new RandomAccessFile(file, "r");

            if (!aircIndexFile.exists()) {
                aircIndexFile.getParentFile().mkdirs();
                aircIndexFile.createNewFile();
            }
            if (!aircFile.exists()) {
                aircFile.getParentFile().mkdirs();
                aircFile.createNewFile();
            }

            //所有的索引数据均以JSON文件保存
            fwInfo = new FileWriter(aircIndexFile.getAbsoluteFile());
            fos = new FileOutputStream(aircFile.getAbsoluteFile());
            bos = new BufferedOutputStream(fos);

            //核心函数在这里
            List<AnalyseDataDO> totalDataList = extract(raf, bos, overviewDO.getId(), lumsParams);
            Long count = 0L;
            for (AnalyseDataDO dataDO: totalDataList){
                count += dataDO.getFeatureScoresList().size();
            }
            overviewDO.setTotalPeptideCount(totalDataList.size());
            overviewDO.setAircIndexPath(aircIndexPath);
            overviewDO.setAircPath(aircFilePath);
            overviewDO.setHasAircFile(true);
            overviewDO.setPeakCount(count);
            //将AircInfo写入到本地文件
            AircInfo aircInfo = new AircInfo();
            aircInfo.setOverview(overviewDO);
            aircInfo.setDataList(totalDataList);
            fwInfo.write(JSON.toJSONString(aircInfo));

        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } finally {
            FileUtil.close(raf);
            FileUtil.close(fwInfo);
            FileUtil.close(bos);
            FileUtil.close(fos);
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
            TreeMap<Float, MzIntensityPairs> rtMap;
            try{
                rtMap = airdFileParser.parseSwathBlockValues(raf, scanIndexDO , ByteUtil.getByteOrder(exp.getByteOrder()));
            }catch (Exception e){
                logger.error("PrecursorMZ:"+scanIndexDO.getPrecursorMz());
                throw e;
            }

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
            e.printStackTrace();
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

        List<WindowRange> ranges = exp.getWindowRanges();
        List<AnalyseDataDO> finalList = new ArrayList<>();

        HashMap<Float, ScanIndexDO> swathMap = scanIndexService.getSwathIndexList(exp.getId());

        try {
            raf = new RandomAccessFile(file, "r");
            for (WindowRange range : ranges) {
                //Step2.获取标准库的目标肽段片段的坐标
                //key为rt
                TreeMap<Float, MzIntensityPairs> rtMap;
                List<TargetPeptide> coordinates = peptideService.buildMS2Coordinates(iRtLibraryId, SlopeIntercept.create(), -1, range, null, exp.getType(), false);
                if (coordinates.size() == 0) {
                    logger.warn("No iRT Coordinates Found,Rang:" + range.getStart() + ":" + range.getEnd());
                    continue;
                }
                //Step3.提取指定原始谱图
                ScanIndexDO index = swathMap.get(range.getStart());
                if(index != null){
                    try{
                        rtMap = airdFileParser.parseSwathBlockValues(raf, index, ByteUtil.getByteOrder(exp.getByteOrder()));
                    }catch (Exception e){
                        logger.error("PrecursorMZStart:"+index.getPrecursorMzStart());
                        throw e;
                    }

                }else{
                    continue;
                }

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
            if(dataList == null){
                return ResultDO.buildError(ResultCode.IRT_EXCEPTION);
            }
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

    public HashMap<Float, Float[]> getPrmRtWindowMap(String expId){
        ScanIndexQuery query = new ScanIndexQuery();
        query.setExperimentId(expId);
        query.setMsLevel(2);
        List<ScanIndexDO> msAllIndexes = scanIndexDAO.getAll(query);
        HashMap<Float, Float[]> peptideMap = new HashMap<>();
        for(ScanIndexDO scanIndexDO: msAllIndexes){
            float precursorMz = scanIndexDO.getPrecursorMz();
            if (!peptideMap.containsKey(precursorMz)) {
                peptideMap.put(precursorMz, new Float[]{Float.MAX_VALUE, Float.MIN_VALUE});
            }
            if(scanIndexDO.getRt()>peptideMap.get(precursorMz)[1]){
                peptideMap.get(precursorMz)[1] = scanIndexDO.getRt();
            }
            if(scanIndexDO.getRt()<peptideMap.get(precursorMz)[0]){
                peptideMap.get(precursorMz)[0] = scanIndexDO.getRt();
            }
        }
        return peptideMap;
    }

    /**
     * 卷积MS2图谱并且输出最终结果,不返回最终的卷积结果以减少内存的使用
     *
     * @param raf        用于读取Aird文件
     * @param bos        用于存储卷积结果
     * @param overviewId
     * @param lumsParams
     */
    private List<AnalyseDataDO> extract(RandomAccessFile raf, BufferedOutputStream bos, String overviewId, LumsParams lumsParams) {

        //Step1.获取窗口信息.
        logger.info("获取Swath窗口信息");
        List<WindowRange> rangs = lumsParams.getExperimentDO().getWindowRanges();
        HashMap<Float, ScanIndexDO> swathMap = scanIndexService.getSwathIndexList(lumsParams.getExperimentDO().getId());
        HashMap<Float, Float[]> rtRangeMap = null;
        if (lumsParams.getExperimentDO().getType().equals(Constants.EXP_TYPE_PRM)){
            String expId = analyseOverviewService.getById(overviewId).getModel().getExpId();
            rtRangeMap = experimentService.getPrmRtWindowMap(expId);
        }
        //按窗口开始扫描.如果一共有N个窗口,则一共分N个批次进行扫描卷积
        logger.info("总计有窗口:" + rangs.size() + "个,开始进行MS2卷积计算");
        int count = 1;
        List<AnalyseDataDO> totalDataList = new ArrayList<>();
        try {
            long startPosition = 0;
            for (WindowRange range : rangs) {
                long start = System.currentTimeMillis();
//                if (swathMap.get(range.getStart()) == null){
//                    System.out.println("");
//                }
                List<AnalyseDataDO> dataList = doExtract(raf, swathMap.get(range.getStart()), range, rtRangeMap, overviewId, lumsParams);
                if (dataList != null) {
                    totalDataList.addAll(dataList);
                    //将卷积的核心数据压缩以后存储到本地
                    for (AnalyseDataDO data : dataList) {
                        byte[] rtArray = data.getConvRtArray();
                        byte[] intensityAll = new byte[0];
                        //存储rt array的位置信息
                        data.setStartPos(startPosition);
                        List<Integer> deltaList = new ArrayList<>();
                        deltaList.add(rtArray.length);
                        startPosition = startPosition + rtArray.length;
                        //逐个存储fragment对应的卷积位置的
                        for(String key : data.getConvIntensityMap().keySet()){
                            deltaList.add(data.getConvIntensityMap().get(key).length);
                            startPosition = startPosition + data.getConvIntensityMap().get(key).length;
                            intensityAll = ArrayUtils.addAll(intensityAll, data.getConvIntensityMap().get(key));
                        }

                        byte[] result = ArrayUtils.addAll(rtArray, intensityAll);

                        bos.write(result);
                        //压缩数据已经存储到本地,清空内容中的压缩数据
                        data.setPosDeltaList(deltaList);
                        data.setConvRtArray(null);
                        for(String key : data.getConvIntensityMap().keySet()){
                            data.getConvIntensityMap().put(key, null);
                        }
                    }
                }
                analyseDataService.insertAll(dataList, false);
                logger.info("第" + count + "轮数据卷积完毕,有效肽段:" + (dataList == null ? 0 : dataList.size()) + "个,耗时:" + (System.currentTimeMillis() - start) + "毫秒");
                count++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        return totalDataList;
    }

    /**
     * 返回卷积到的数目
     *
     * @param raf
     * @param lumsParams
     * @param swathIndex
     * @param range
     * @param overviewId
     * @return
     * @throws Exception
     */
    private List<AnalyseDataDO> doExtract(RandomAccessFile raf, ScanIndexDO swathIndex, WindowRange range, HashMap<Float, Float[]> rtRangeMap, String overviewId, LumsParams lumsParams) throws Exception {
        List<TargetPeptide> coordinates;
        TreeMap<Float, MzIntensityPairs> rtMap;
        //Step2.获取标准库的目标肽段片段的坐标
        Float[] rtRange = null;
        if (rtRangeMap != null){
            float precursorMz = range.getMz();
            rtRange = rtRangeMap.get(precursorMz);
        }
        if (rtRange == null){
            System.out.println("debug here");
        }
        coordinates = peptideService.buildMS2Coordinates(lumsParams.getLibraryId(), lumsParams.getSlopeIntercept(), lumsParams.getRtExtractWindow(), range, rtRange, lumsParams.getExperimentDO().getType(),lumsParams.isUniqueOnly());
        if (coordinates.isEmpty()) {
            logger.warn("No Coordinates Found,Rang:" + range.getStart() + ":" + range.getEnd());
            return null;
        }
        if (coordinates.size() != 2){
            logger.warn("coordinate size != 2,Rang:" + range.getStart() + ":" + range.getEnd());
        }
        //Step3.提取指定原始谱图
        long start = System.currentTimeMillis();

        rtMap = airdFileParser.parseSwathBlockValues(raf, swathIndex, ByteUtil.getByteOrder(lumsParams.getExperimentDO().getByteOrder()));

        logger.info("IO及解码耗时:" + (System.currentTimeMillis() - start));
        if (lumsParams.isUseEpps()) {
            return epps(coordinates, rtMap, overviewId, lumsParams);
        } else {
            return extract(coordinates, rtMap, overviewId, lumsParams.getRtExtractWindow(), lumsParams.getMzExtractWindow());
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
    private List<AnalyseDataDO> extract(List<TargetPeptide> coordinates, TreeMap<Float, MzIntensityPairs> rtMap, String overviewId, Float rtExtractWindow, Float mzExtractWindow) {
        List<AnalyseDataDO> dataList = new ArrayList<>();
        long start = System.currentTimeMillis();
        //PRM use adaptiveWindow
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
        return dataList;
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
    private List<AnalyseDataDO> epps(List<TargetPeptide> coordinates, TreeMap<Float, MzIntensityPairs> rtMap, String overviewId, LumsParams lumsParams) {
        List<AnalyseDataDO> dataList = new ArrayList<>();
        long start = System.currentTimeMillis();

        HashSet<String> targetIgnorePeptides = new HashSet<>();
        List<TargetPeptide> decoyList = new ArrayList<>();
        //传入的coordinates是没有经过排序的,需要排序先处理真实肽段,再处理伪肽段
        for (TargetPeptide tp : coordinates) {
            if (tp.getIsDecoy()) {
                decoyList.add(tp);
                continue;
            }
            //Step1. 常规卷积,卷积结果不进行压缩处理
            AnalyseDataDO dataDO = extractForOne(tp, rtMap, lumsParams.getMzExtractWindow(), lumsParams.getRtExtractWindow(), overviewId);
            if (dataDO == null) {
                logger.info("未卷积到任何片段,PeptideRef:"+tp.getPeptideRef());
                continue;
            }
//            if (dataDO.getRtArray().length < 20){
//                logger.info("卷积谱图时间太短,PeptideRef:" + tp.getPeptideRef());
//                continue;
//            }
            //Step2. 常规选峰及打分
            scoreService.scoreForOne(dataDO, tp, rtMap, lumsParams);
            if (dataDO.getFeatureScoresList() == null) {
//                logger.info("未满足基础条件,直接忽略:"+dataDO.getPeptideRef());
                targetIgnorePeptides.add(dataDO.getPeptideRef());
                continue;
            }
            AnalyseDataUtil.compress(dataDO);
            dataList.add(dataDO);
        }
        for (TargetPeptide tp : decoyList) {
            //如果伪肽段在忽略列表里面,那么直接忽略
            if (targetIgnorePeptides.contains(tp.getPeptideRef()) && !lumsParams.getExperimentDO().getType().equals(Constants.EXP_TYPE_PRM)) {
                continue;
            }
            //Step1. 常规卷积,卷积结果不进行压缩处理
            AnalyseDataDO dataDO = extractForOne(tp, rtMap, lumsParams.getMzExtractWindow(), lumsParams.getRtExtractWindow(), overviewId);
            if (dataDO == null) {
                continue;
            }
            //Step2. 常规选峰及打分
            scoreService.scoreForOne(dataDO, tp, rtMap, lumsParams);
            if (dataDO.getFeatureScoresList() == null) {
                continue;
            }
            AnalyseDataUtil.compress(dataDO);
            dataList.add(dataDO);
        }
        logger.info("卷积+选峰+打分耗时:" + (System.currentTimeMillis() - start));
        return dataList;
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
        boolean useAdaptiveWindow = false;
        if (mzExtractWindow == -1){
            useAdaptiveWindow = true;
        }
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
                float acc;
                if (useAdaptiveWindow){
                    acc = ConvolutionUtil.adaptiveAccumulation(pairMzArray, pairIntensityArray, fi.getMz().floatValue());
                }else {
                    acc = ConvolutionUtil.accumulation(pairMzArray, pairIntensityArray, mzStart, mzEnd);
                }
                if (acc != 0) {
                    isAllZero = false;
                }
                intArray[i] = acc;
            }
            if (isAllZero) {
                continue;
                //                dataDO.getIntensityMap().put(fi.getCutInfo(), null);
            } else {
                isHit = true;
                dataDO.getIntensityMap().put(fi.getCutInfo(), intArray);
            }
            dataDO.getMzMap().put(fi.getCutInfo(), fi.getMz().floatValue());
        }

        //如果所有的片段均没有卷积到结果,则直接返回null
        if (!isHit) {
            return null;
        }

        return dataDO;
    }

    /**
     * 根据input入参初始化一个AnalyseOverviewDO
     *
     * @param input
     * @return
     */
    private AnalyseOverviewDO createOverview(LumsParams input) {
        //创建实验初始化概览数据
        AnalyseOverviewDO overviewDO = new AnalyseOverviewDO();
        overviewDO.setExpId(input.getExperimentDO().getId());
        overviewDO.setExpName(input.getExperimentDO().getName());
        overviewDO.setType(input.getExperimentDO().getType());
        if (input.getLibraryId() != null) {
            String name = libraryService.getNameById(input.getLibraryId());
            overviewDO.setLibraryId(input.getLibraryId());
            overviewDO.setLibraryName(name);
            overviewDO.setName(input.getExperimentDO().getName() + "-" + name + "-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        }

        overviewDO.setCreator(input.getCreator());
        overviewDO.setCreateDate(new Date());
        overviewDO.setNote(input.getNote());
        overviewDO.setRtExtractWindow(input.getRtExtractWindow());
        overviewDO.setMzExtractWindow(input.getMzExtractWindow());
        overviewDO.setSigma(input.getSigmaSpacing().getSigma());
        overviewDO.setSpacing(input.getSigmaSpacing().getSpacing());
        overviewDO.setShapeScoreThreshold(input.getXcorrShapeThreshold());
        overviewDO.setShapeScoreWeightThreshold(input.getXcorrShapeWeightThreshold());
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
