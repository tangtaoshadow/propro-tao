package com.westlake.air.propro.algorithm.extract;

import com.alibaba.fastjson.JSON;
import com.westlake.air.propro.algorithm.parser.AirdFileParser;
import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.dao.ProjectDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.aird.Compressor;
import com.westlake.air.propro.domain.bean.aird.WindowRange;
import com.westlake.air.propro.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.propro.domain.bean.compressor.AircInfo;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.domain.db.simple.TargetPeptide;
import com.westlake.air.propro.domain.params.LumsParams;
import com.westlake.air.propro.domain.query.SwathIndexQuery;
import com.westlake.air.propro.service.*;
import com.westlake.air.propro.utils.AnalyseUtil;
import com.westlake.air.propro.utils.ByteUtil;
import com.westlake.air.propro.utils.ConvolutionUtil;
import com.westlake.air.propro.utils.FileUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

@Component("extractor")
public class Extractor {

    public final Logger logger = LoggerFactory.getLogger(Extractor.class);

    @Autowired
    AirdFileParser airdFileParser;
    @Autowired
    LibraryService libraryService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    ProjectDAO projectDAO;
    @Autowired
    SwathIndexService swathIndexService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    PeptideService peptideService;
    @Autowired
    ScoreService scoreService;

    /**
     * 卷积的核心函数,最终返回卷积到的Peptide数目
     * 目前只支持MS2的卷积
     *
     * @param lumsParams useEpps: true: 将卷积,选峰及打分合并在一个步骤中执行,可以完整的省去一次IO读取及解析,大大提升分析速度
     *                   需要experimentDO,libraryId,rtExtractionWindow,mzExtractionWindow,SlopeIntercept
     * @return
     */
    public ResultDO<AnalyseOverviewDO> extract(LumsParams lumsParams) {
        ResultDO<AnalyseOverviewDO> resultDO = new ResultDO(true);
        logger.info("基本条件检查开始");
        ResultDO checkResult = ConvolutionUtil.checkExperiment(lumsParams.getExperimentDO());
        if (checkResult.isFailed()) {
            logger.error("条件检查失败:" + checkResult.getMsgInfo());
            return checkResult;
        }

        AnalyseOverviewDO overviewDO = createOverview(lumsParams);

        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile((File) checkResult.getModel(), "r");
            //核心函数在这里
            extract(raf, overviewDO, lumsParams);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtil.close(raf);
        }

        analyseOverviewService.update(overviewDO);
        resultDO.setModel(overviewDO);
        return resultDO;
    }

    /**
     * 实时卷积某一个PeptideRef的图谱,卷积的rtWindows默认为-1,即全时间段卷积
     *
     * @param exp
     * @param peptide
     * @return
     */
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
            SwathIndexQuery query = new SwathIndexQuery(exp.getId(), 2);
            query.setMz(peptide.getMz().floatValue());
            SwathIndexDO swathIndexDO = swathIndexService.getSwathIndex(exp.getId(), peptide.getMz().floatValue());
            //Step2.获取该窗口内的谱图Map,key值代表了RT
            TreeMap<Float, MzIntensityPairs> rtMap;
            try {
                rtMap = airdFileParser.parseSwathBlockValues(raf, swathIndexDO, exp.fetchCompressor(Compressor.TARGET_MZ), exp.fetchCompressor(Compressor.TARGET_INTENSITY));
            } catch (Exception e) {
                logger.error("PrecursorMZ:" + swathIndexDO.getRange().getMz());
                throw e;
            }

            TargetPeptide tp = new TargetPeptide(peptide);
            Double rt = peptide.getRt();
            if (rtExtractorWindow == -1) {
                tp.setRtStart(-1);
                tp.setRtEnd(99999);
            } else {
                Double targetRt = (rt - exp.getIrtResult().getSi().getIntercept()) / exp.getIrtResult().getSi().getSlope();
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
    public void extractForIrt(List<AnalyseDataDO> finalList, List<TargetPeptide> coordinates, TreeMap<Float, MzIntensityPairs> rtMap, String overviewId, Float mzExtractWindow, Float rtExtractWindow) {
        for (TargetPeptide tp : coordinates) {
            AnalyseDataDO dataDO = extractForOne(tp, rtMap, mzExtractWindow, rtExtractWindow, overviewId);
            if (dataDO == null) {
                continue;
            }
            finalList.add(dataDO);
        }
    }

    private AnalyseDataDO extractForOne(TargetPeptide tp, TreeMap<Float, MzIntensityPairs> rtMap, Float mzExtractWindow, Float rtExtractWindow, String overviewId) {
        float mzStart = 0;
        float mzEnd = -1;
        boolean useAdaptiveWindow = false;
        if (mzExtractWindow == -1) {
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
                if (useAdaptiveWindow) {
                    acc = ConvolutionUtil.adaptiveAccumulation(pairMzArray, pairIntensityArray, fi.getMz().floatValue());
                } else {
                    acc = ConvolutionUtil.accumulation(pairMzArray, pairIntensityArray, mzStart, mzEnd);
                }
                if (acc != 0) {
                    isAllZero = false;
                }
                intArray[i] = acc;
            }
            if (isAllZero) {
                continue;
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
     * 卷积MS2图谱并且输出最终结果,不返回最终的卷积结果以减少内存的使用
     *
     * @param raf        用于读取Aird文件
     * @param overviewDO
     * @param lumsParams
     */
    private void extract(RandomAccessFile raf, AnalyseOverviewDO overviewDO, LumsParams lumsParams) {

        //Step1.获取窗口信息.
        logger.info("获取Swath窗口信息");
        List<WindowRange> rangs = lumsParams.getExperimentDO().getWindowRanges();
        SwathIndexQuery query = new SwathIndexQuery(lumsParams.getExperimentDO().getId(), 2);

        //获取所有MS2的窗口
        List<SwathIndexDO> swathIndexList = swathIndexService.getAll(query);
        HashMap<Float, Float[]> rtRangeMap = null;
        if (lumsParams.getExperimentDO().getType().equals(Constants.EXP_TYPE_PRM)) {
            rtRangeMap = experimentService.getPrmRtWindowMap(swathIndexList);
            lumsParams.setRtRangeMap(rtRangeMap);
        }

        //按窗口开始扫描.如果一共有N个窗口,则一共分N个批次进行扫描卷积
        logger.info("总计有窗口:" + rangs.size() + "个,开始进行MS2卷积计算");
        int count = 1;
        try {
            long peakCount = 0L;
            int dataCount = 0;
            for (SwathIndexDO index : swathIndexList) {
                long start = System.currentTimeMillis();
                List<AnalyseDataDO> dataList = doExtract(raf, index, overviewDO.getId(), lumsParams);
                if (dataList != null) {
                    for (AnalyseDataDO dataDO : dataList) {
                        peakCount += dataDO.getFeatureScoresList().size();
                    }
                    dataCount += dataList.size();
                }
                analyseDataService.insertAll(dataList, false);
                logger.info("第" + count + "轮数据卷积完毕,有效肽段:" + (dataList == null ? 0 : dataList.size()) + "个,耗时:" + (System.currentTimeMillis() - start) + "毫秒");
                count++;
            }

            overviewDO.setTotalPeptideCount(dataCount);
            overviewDO.setPeakCount(peakCount);
            analyseOverviewService.update(overviewDO);

        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    /**
     * 返回卷积到的数目
     *
     * @param raf
     * @param lumsParams
     * @param swathIndex
     * @param overviewId
     * @return
     * @throws Exception
     */
    private List<AnalyseDataDO> doExtract(RandomAccessFile raf, SwathIndexDO swathIndex, String overviewId, LumsParams lumsParams) throws Exception {
        List<TargetPeptide> coordinates;
        TreeMap<Float, MzIntensityPairs> rtMap;
        //Step2.获取标准库的目标肽段片段的坐标
        Float[] rtRange = null;
        if (lumsParams.getRtRangeMap() != null) {
            float precursorMz = swathIndex.getRange().getMz();
            rtRange = lumsParams.getRtRangeMap().get(precursorMz);
        }
        ExperimentDO exp = lumsParams.getExperimentDO();
        coordinates = peptideService.buildMS2Coordinates(lumsParams.getLibrary().getId(), lumsParams.getSlopeIntercept(), lumsParams.getRtExtractWindow(), swathIndex.getRange(), rtRange, exp.getType(), lumsParams.isUniqueOnly());
        if (coordinates.isEmpty()) {
            logger.warn("No Coordinates Found,Rang:" + swathIndex.getRange().getStart() + ":" + swathIndex.getRange().getEnd());
            return null;
        }
        if (coordinates.size() != 2) {
            logger.warn("coordinate size != 2,Rang:" + swathIndex.getRange().getStart() + ":" + swathIndex.getRange().getEnd());
        }
        //Step3.提取指定原始谱图
        long start = System.currentTimeMillis();

        rtMap = airdFileParser.parseSwathBlockValues(raf, swathIndex, exp.fetchCompressor(Compressor.TARGET_MZ), exp.fetchCompressor(Compressor.TARGET_INTENSITY));

        logger.info("IO及解码耗时:" + (System.currentTimeMillis() - start));
        return epps(coordinates, rtMap, overviewId, lumsParams);

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
                logger.info("未卷积到任何片段,PeptideRef:" + tp.getPeptideRef());
                continue;
            }
//            if (dataDO.getRtArray().length < 20){
//                logger.info("卷积谱图时间太短,PeptideRef:" + tp.getPeptideRef());
//                continue;
//            }
            //Step2. 常规选峰及打分
            scoreService.scoreForOne(dataDO, tp, rtMap, lumsParams);
            if (dataDO.getFeatureScoresList() == null) {
                //logger.info("未满足基础条件,直接忽略:"+dataDO.getPeptideRef());
                targetIgnorePeptides.add(dataDO.getPeptideRef());
                continue;
            }
            AnalyseUtil.compress(dataDO);
            dataList.add(dataDO);
        }
        for (TargetPeptide tp : decoyList) {
            //如果伪肽段在忽略列表里面,那么直接忽略
            if (targetIgnorePeptides.contains(tp.getPeptideRef()) && !lumsParams.getExperimentDO().getType().equals(Constants.EXP_TYPE_PRM)) {
                continue;
            }
            //Step1. 常规卷积,卷积结果不进行压缩处理,因为后续会立即进行子分数打分,等到打分结束以后再进行压缩
            AnalyseDataDO dataDO = extractForOne(tp, rtMap, lumsParams.getMzExtractWindow(), lumsParams.getRtExtractWindow(), overviewId);
            if (dataDO == null) {
                continue;
            }
            //Step2. 常规选峰及打分
            scoreService.scoreForOne(dataDO, tp, rtMap, lumsParams);
            if (dataDO.getFeatureScoresList() == null) {
                continue;
            }
            AnalyseUtil.compress(dataDO);
            dataList.add(dataDO);
        }
        logger.info("卷积+选峰+打分耗时:" + (System.currentTimeMillis() - start));
        return dataList;
    }

    /**
     * 根据input入参初始化一个AnalyseOverviewDO
     *
     * @param input
     * @return
     */
    public AnalyseOverviewDO createOverview(LumsParams input) {
        AnalyseOverviewDO overviewDO = new AnalyseOverviewDO();
        overviewDO.setExpId(input.getExperimentDO().getId());
        overviewDO.setExpName(input.getExperimentDO().getName());
        overviewDO.setType(input.getExperimentDO().getType());
        overviewDO.setScoreTypes(input.getScoreTypes()); //存储打分分数类型的快照
        overviewDO.setLibraryId(input.getLibrary().getId());
        overviewDO.setLibraryName(input.getLibrary().getName());
        overviewDO.setLibraryPeptideCount(input.getLibrary().getTotalCount().intValue());
        overviewDO.setName(input.getExperimentDO().getName() + "-" + input.getLibrary().getName() + "-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        overviewDO.setOwnerName(input.getOwnerName());
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

        analyseOverviewService.insert(overviewDO);
        input.setOverviewId(overviewDO.getId());
        return overviewDO;
    }
}
