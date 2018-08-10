package com.westlake.air.pecs.service.impl;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.dao.AnalyseDataDAO;
import com.westlake.air.pecs.dao.AnalyseOverviewDAO;
import com.westlake.air.pecs.dao.ExperimentDAO;
import com.westlake.air.pecs.dao.ScanIndexDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.MzIntensityPairs;
import com.westlake.air.pecs.domain.db.simple.SimpleScanIndex;
import com.westlake.air.pecs.domain.db.simple.TargetTransition;
import com.westlake.air.pecs.domain.bean.WindowRang;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.domain.query.AnalyseDataQuery;
import com.westlake.air.pecs.domain.query.AnalyseOverviewQuery;
import com.westlake.air.pecs.domain.query.ExperimentQuery;
import com.westlake.air.pecs.domain.query.ScanIndexQuery;
import com.westlake.air.pecs.parser.BaseExpParser;
import com.westlake.air.pecs.parser.MzMLParser;
import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.TransitionService;
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
    public ResultDO extract(String expId, String creator, float rtExtractWindow, float mzExtractWindow, int buildType) {

        ResultDO resultDO = new ResultDO(true);
        //基本条件检查
        logger.info("基本条件检查开始");
        ExperimentDO experimentDO = experimentDAO.getById(expId);
        ResultDO checkResult = checkExperiment(experimentDO);
        if (checkResult.isFailed()) {
            return checkResult;
        }

        File file = (File) checkResult.getModel();
        RandomAccessFile raf = null;

        //卷积前查看之前是否已经做过卷积处理,如果做过的话先删除原有的卷积数据
//        AnalyseOverviewDO overviewOldDO = analyseOverviewDAO.getFirstByExperimentId(expId);
//        if (overviewOldDO != null) {
//            logger.info("发现已有的卷积数据,原有卷积数据卷积日期为" + overviewOldDO.getCreateDate() + ";正在删除中");
//            analyseDataDAO.deleteAllByOverviewId(overviewOldDO.getId());
//            analyseOverviewDAO.delete(overviewOldDO.getId());
//            logger.info("原有卷积数据删除完毕,开始准备创建新的卷积数据");
//        }

        //创建实验初始化概览数据
        logger.info("开始创建卷积概览");
        AnalyseOverviewDO overviewDO = new AnalyseOverviewDO();
        overviewDO.setExpId(expId);
        overviewDO.setName(experimentDO.getName() + "-" + experimentDO.getSLibraryName() + "-" + experimentDO.getVLibraryName() + "-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        overviewDO.setExpName(experimentDO.getName());
        overviewDO.setSLibraryId(experimentDO.getSLibraryId());
        overviewDO.setSLibraryName(experimentDO.getSLibraryName());
        overviewDO.setVLibraryId(experimentDO.getVLibraryId());
        overviewDO.setVLibraryName(experimentDO.getVLibraryName());
        overviewDO.setCreator(creator);
        overviewDO.setCreateDate(new Date());
        overviewDO.setRtExtractWindow(rtExtractWindow);
        overviewDO.setMzExtractWindow(mzExtractWindow);
        analyseOverviewDAO.insert(overviewDO);

        try {
            raf = new RandomAccessFile(file, "r");

            //构建卷积坐标(耗时操作)
            if (buildType == 0) {
                extractMS1(raf, experimentDO, overviewDO.getId(), rtExtractWindow, mzExtractWindow);
                extractMS2(file, experimentDO, overviewDO.getId(), rtExtractWindow, mzExtractWindow);
            } else if (buildType == 1) {
                extractMS1(raf, experimentDO, overviewDO.getId(), rtExtractWindow, mzExtractWindow);
            } else if (buildType == 2) {
                extractMS2(file, experimentDO, overviewDO.getId(), rtExtractWindow, mzExtractWindow);
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
    public void extractMS1(RandomAccessFile raf, ExperimentDO exp, String overviewId, float rtExtractWindow, float mzExtractWindow) {

        //Step1.获取标准库目标卷积片段
        List<TargetTransition> coordinates = transitionService.buildMS1Coordinates(exp.getSLibraryId(), rtExtractWindow);
        if (coordinates == null || coordinates.size() == 0) {
            logger.error("标准库目标为空");
            return;
        }
        //Step2.获取指定索引列表
        List<SimpleScanIndex> indexes = scanIndexDAO.getSimpleAll(new ScanIndexQuery(exp.getId(), 1));
        //Step4.提取指定原始谱图
        TreeMap<Float, MzIntensityPairs> rtMap = parseSpectrum(raf, indexes, getParser(exp.getFileType()));
        //Step5.卷积并且存储数据
        convoluteAndInsert(coordinates, rtMap, overviewId, rtExtractWindow, mzExtractWindow, true);
    }

    @Override
    public void extractMS2(File file, ExperimentDO exp, String overviewId, float rtExtractWindow, float mzExtractWindow) {

        //Step1.获取窗口信息.
        List<WindowRang> rangs = getWindows(exp.getId());
        logger.info("总计有窗口:" + rangs.size() + "个");

        //按窗口开始扫描.如果一共有N个窗口,则一共分N个批次进行扫描卷积
        logger.info("开始进行MS2卷积计算");
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
                List<SimpleScanIndex> indexes = scanIndexDAO.getSimpleAll(query);
                logger.info("本批次将扫描谱图" + indexes.size() + "张");
                //Step3.获取标准库的目标肽段片段的坐标
                coordinates = transitionService.buildMS2Coordinates(exp.getSLibraryId(), rtExtractWindow, rang.getMzStart(), rang.getMzEnd());
                //Step4.提取指定原始谱图
                rtMap = parseSpectrum(raf, indexes, getParser(exp.getFileType()));

                //Step5.卷积并且存储数据
                convoluteAndInsert(coordinates, rtMap, overviewId, rtExtractWindow, mzExtractWindow, false);
                logger.info("第" + count + "数据卷积完毕,耗时:" + (System.currentTimeMillis() - start) + "毫秒");
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

    private ResultDO<File> checkExperiment(ExperimentDO experimentDO) {
        if (experimentDO == null) {
            return ResultDO.buildError(ResultCode.EXPERIMENT_NOT_EXISTED);
        }
        if (experimentDO.getFileLocation() == null || experimentDO.getFileLocation().isEmpty()) {
            return ResultDO.buildError(ResultCode.FILE_NOT_SET);
        }
        if (experimentDO.getSLibraryId() == null || experimentDO.getSLibraryId().isEmpty()) {
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

    private TreeMap<Float, MzIntensityPairs> parseSpectrum(RandomAccessFile raf, List<SimpleScanIndex> indexes, BaseExpParser baseExpParser) {
        long start = System.currentTimeMillis();
        long start2 = System.currentTimeMillis();
        logger.info("开始创建MS图谱");
        TreeMap<Float, MzIntensityPairs> rtMap = new TreeMap<>();
        int k = 0;
        for (SimpleScanIndex index : indexes) {
            rtMap.put(index.getRt(), baseExpParser.parseOne(raf, index.getStart(), index.getEnd()));
            k++;
            if (k % 1000 == 0) {
                logger.info("解析1000个图谱耗时:" + (System.currentTimeMillis() - start2));
                start2 = System.currentTimeMillis();
            }
        }
        logger.info("解析最后" + k % 1000 + "个图谱耗时:" + (System.currentTimeMillis() - start2));
        logger.info("解析XML文件总计耗时:" + (System.currentTimeMillis() - start));
        return rtMap;
    }

    private void convoluteAndInsert(List<TargetTransition> coordinates, TreeMap<Float, MzIntensityPairs> rtMap, String overviewId, Float rtExtractWindow, Float mzExtractWindow, boolean isMS1) {
        int logCountForMSTarget = 0;
        Long start = System.currentTimeMillis();
        float mzStart = 0;
        float mzEnd = -1;
        List<AnalyseDataDO> dataList = new ArrayList<>();

        for (TargetTransition ms : coordinates) {

            //设置mz卷积窗口
            if(isMS1){
                mzStart = ms.getPrecursorMz() - mzExtractWindow / 2;
                mzEnd = ms.getPrecursorMz() + mzExtractWindow / 2;
            }else{
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
                    Float acc = accumulation(pairMzArray, pairIntensityArray, mzStart, mzEnd);
                    if(acc != 0){
                        isHit = true;
                    }
                    intList.add(acc);
                }
            }

            if(!isHit){
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
                logger.info("已扫描MS目标:" + logCountForMSTarget + "条,累计耗时:" + (System.currentTimeMillis() - start));
                analyseDataDAO.insert(dataList);
                logger.info("数据存入数据库成功");
                dataList.clear();
            }
        }
        analyseDataDAO.insert(dataList);
        logger.info("全部数据处理完毕");
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
    private Float accumulation(Float[] mzArray, Float[] intensityArray, Float mzStart, Float mzEnd) {
        Float result = 0f;
        int start = findIndex(mzArray, mzStart);
        int end = findIndex(mzArray, mzEnd) - 1;
        for (int index = start; index <= end; index++) {
            result += intensityArray[index];
        }
        return result;
    }

    // 找到从小到大排序的第一个大于目标值的索引
    private int findIndex(Float[] array, Float target) {
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

    class Task implements Callable<Integer> {
        File file;
        ExperimentDO exp;
        String overviewId;
        float rtExtractWindow;
        float mzExtractWindow;
        WindowRang rang;


        public Task(File file, ExperimentDO exp, String overviewId, float rtExtractWindow, float mzExtractWindow, WindowRang rang) {
            this.file = file;
            this.exp = exp;
            this.overviewId = overviewId;
            this.rtExtractWindow = rtExtractWindow;
            this.mzExtractWindow = mzExtractWindow;
            this.rang = rang;
        }

        @Override
        public Integer call() throws Exception {
            logger.info("开始处理线程:" + rang.getMzStart() + "-" + rang.getMzEnd());
            runMs1Extractor(file, exp, overviewId, rtExtractWindow, mzExtractWindow, rang);
            return 1;
        }

        private void runMs1Extractor(File file, ExperimentDO exp, String overviewId, float rtExtractWindow, float mzExtractWindow, WindowRang rang) {
            RandomAccessFile raf = null;
            try {
                logger.info("分线程开始进行数据卷积");
                long start = System.currentTimeMillis();
                List<TargetTransition> coordinates;
                TreeMap<Float, MzIntensityPairs> rtMap;
                raf = new RandomAccessFile(file, "r");

                //Step2.获取指定索引列表
                ScanIndexQuery query = new ScanIndexQuery(exp.getId(), 2);
                query.setPrecursorMzStart(rang.getMzStart());
                query.setPrecursorMzEnd(rang.getMzEnd());
                List<SimpleScanIndex> indexes = scanIndexDAO.getSimpleAll(query);
                logger.info("本批次将扫描谱图" + indexes.size() + "张");
                //Step3.获取标准库的目标肽段片段的坐标
                coordinates = transitionService.buildMS2Coordinates(exp.getSLibraryId(), rtExtractWindow, rang.getMzStart(), rang.getMzEnd());
                //Step4.提取指定原始谱图
                rtMap = parseSpectrum(raf, indexes, getParser(exp.getFileType()));

                //Step5.卷积并且存储数据
//                convoluteAndInsert(coordinates, rtMap, overviewId, mzExtractWindow, false);
                logger.info("数据卷积完毕,耗时:" + (System.currentTimeMillis() - start) + "毫秒");
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
    }
}
