package com.westlake.air.pecs.service.impl;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.dao.AnalyseDataDAO;
import com.westlake.air.pecs.dao.AnalyseOverviewDAO;
import com.westlake.air.pecs.dao.ExperimentDAO;
import com.westlake.air.pecs.dao.ScanIndexDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.pecs.domain.db.*;
import com.westlake.air.pecs.domain.db.simple.SimpleScanIndex;
import com.westlake.air.pecs.domain.db.simple.TargetTransition;
import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.query.ExperimentQuery;
import com.westlake.air.pecs.domain.query.ScanIndexQuery;
import com.westlake.air.pecs.parser.BaseExpParser;
import com.westlake.air.pecs.parser.MzMLParser;
import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.service.*;
import com.westlake.air.pecs.utils.ConvolutionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;

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
                indexList = mzXMLParser.index(file, experimentDO.getId(), taskDO);
            } else if (experimentDO.getFileType().equals(Constants.EXP_SUFFIX_MZML)) {
                indexList = mzMLParser.index(file, experimentDO.getId(), taskDO);
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
    public ResultDO extract(ExperimentDO experimentDO, String libraryId, String creator, float rtExtractWindow, float mzExtractWindow, int buildType, TaskDO taskDO) {
        ResultDO resultDO = new ResultDO(true);
        //基本条件检查
        taskDO.addLog("基本条件检查开始");
        taskService.update(taskDO);

        ResultDO checkResult = ConvolutionUtil.checkExperiment(experimentDO);
        if (checkResult.isFailed()) {
            return checkResult;
        }

        File file = (File) checkResult.getModel();
        RandomAccessFile raf = null;

        //创建实验初始化概览数据
        AnalyseOverviewDO overviewDO = new AnalyseOverviewDO();
        overviewDO.setExpId(experimentDO.getId());
        overviewDO.setName(experimentDO.getName() + "-" + experimentDO.getLibraryName() + "-" + experimentDO.getIRtLibraryName() + "-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        overviewDO.setExpName(experimentDO.getName());
        overviewDO.setLibraryId(libraryId);
        overviewDO.setLibraryName(libraryService.getNameById(libraryId));
        overviewDO.setCreator(creator);
        overviewDO.setCreateDate(new Date());
        overviewDO.setRtExtractWindow(rtExtractWindow);
        overviewDO.setMzExtractWindow(mzExtractWindow);
        analyseOverviewDAO.insert(overviewDO);

        try {
            raf = new RandomAccessFile(file, "r");

            if (buildType == 0) {
                extractMS1(raf, experimentDO, overviewDO.getId(), libraryId, rtExtractWindow, mzExtractWindow, taskDO);
                extractMS2(file, experimentDO, overviewDO.getId(), libraryId, rtExtractWindow, mzExtractWindow, taskDO);
            } else if (buildType == 1) {
                extractMS1(raf, experimentDO, overviewDO.getId(), libraryId, rtExtractWindow, mzExtractWindow, taskDO);
            } else if (buildType == 2) {
                extractMS2(file, experimentDO, overviewDO.getId(), libraryId, rtExtractWindow, mzExtractWindow, taskDO);
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

    private void extractMS1(RandomAccessFile raf, ExperimentDO exp, String overviewId, String libraryId, float rtExtractWindow, float mzExtractWindow, TaskDO taskDO) {

        //Step1.获取标准库目标卷积片段
        taskDO.addLog("构建MS1卷积坐标");
        taskService.update(taskDO);
        List<TargetTransition> coordinates = transitionService.buildMS1Coordinates(libraryId, rtExtractWindow, taskDO);
        if (coordinates == null || coordinates.size() == 0) {
            taskDO.addLog("标准库目标为空");
            taskDO.finish(TaskDO.STATUS_FAILED);
            taskService.update(taskDO);
            return;
        }

        //Step2.获取指定索引列表
        taskDO.addLog("获取全部MS1谱图索引列表");
        taskService.update(taskDO);
        List<SimpleScanIndex> indexes = scanIndexService.getSimpleAll(new ScanIndexQuery(exp.getId(), 1));

        //Step4.提取指定原始谱图
        taskDO.addLog("解析MS1指定谱图");
        taskService.update(taskDO);
        TreeMap<Float, MzIntensityPairs> rtMap = parseSpectrum(raf, indexes, getParser(exp.getFileType()), taskDO);

        //Step5.卷积并且存储数据
        taskDO.addLog("开始卷积MS1数据");
        taskService.update(taskDO);
        convoluteAndInsert(coordinates, rtMap, overviewId, rtExtractWindow, mzExtractWindow, true, taskDO);
    }

    private void extractMS2(File file, ExperimentDO exp, String overviewId, String libraryId, float rtExtractWindow, float mzExtractWindow, TaskDO taskDO) {

        //Step1.获取窗口信息.
        taskDO.addLog("获取Swath窗口信息");
        taskService.update(taskDO);
        List<WindowRang> rangs = getWindows(exp.getId());

        //按窗口开始扫描.如果一共有N个窗口,则一共分N个批次进行扫描卷积
        taskDO.addLog("总计有窗口:" + rangs.size() + "个,开始进行MS2卷积计算");
        taskService.update(taskDO);

        RandomAccessFile raf = null;
        int count = 1;
        try {
            raf = new RandomAccessFile(file, "r");
            for (WindowRang rang : rangs) {

                long start = System.currentTimeMillis();
                List<TargetTransition> coordinates;
                TreeMap<Float, MzIntensityPairs> rtMap;
                //Step2.获取指定索引列表
                ScanIndexQuery query = new ScanIndexQuery(exp.getId(), 2);
                query.setPrecursorMzStart(rang.getMzStart());
                query.setPrecursorMzEnd(rang.getMzEnd());
                List<SimpleScanIndex> indexes = scanIndexService.getSimpleAll(query);
                //Step3.获取标准库的目标肽段片段的坐标
                coordinates = transitionService.buildMS2Coordinates(libraryId, rtExtractWindow, rang.getMzStart(), rang.getMzEnd(), taskDO);
                //Step4.提取指定原始谱图
                rtMap = parseSpectrum(raf, indexes, getParser(exp.getFileType()), taskDO);
                //Step5.卷积并且存储数据
                convoluteAndInsert(coordinates, rtMap, overviewId, rtExtractWindow, mzExtractWindow, false, taskDO);

                taskDO.addLog("第" + count + "轮数据卷积完毕,耗时:" + (System.currentTimeMillis() - start) + "毫秒");
                taskService.update(taskDO);
                count++;
            }
            taskDO.finish(TaskDO.STATUS_SUCCESS);
            taskService.update(taskDO);

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

    private TreeMap<Float, MzIntensityPairs> parseSpectrum(RandomAccessFile raf, List<SimpleScanIndex> indexes, BaseExpParser baseExpParser, TaskDO taskDO) {
        long start = System.currentTimeMillis();

        TreeMap<Float, MzIntensityPairs> rtMap = new TreeMap<>();
        for (SimpleScanIndex index : indexes) {
            rtMap.put(index.getRt(), baseExpParser.parseOne(raf, index.getStart(), index.getEnd()));
        }
        taskDO.addLog("解析" + indexes.size() + "条XML谱图文件总计耗时:" + (System.currentTimeMillis() - start));
        taskService.update(taskDO);
        return rtMap;
    }

    private void convoluteAndInsert(List<TargetTransition> coordinates, TreeMap<Float, MzIntensityPairs> rtMap, String overviewId, Float rtExtractWindow, Float mzExtractWindow, boolean isMS1, TaskDO taskDO) {
        int logCountForMSTarget = 0;
        Long start = System.currentTimeMillis();
        float mzStart = 0;
        float mzEnd = -1;
        List<AnalyseDataDO> dataList = new ArrayList<>();

        for (TargetTransition ms : coordinates) {

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

            boolean isHit = false;
            for (Float rt : rtMap.keySet()) {
                if (rtExtractWindow != -1 && rt > ms.getRtEnd()) {
                    break;
                }
                if (rtExtractWindow == -1 || (rt >= ms.getRtStart() && rt <= ms.getRtEnd())) {
                    MzIntensityPairs pairs = rtMap.get(rt);
                    Float[] pairMzArray = pairs.getMzArray();
                    Float[] pairIntensityArray = pairs.getIntensityArray();
                    rtList.add(rt);
                    Float acc = ConvolutionUtil.accumulation(pairMzArray, pairIntensityArray, mzStart, mzEnd);
                    if (acc != 0) {
                        isHit = true;
                    }
                    intList.add(acc);
                }
            }

            if (!isHit) {
                continue;
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

            Float[] rtArray = new Float[rtList.size()];
            Float[] intArray = new Float[intList.size()];
            rtList.toArray(rtArray);
            intList.toArray(intArray);
            dataDO.setRtArray(rtArray);
            dataDO.setIntensityArray(intArray);
            dataDO.setIsHit(true);

            dataDO.setOverviewId(overviewId);
            dataDO.setAnnotations(ms.getAnnotations());
            dataDO.setCutInfo(ms.getCutInfo());
            dataDO.setPeptideRef(ms.getPeptideRef());
            dataDO.setProteinName(ms.getProteinName());
            dataDO.setIsDecoy(ms.getIsDecoy());
            dataList.add(dataDO);

            //每隔1000条数据落库一次,以减少对内存的依赖
            logCountForMSTarget++;
            if (logCountForMSTarget % 10000 == 0) {
                taskDO.addLog("已扫描MS目标:" + logCountForMSTarget + "条,累计耗时:" + (System.currentTimeMillis() - start));
                taskService.update(taskDO);
                analyseDataDAO.insert(dataList);
                dataList.clear();
            }
        }
        analyseDataDAO.insert(dataList);
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
}
