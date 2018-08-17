package com.westlake.air.pecs.async;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.dao.AnalyseDataDAO;
import com.westlake.air.pecs.dao.AnalyseOverviewDAO;
import com.westlake.air.pecs.dao.ExperimentDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.db.*;
import com.westlake.air.pecs.domain.db.simple.SimpleScanIndex;
import com.westlake.air.pecs.domain.db.simple.TargetTransition;
import com.westlake.air.pecs.domain.query.ScanIndexQuery;
import com.westlake.air.pecs.parser.BaseExpParser;
import com.westlake.air.pecs.parser.MzMLParser;
import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.service.*;
import com.westlake.air.pecs.service.impl.ExperimentServiceImpl;
import com.westlake.air.pecs.utils.ConvolutionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-17 10:40
 */
@Component("experimentTask")
public class ExperimentTask {

    public final Logger logger = LoggerFactory.getLogger(ExperimentTask.class);

    @Autowired
    MzXMLParser mzXMLParser;
    @Autowired
    MzMLParser mzMLParser;
    @Autowired
    TaskService taskService;
    @Autowired
    ScanIndexService scanIndexService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    AnalyseDataDAO analyseDataDAO;
    @Autowired
    ExperimentDAO experimentDAO;
    @Autowired
    AnalyseOverviewDAO analyseOverviewDAO;
    @Autowired
    TransitionService transitionService;
    @Autowired
    LibraryService libraryService;

    @Async
    public void saveExperimentTask(ExperimentDO experimentDO, File file, TaskDO taskDO) {
        try {
            List<ScanIndexDO> indexList = null;
            //传入不同的文件类型会调用不同的解析层
            if (experimentDO.getFileType().equals(Constants.EXP_SUFFIX_MZXML)) {
                indexList = mzXMLParser.index(file, experimentDO.getId(), taskDO);
            } else if (experimentDO.getFileType().equals(Constants.EXP_SUFFIX_MZML)) {
                indexList = mzMLParser.index(file, experimentDO.getId(), taskDO);
            }

            taskDO.setCurrentStep(2);
            taskDO.addLog("索引构建完毕,开始存储索引");
            taskService.update(taskDO);

            ResultDO resultDO = scanIndexService.insertAll(indexList, true);

            taskDO.setCurrentStep(3);
            if (resultDO.isFailed()) {
                taskDO.addLog("索引存储失败" + resultDO.getMsgInfo());
                taskDO.finish(TaskDO.STATUS_FAILED);
                taskService.update(taskDO);
                experimentService.delete(experimentDO.getId());
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

    /**
     * @param experimentDO
     * @param creator
     * @param rtExtractWindow
     * @param mzExtractWindow
     * @param buildType       0:解压缩MS1和MS2; 1:解压缩MS1; 2:解压缩MS2
     * @return
     */
    @Async
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

    public void extractMS1(RandomAccessFile raf, ExperimentDO exp, String overviewId, String libraryId, float rtExtractWindow, float mzExtractWindow, TaskDO taskDO) {

        //Step1.获取标准库目标卷积片段
        taskDO.setCurrentStep(2);
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
        taskDO.setCurrentStep(3);
        taskDO.addLog("获取全部MS1谱图索引列表");
        taskService.update(taskDO);
        List<SimpleScanIndex> indexes = scanIndexService.getSimpleAll(new ScanIndexQuery(exp.getId(), 1));

        //Step4.提取指定原始谱图
        taskDO.setCurrentStep(4);
        taskDO.addLog("解析MS1指定谱图");
        taskService.update(taskDO);
        TreeMap<Float, MzIntensityPairs> rtMap = parseSpectrum(raf, indexes, getParser(exp.getFileType()), taskDO);

        //Step5.卷积并且存储数据
        taskDO.setCurrentStep(5);
        taskDO.addLog("开始卷积MS1数据");
        taskService.update(taskDO);
        convoluteAndInsert(coordinates, rtMap, overviewId, rtExtractWindow, mzExtractWindow, true, taskDO);
    }

    public void extractMS2(File file, ExperimentDO exp, String overviewId, String libraryId, float rtExtractWindow, float mzExtractWindow, TaskDO taskDO) {

        //Step1.获取窗口信息.
        taskDO.setCurrentStep(6);
        taskDO.addLog("获取Swath窗口信息");
        taskService.update(taskDO);
        List<WindowRang> rangs = experimentService.getWindows(exp.getId());

        //按窗口开始扫描.如果一共有N个窗口,则一共分N个批次进行扫描卷积
        taskDO.setCurrentStep(7);
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
        taskDO.addLog("解析"+indexes.size()+"条XML谱图文件总计耗时:" + (System.currentTimeMillis() - start));
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
