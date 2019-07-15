package com.westlake.air.propro.algorithm.irt;

import com.westlake.air.propro.algorithm.extract.Extractor;
import com.westlake.air.propro.algorithm.feature.RtNormalizerScorer;
import com.westlake.air.propro.algorithm.fitter.LinearFitter;
import com.westlake.air.propro.algorithm.parser.AirdFileParser;
import com.westlake.air.propro.algorithm.peak.FeatureExtractor;
import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.aird.Compressor;
import com.westlake.air.propro.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.bean.irt.IrtResult;
import com.westlake.air.propro.domain.bean.score.PeptideFeature;
import com.westlake.air.propro.domain.bean.score.ScoreRtPair;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.AnalyseDataDO;
import com.westlake.air.propro.domain.db.ExperimentDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.SwathIndexDO;
import com.westlake.air.propro.domain.db.simple.TargetPeptide;
import com.westlake.air.propro.domain.query.SwathIndexQuery;
import com.westlake.air.propro.service.PeptideService;
import com.westlake.air.propro.service.ScoreService;
import com.westlake.air.propro.service.SwathIndexService;
import com.westlake.air.propro.utils.ConvolutionUtil;
import com.westlake.air.propro.utils.FileUtil;
import com.westlake.air.propro.utils.MathUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@Component("irt")
public class Irt {

    public final Logger logger = LoggerFactory.getLogger(Irt.class);

    @Autowired
    PeptideService peptideService;
    @Autowired
    AirdFileParser airdFileParser;
    @Autowired
    Extractor extractor;
    @Autowired
    ScoreService scoreService;
    @Autowired
    SwathIndexService swathIndexService;
    @Autowired
    FeatureExtractor featureExtractor;
    @Autowired
    RtNormalizerScorer rtNormalizerScorer;
    @Autowired
    LinearFitter linearFitter;

    /**
     * 卷积并且求出iRT
     *
     * @param experimentDO
     * @param library
     * @param mzExtractWindow
     * @param sigmaSpacing
     * @return
     */
    public ResultDO<IrtResult> extractAndAlign(ExperimentDO experimentDO, LibraryDO library, Float mzExtractWindow, SigmaSpacing sigmaSpacing) {
        try {
            List<AnalyseDataDO> dataList = extract(experimentDO, library, mzExtractWindow);
            if (dataList == null) {
                return ResultDO.buildError(ResultCode.IRT_EXCEPTION);
            }
            ResultDO<IrtResult> resultDO = new ResultDO<>(false);
            try {
                resultDO = align(dataList, library, sigmaSpacing);
            } catch (Exception e) {
                e.printStackTrace();
                resultDO.setMsgInfo(e.getMessage());
            }
            experimentDO.setIrtResult(resultDO.getModel());
            return resultDO;
        } catch (Exception e) {
            e.printStackTrace();
            return ResultDO.buildError(ResultCode.IRT_EXCEPTION);
        }
    }

    /**
     * 卷积iRT校准库的数据
     *
     * @param exp
     * @param library
     * @param mzExtractWindow
     * @return
     */
    private List<AnalyseDataDO> extract(ExperimentDO exp, LibraryDO library, float mzExtractWindow) {

        ResultDO checkResult = ConvolutionUtil.checkExperiment(exp);
        if (checkResult.isFailed()) {
            logger.error(checkResult.getMsgInfo());
            return null;
        }

        File file = (File) checkResult.getModel();
        RandomAccessFile raf = null;

        List<AnalyseDataDO> finalList = new ArrayList<>();

        SwathIndexQuery query = new SwathIndexQuery(exp.getId(), 2);
        List<SwathIndexDO> swathList = swathIndexService.getAll(query);

        try {
            raf = new RandomAccessFile(file, "r");
            for (SwathIndexDO swathIndexDO : swathList) {
                //Step2.获取标准库的目标肽段片段的坐标
                //key为rt
                TreeMap<Float, MzIntensityPairs> rtMap;
                List<TargetPeptide> coordinates = peptideService.buildMS2Coordinates(library, SlopeIntercept.create(), -1, swathIndexDO.getRange(), null, exp.getType(), false, true);
                if (coordinates.size() == 0) {
                    logger.warn("No iRT Coordinates Found,Rang:" + swathIndexDO.getRange().getStart() + ":" + swathIndexDO.getRange().getEnd());
                    continue;
                }
                //Step3.提取指定原始谱图
                try {
                    rtMap = airdFileParser.parseSwathBlockValues(raf, swathIndexDO, exp.fetchCompressor(Compressor.TARGET_MZ), exp.fetchCompressor(Compressor.TARGET_INTENSITY));
                } catch (Exception e) {
                    logger.error("PrecursorMZStart:" + swathIndexDO.getRange().getStart());
                    throw e;
                }

                //Step4.卷积并且存储数据,如果传入的库是标准库,那么使用采样的方式进行卷积
                if (library.getType().equals(LibraryDO.TYPE_IRT)) {
                    extractor.extractForIrt(finalList, coordinates, rtMap, null, mzExtractWindow, -1f);
                } else {
                    extractor.extractForIrtWithLib(finalList, coordinates, rtMap, null, mzExtractWindow, -1f);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtil.close(raf);
        }

        return finalList;
    }

    /**
     * 从一个卷积结果列表中求出iRT
     *
     * @param dataList
     * @param library
     * @param sigmaSpacing Sigma通常为30/8 = 6.25/Spacing通常为0.01
     * @return
     */
    private ResultDO<IrtResult> align(List<AnalyseDataDO> dataList, LibraryDO library, SigmaSpacing sigmaSpacing) throws Exception {

        List<List<ScoreRtPair>> scoreRtList = new ArrayList<>();
        List<Double> compoundRt = new ArrayList<>();
        ResultDO<IrtResult> resultDO = new ResultDO<>();
        double minGroupRt = Double.MAX_VALUE, maxGroupRt = Double.MIN_VALUE;
        for (AnalyseDataDO dataDO : dataList) {
            TargetPeptide tp = peptideService.getTargetPeptideByDataRef(library.getId(), dataDO.getPeptideRef(), dataDO.getIsDecoy());
            PeptideFeature peptideFeature = featureExtractor.getExperimentFeature(dataDO, tp.buildIntensityMap(), sigmaSpacing);
            if (!peptideFeature.isFeatureFound()) {
                continue;
            }
            double groupRt = dataDO.getRt();
            if (groupRt > maxGroupRt){
                maxGroupRt = groupRt;
            }
            if (groupRt < minGroupRt){
                minGroupRt = groupRt;
            }
            List<ScoreRtPair> scoreRtPairs = rtNormalizerScorer.score(peptideFeature.getPeakGroupList(), peptideFeature.getNormedLibIntMap(), groupRt);
            if (scoreRtPairs.size() == 0){
                continue;
            }
            scoreRtList.add(scoreRtPairs);
            compoundRt.add(groupRt);
        }

        List<Pair<Double,Double>> pairs = simpleFindBestFeature(scoreRtList, compoundRt);
        double delta = (maxGroupRt - minGroupRt)/30d;
        List<Pair<Double,Double>> pairsCorrected = chooseReliablePairs(pairs, delta);

        System.out.println("choose finish ------------------------");
        IrtResult irtResult = new IrtResult();

        List<Double[]> selectedList = new ArrayList<>();
        List<Double[]> unselectedList = new ArrayList<>();
        for (int i = 0; i < pairs.size(); i++) {
            if(pairsCorrected.contains(pairs.get(i))){
                selectedList.add(new Double[]{pairs.get(i).getLeft(),pairs.get(i).getRight()});
            }else{
                unselectedList.add(new Double[]{pairs.get(i).getLeft(),pairs.get(i).getRight()});
            }
        }
        irtResult.setSelectedPairs(selectedList);
        irtResult.setUnselectedPairs(unselectedList);
        SlopeIntercept slopeIntercept = linearFitter.proproFit(pairsCorrected, delta);
        irtResult.setSi(slopeIntercept);
        resultDO.setSuccess(true);
        resultDO.setModel(irtResult);

        return resultDO;
    }

    /**
     * get rt pairs for every peptideRef
     *
     * @param scoresList peptideRef list of List<ScoreRtPair>
     * @param rt         get from groupsResult.getModel()
     * @return rt pairs
     */
    private List<Pair<Double,Double>> simpleFindBestFeature(List<List<ScoreRtPair>> scoresList, List<Double> rt) {

        List<Pair<Double,Double>> pairs = new ArrayList<>();

        for (int i = 0; i < scoresList.size(); i++) {
            List<ScoreRtPair> scores = scoresList.get(i);
            double max = Double.MIN_VALUE;
            //find max scoreForAll's rt
            double expRt = 0d;
            for (int j = 0; j < scores.size(); j++) {
                if (scores.get(j).getScore() > max) {
                    max = scores.get(j).getScore();
                    expRt = scores.get(j).getRt();
                }
            }
            if (Constants.ESTIMATE_BEST_PEPTIDES && max < Constants.OVERALL_QUALITY_CUTOFF) {
                continue;
            }
            Pair<Double,Double> rtPair = Pair.of(rt.get(i), expRt);
            pairs.add(rtPair);
        }
        return pairs;
    }

    private List<Pair<Double,Double>> chooseReliablePairs(List<Pair<Double,Double>> rtPairs, double delta) throws Exception {
        SlopeIntercept slopeIntercept = linearFitter.huberFit(rtPairs, delta);
        TreeMap<Double, Pair<Double,Double>> errorMap = new TreeMap<>();
        for (Pair<Double, Double> pair: rtPairs){
            errorMap.put(Math.abs(pair.getRight() * slopeIntercept.getSlope() + slopeIntercept.getIntercept() - pair.getLeft()), pair);
        }
        List<Pair<Double,Double>> sortedPairs = new ArrayList<>(errorMap.values());
        int cutLine = 2;
        for (int i = sortedPairs.size(); i > 2; i--){
            if (MathUtil.getRsq(sortedPairs.subList(0,i)) >= 0.95){
                cutLine = i;
                break;
            }
        }
        return sortedPairs.subList(0,cutLine);
    }
}
