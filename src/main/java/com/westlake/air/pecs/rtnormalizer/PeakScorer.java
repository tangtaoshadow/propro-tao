package com.westlake.air.pecs.rtnormalizer;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.westlake.air.pecs.dao.AminoAcidDAO;
import com.westlake.air.pecs.dao.UnimodDAO;
import com.westlake.air.pecs.domain.bean.score.RTNormalizationScores;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import com.westlake.air.pecs.domain.bean.score.ScoreRtPair;
import com.westlake.air.pecs.domain.bean.score.ExperimentFeature;
import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.dao.ElementsDAO;
import com.westlake.air.pecs.domain.bean.*;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.bean.transition.Annotation;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.simple.IntensityGroup;
import com.westlake.air.pecs.domain.db.simple.TransitionGroup;
import com.westlake.air.pecs.parser.model.chemistry.AminoAcid;
import com.westlake.air.pecs.parser.model.chemistry.Element;
import com.westlake.air.pecs.parser.model.chemistry.Unimod;
import com.westlake.air.pecs.utils.MathUtil;
import org.omg.PortableServer.ServantLocatorPOA;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-05 17:18
 */
@Component("peakScorer")
public class PeakScorer {

//    private float windowLength = 1000;
//    private int binCount = 30;

//    private float rtNormalizationFactor = 1.0f;
//    private int addUpSpectra = 1;
//    private float spacingForSpectraResampling = 0.005f;
//    //score use params


    /**
     *        return scores.library_corr                     * -0.34664267 +
     *               scores.library_norm_manhattan           *  2.98700722 +
     *               scores.norm_rt_score                    *  7.05496384 +
     *               scores.xcorr_coelution_score            *  0.09445371 +
     *               scores.xcorr_shape_score                * -5.71823862 +
     *               scores.log_sn_score                     * -0.72989582 +
     *               scores.elution_model_fit_score          *  1.88443209;
     * @param chromatograms chromatogramList in transitionGroup
     * @param experimentFeatures features extracted from chromatogramList in transitionGroup
     * @param libraryIntensity intensity in transitionList in transitionGroup
     * @return List of overallQuality
     */
    public List<ScoreRtPair> score(List<RtIntensityPairs> chromatograms, List<List<ExperimentFeature>> experimentFeatures, List<Float> libraryIntensity, SlopeIntercept slopeIntercept, float groupRt, float windowLength, int binCount){

        //get signal to noise list
        List<float[]> signalToNoiseList = new ArrayList<>();
        for(RtIntensityPairs chromatogram: chromatograms) {
            float[] signalToNoise = new SignalToNoiseEstimator().computeSTN(chromatogram, windowLength, binCount);
            signalToNoiseList.add(signalToNoise);
        }

        List<ScoreRtPair> finalScores = new ArrayList<>();
        for(List<ExperimentFeature> features: experimentFeatures){
            RTNormalizationScores scores = new RTNormalizationScores();
            calculateChromatographicScores(chromatograms, features, libraryIntensity, signalToNoiseList, scores);
            calculateLibraryScores(features,libraryIntensity, scores, slopeIntercept, groupRt);
            float ldaScore = calculateLdaPrescore(scores);
            ScoreRtPair scoreRtPair = new ScoreRtPair();
            scoreRtPair.setRt(features.get(0).getRt());
            scoreRtPair.setScore(ldaScore);
            finalScores.add(scoreRtPair);
        }

        return finalScores;
    }




    /**
     * scores.xcorr_coelution_score
     * scores.xcorr_shape_score
     * scores.log_sn_score
     * @param chromatograms chromatogram list of transition group
     * @param experimentFeatures list of features in selected mrmfeature
     * @param signalToNoiseList signal to noise list of chromatogram list
     */
    private void calculateChromatographicScores(List<RtIntensityPairs> chromatograms, List<ExperimentFeature> experimentFeatures, List<Float> libraryIntensity, List<float[]> signalToNoiseList, RTNormalizationScores scores){
        Table<Integer, Integer, Float[]> xcorrMatrix = initializeXCorrMatrix(experimentFeatures);

        //xcorrCoelutionScore
        //xcorrCoelutionScoreWeighted
        //xcorrShapeScore
        //xcorrShapeScoreWeighted
        float[] normalizedLibraryIntensity = normalizeSum(libraryIntensity);
        List<Integer> deltas = new ArrayList<>();
        List<Float> deltasWeighted = new ArrayList<>();
        List<Float> intensities = new ArrayList<>();
        List<Float> intensitiesWeighted = new ArrayList<>();
        Float[] value;
        int max;
        for(int i = 0; i<experimentFeatures.size(); i++){
            value = xcorrMatrix.get(i, i);
            max = MathUtil.findMaxIndex(value);
            deltasWeighted.add(Math.abs(max - (value.length -1)/2) * normalizedLibraryIntensity[i] * normalizedLibraryIntensity[i]);
            intensities.add(value[max] * normalizedLibraryIntensity[i] * normalizedLibraryIntensity[i]);
            for (int j = i; j<experimentFeatures.size(); j++){
                value = xcorrMatrix.get(i, j);
                max = MathUtil.findMaxIndex(value);
                deltas.add(Math.abs(max - (value.length -1)/2)); //first: maxdelay
                intensities.add(value[max]);
                if(j !=i){
                    deltasWeighted.add(Math.abs(max - (value.length -1)/2) * normalizedLibraryIntensity[i] * normalizedLibraryIntensity[j] * 2f);
                    intensitiesWeighted.add(value[max] * normalizedLibraryIntensity[i] * normalizedLibraryIntensity[j] * 2f);
                }
            }
        }
        float sumDelta = 0.0f, sumDeltaWeighted = 0.0f, sumIntensity = 0.0f, sumIntensityWeighted = 0.0f;
        for(int i=0; i<deltas.size(); i++){
            sumDelta += deltas.get(i);
            sumDeltaWeighted += deltasWeighted.get(i);
            sumIntensity += intensities.get(i);
            sumIntensityWeighted += intensitiesWeighted.get(i);
        }
        float meanDelta = sumDelta / deltas.size();
        float meanIntensity = sumIntensity / intensities.size();
        sumDelta = 0;
        for(int delta: deltas){
            sumDelta += (delta - meanDelta) * (delta - meanDelta);
        }
        float stdDelta = (float) Math.sqrt(sumDelta / (deltas.size()-1));
        scores.setVarXcorrCoelution(meanDelta + stdDelta);
        scores.setVarXcorrCoelutionWeighted(sumDeltaWeighted);
        scores.setVarXcorrShape(meanIntensity);
        scores.setVarXcorrShapeWeighted(sumIntensityWeighted);

        //logSnScore
        float rt;
        int leftIndex, rightIndex;
        float snScore = 0.0f;
        if(signalToNoiseList.size() == 0){
            snScore = 0.0f;
        }
        for(int k = 0; k<signalToNoiseList.size();k++){
            rt = experimentFeatures.get(0).getRt();
            leftIndex = MathUtil.bisection(chromatograms.get(k), rt);
            rightIndex = leftIndex + 1;
            if(Math.abs(chromatograms.get(k).getRtArray()[leftIndex] - rt) < Math.abs(chromatograms.get(k).getRtArray()[rightIndex] - rt)){
                snScore += signalToNoiseList.get(k)[leftIndex];
            }else {
                snScore += signalToNoiseList.get(k)[rightIndex];
            }
        }
        snScore /= signalToNoiseList.size();
        if(snScore < 1){
            scores.setVarLogSnScore(0);
        }else {
            scores.setVarLogSnScore((float)Math.log(snScore));
        }
    }




    /**
     * scores.library_corr
     * scores.library_norm_manhattan
     * @param experimentFeatures get experimentIntensity: from features extracted
     * @param libraryIntensity get libraryIntensity: from transitions
     * @param scores library_corr, library_norm_manhattan
     */
    private void calculateLibraryScores(List<ExperimentFeature> experimentFeatures, List<Float> libraryIntensity, RTNormalizationScores scores, SlopeIntercept slopeIntercept, float groupRt){
        List<Float> experimentIntensity = new ArrayList<>();
        for(ExperimentFeature experimentFeature: experimentFeatures){
            experimentIntensity.add(experimentFeature.getIntensity());
        }
        // experimentIntensity, libraryIntensity same size
        //library_norm_manhattan
        float sum = 0.0f;
        float[] x = normalizeSum(libraryIntensity);
        float[] y = normalizeSum(experimentIntensity);
        for(int i=0; i<x.length; i++){
            sum += Math.abs(x[i] - y[i]);
        }
        scores.setVarLibraryRsmd(sum / x.length);

        //library_corr
        float corr = 0.0f, m1 = 0.0f, m2 = 0.0f, s1 = 0.0f, s2 = 0.0f;
        for(int i=0;i<libraryIntensity.size(); i++){
            corr += experimentIntensity.get(i) * libraryIntensity.get(i);
            m1 += experimentIntensity.get(i);
            m2 += libraryIntensity.get(i);
            s1 += experimentIntensity.get(i) * experimentIntensity.get(i);
            s2 += libraryIntensity.get(i) * libraryIntensity.get(i);
        }
        m1 /= libraryIntensity.size();
        m2 /= libraryIntensity.size();
        s1 -= m1 * m1 * libraryIntensity.size();
        s2 -= m2 * m2 * libraryIntensity.size();
        if(s1 < Math.pow(1,-12) || s2 < Math.pow(1,-12)){
            scores.setVarLibraryCorr(0.0f);
        }else {
            corr -= m1 * m2 * libraryIntensity.size();
            corr /= Math.sqrt(s1 * s2);
            scores.setVarLibraryCorr(corr);
        }

        //varNormRtScore
        float experimentalRt = experimentFeatures.get(0).getRt();
        float normalizedExperimentalRt = MathUtil.trafoApplier(slopeIntercept, experimentalRt);
        if(groupRt <= -1000f){
            scores.setVarNormRtScore(0);
        }else {
            scores.setVarNormRtScore(Math.abs(normalizedExperimentalRt - groupRt));
        }
    }

    private void calculateIntensityScore(List<ExperimentFeature> experimentFeatures, RTNormalizationScores scores){
        float intensitySum = 0.0f;
        for(ExperimentFeature feature: experimentFeatures){
            intensitySum += feature.getIntensity();
        }
        float totalXic = experimentFeatures.get(0).getTotalXic();
        scores.setVarIntensityScore(intensitySum / totalXic);
    }

    /**
     *
     * @param productMzArray 根据transitionGroup获得存在transition中的productMz，存成Float array
     * @param spectrumMzArray 根据transitionGroup选取的RT选择的最近的Spectrum对应的mzArray
     * @param spectrumIntArray 根据transitionGroup选取的RT选择的最近的Spectrum对应的intensityArray
     * @param libraryIntensity unNormalized library intensity(in transition)
     * @param scores score for JProphet
     */
    private void calculateDiaMassDiffScore(Float[] productMzArray, Float[] spectrumMzArray, Float[] spectrumIntArray, List<Float> libraryIntensity, RTNormalizationScores scores){

        float ppmScore = 0.0f;
        float ppmScoreWeighted = 0.0f;
        for(int i=0; i< productMzArray.length; i++){
            float left = productMzArray[i] - Constants.DIA_EXTRACT_WINDOW / 2f;
            float right = productMzArray[i] + Constants.DIA_EXTRACT_WINDOW/ 2f;

            //integrate window
            float mz = 0f, intensity = 0f;
            int leftIndex = MathUtil.bisection(spectrumMzArray, left);
            int rightIndex = MathUtil.bisection(spectrumMzArray, right);
            if(spectrumMzArray[0] < left){
                leftIndex ++;
            }
            if(spectrumMzArray[spectrumMzArray.length-1] < right){
                rightIndex ++;
            }
            for(int index = leftIndex; index <=rightIndex; index ++){
                intensity += spectrumIntArray[index];
                mz += spectrumMzArray[index] * spectrumIntArray[index];
            }
            if(intensity > 0f){
                mz /= intensity;
                float diffPpm = Math.abs(mz - productMzArray[i]) * 1000000f / productMzArray[i];
                ppmScore += diffPpm;
                ppmScoreWeighted += diffPpm * libraryIntensity.get(i);
            }
        }
        scores.setVarMassdevScore(ppmScore);
        scores.setVarMassdevScoreWeighted(ppmScoreWeighted);
    }


    private void calculateDiaIsotopeScores(List<ExperimentFeature> experimentFeatures, Float[] productMzArray, Float[] spectrumMzArray, Float[] spectrumIntArray, List<Integer> productCharge, RTNormalizationScores scores){
        float isotopeCorr = 0f;
        float isotopeOverlap = 0f;

        //getFirstIsotopeRelativeIntensities
        float relIntensity;
        float intensitySum = 0.0f;
        for(ExperimentFeature feature: experimentFeatures){
            intensitySum += feature.getIntensity();
        }

        for(int i=0; i<experimentFeatures.size()-1; i++){
            relIntensity = experimentFeatures.get(i).getIntensity() / intensitySum;
            int putativeFragmentCharge = 1;
            if(productCharge.get(i) > 0){
                putativeFragmentCharge = productCharge.get(i);
            }
            List<Float> isotopesIntList = new ArrayList<>();
            float maxIntensity = 0.0f;
            for(int iso=0; iso<=Constants.DIA_NR_ISOTOPES; iso++){
                float left = productMzArray[i] + iso * Constants.C13C12_MASSDIFF_U / putativeFragmentCharge;
                float right = left;
                left -= Constants.DIA_EXTRACT_WINDOW / 2f;
                right += Constants.DIA_EXTRACT_WINDOW/ 2f;

                //integrate window
                float mz = 0f, intensity = 0f;
                int leftIndex = MathUtil.bisection(spectrumMzArray, left);
                int rightIndex = MathUtil.bisection(spectrumMzArray, right);
                //TODO: bisection series
                if(spectrumMzArray[0] < left){
                    leftIndex ++;
                }
                if(spectrumMzArray[spectrumMzArray.length-1] < right){
                    rightIndex ++;
                }
                for(int index = leftIndex; index <=rightIndex; index ++){
                    intensity += spectrumIntArray[index];
                    mz += spectrumMzArray[index] * spectrumIntArray[index];
                }
                if(intensity > 0f) {
                    mz /= intensity;
                }else {
                    mz = -1;
                    intensity = 0;
                }
                if(intensity > maxIntensity){
                    maxIntensity = intensity;
                }
                isotopesIntList.add(intensity);
            }

            //get isotopeCorr
            List<Float> isotopeDistList;
            float averageWeight = Math.abs(productMzArray[i] * putativeFragmentCharge);
            float averageWeightC = (float)new ElementsDAO().getElementBySymbol(Element.C).getAverageWeight();
            float averageWeightH = (float)new ElementsDAO().getElementBySymbol(Element.H).getAverageWeight();
            float averageWeightN = (float)new ElementsDAO().getElementBySymbol(Element.N).getAverageWeight();
            float averageWeightO = (float)new ElementsDAO().getElementBySymbol(Element.O).getAverageWeight();
            float averageWeightS = (float)new ElementsDAO().getElementBySymbol(Element.S).getAverageWeight();
            float averageWeightP = (float)new ElementsDAO().getElementBySymbol(Element.P).getAverageWeight();
            float avgTotal = Constants.C * averageWeightC +
                    Constants.H * averageWeightH +
                    Constants.N * averageWeightN +
                    Constants.O * averageWeightO +
                    Constants.S * averageWeightS +
                    Constants.P * averageWeightP;
            float factor = averageWeight / avgTotal;
            HashMap<String, Integer> formula = new HashMap<>();
            formula.put("C", Math.round(Constants.C * factor));
            formula.put("N", Math.round(Constants.N * factor));
            formula.put("O", Math.round(Constants.O * factor));
            formula.put("S", Math.round(Constants.S * factor));
            formula.put("P", Math.round(Constants.P * factor));

            float getAverageWeight = averageWeightC * formula.get("C") +
                    averageWeightN * formula.get("N") +
                    averageWeightO * formula.get("O") +
                    averageWeightS * formula.get("S") +
                    averageWeightP * formula.get("P");
            float remainingMass = averageWeight - getAverageWeight;
            formula.put("H", Math.round(remainingMass / averageWeightH));

            List<String> isotopeC = new ElementsDAO().getElementBySymbol(Element.C).getIsotopes();
            List<String> isotopeH = new ElementsDAO().getElementBySymbol(Element.H).getIsotopes();
            List<String> isotopeN = new ElementsDAO().getElementBySymbol(Element.N).getIsotopes();
            List<String> isotopeO = new ElementsDAO().getElementBySymbol(Element.O).getIsotopes();
            List<String> isotopeS = new ElementsDAO().getElementBySymbol(Element.S).getIsotopes();
            List<String> isotopeP = new ElementsDAO().getElementBySymbol(Element.P).getIsotopes();

            List<Float> isotopeDistributionC = getIsotopePercent(isotopeC);
            List<Float> isotopeDistributionH = getIsotopePercent(isotopeH);
            List<Float> isotopeDistributionN = getIsotopePercent(isotopeN);
            List<Float> isotopeDistributionO = getIsotopePercent(isotopeO);
            List<Float> isotopeDistributionS = getIsotopePercent(isotopeS);
            List<Float> isotopeDistributionP = getIsotopePercent(isotopeP);

            List<Float> distributionResult = new ArrayList<>();
            int maxIsotope = Constants.DIA_NR_ISOTOPES + 1;
            List<Float> isotopeDistributionConvolvedC = convolvePow(isotopeDistributionC, formula.get("C"));
            List<Float> isotopeDistributionConvolvedH = convolvePow(isotopeDistributionH, formula.get("H"));
            List<Float> isotopeDistributionConvolvedN = convolvePow(isotopeDistributionN, formula.get("N"));
            List<Float> isotopeDistributionConvolvedO = convolvePow(isotopeDistributionO, formula.get("O"));
            List<Float> isotopeDistributionConvolvedS = convolvePow(isotopeDistributionS, formula.get("S"));
            List<Float> isotopeDistributionConvolvedP = convolvePow(isotopeDistributionP, formula.get("P"));
            distributionResult = Arrays.asList(convolve(distributionResult, isotopeDistributionConvolvedC, maxIsotope));
            distributionResult = Arrays.asList(convolve(distributionResult, isotopeDistributionConvolvedH, maxIsotope));
            distributionResult = Arrays.asList(convolve(distributionResult, isotopeDistributionConvolvedN, maxIsotope));
            distributionResult = Arrays.asList(convolve(distributionResult, isotopeDistributionConvolvedO, maxIsotope));
            distributionResult = Arrays.asList(convolve(distributionResult, isotopeDistributionConvolvedS, maxIsotope));
            distributionResult = Arrays.asList(convolve(distributionResult, isotopeDistributionConvolvedP, maxIsotope));

            MathUtil.renormalize(distributionResult);
            float maxValueOfDistribution = distributionResult.get(MathUtil.findMaxIndex(distributionResult));
            for(int j=0; j<distributionResult.size(); j++){
                distributionResult.set(j, distributionResult.get(j) / maxValueOfDistribution);
            }

            isotopeDistList = distributionResult;

            float corr = 0.0f, m1 = 0.0f, m2 = 0.0f, s1 = 0.0f, s2 = 0.0f;
            for(int j=0;j<isotopesIntList.size(); j++){
                corr += isotopesIntList.get(j) * isotopeDistList.get(j);
                m1 += isotopesIntList.get(j);
                m2 += isotopeDistList.get(j);
                s1 += isotopesIntList.get(j) * isotopesIntList.get(j);
                s2 += isotopeDistList.get(j) * isotopeDistList.get(j);
            }
            m1 /= isotopesIntList.size();
            m2 /= isotopesIntList.size();
            s1 -= m1 * m1 * isotopesIntList.size();
            s2 -= m2 * m2 * isotopesIntList.size();
            if(s1 < Math.pow(1,-12) || s2 < Math.pow(1,-12)){
                continue;
            }else {
                if(s1 * s2 == 0){
                    continue;
                }else {
                    corr -= m1 * m2 * isotopesIntList.size();
                    corr /= Math.sqrt(s1 * s2);
                    isotopeCorr += relIntensity * corr;
                }
            }


            //get isotopeOverlap
            int nrOccurences = 0;
            float ratio, maxRatio = 0.0f;
            for(int ch = 1; ch <= Constants.DIA_NR_CHARGES; ch ++){
                float left = productMzArray[i] - Constants.C13C12_MASSDIFF_U/(float)ch;
                float right = left;
                left -= Constants.DIA_EXTRACT_WINDOW / 2f;
                right += Constants.DIA_EXTRACT_WINDOW/ 2f;

                //integrate window
                float mz = 0f, intensity = 0f;
                int leftIndex = MathUtil.bisection(spectrumMzArray, left);
                int rightIndex = MathUtil.bisection(spectrumMzArray, right);
                if(spectrumMzArray[0] < left){
                    leftIndex ++;
                }
                if(spectrumMzArray[spectrumMzArray.length-1] < right){
                    rightIndex ++;
                }
                for(int index = leftIndex; index <=rightIndex; index ++){
                    intensity += spectrumIntArray[index];
                    mz += spectrumMzArray[index] * spectrumIntArray[index];
                }
                if(intensity > 0f) {
                    mz /= intensity;

                    if(isotopesIntList.get(0)!=0){
                        ratio = intensity / isotopesIntList.get(0);
                    }else {
                        ratio = 0f;
                    }
                    if(ratio > maxRatio){
                        maxRatio = ratio;
                    }
                    float ddiffPpm = (float) Math.abs(mz - (productMzArray[i] - 1.0/(float)ch)) * 1000000f / productMzArray[i];
                    if(ratio > 1 && ddiffPpm < Constants.PEAK_BEFORE_MONO_MAX_PPM_DIFF){
                        nrOccurences ++;
                    }
                }
            }
            isotopeOverlap += nrOccurences * relIntensity;
        }
        scores.setVarIsotopeCorrelationScore(isotopeCorr);
        scores.setVarIsotopeOverlapScore(isotopeOverlap);
    }

    private List<Float> getIsotopePercent(List<String> isotopeLog){
        List<Float> isotopePercentList = new ArrayList<>();
        for(String isotope : isotopeLog){
            isotopePercentList.add(Float.parseFloat(isotope.split(":")[0]) / 100f);
        }
        Collections.sort(isotopePercentList);
        return isotopePercentList;
    }

    private List<Float> convolvePow(List<Float> distribution, int factor){

        if(factor == 1){
            return distribution;
        }
        int maxIsotope = Constants.DIA_NR_ISOTOPES + 1;

        int log2n = MathUtil.getLog2n(factor);
        Float[] convolveSquared = convolveSquare(distribution, maxIsotope);
        List<Float> convolveSquaredList = Arrays.asList(convolveSquared);
        if((log2n & 1) != 0){
            for(int i=1;; i++){
                if((log2n & 1<<i) != 0){
                    Float[] result = convolve(distribution, convolveSquaredList, maxIsotope);
                    distribution = Arrays.asList(result);
                }
                if(i >= log2n){
                    break;
                }
                convolveSquaredList = Arrays.asList(convolveSquare(convolveSquaredList, maxIsotope));
            }
        }
        return distribution;
    }

    private Float[] convolveSquare(List<Float> isotopeDistribution, int maxIsotope){
        int rMax = 2 * isotopeDistribution.size() - 1;
        if(maxIsotope != 0 && maxIsotope + 1< rMax){
            rMax = maxIsotope + 1;
        }
        Float[] result = new Float[rMax];
        for(int i = isotopeDistribution.size() - 1; i>=0; i--){
            for(int j=Math.min(rMax - i, isotopeDistribution.size())-1; j>=0; j--){
                result[i+j] += isotopeDistribution.get(i) * isotopeDistribution.get(j);
            }
        }
        return result;
    }

    private Float[] convolve(List<Float> leftDistribution, List<Float> rightFormerResult, int maxIsotope){
        int rMax = leftDistribution.size() + rightFormerResult.size() - 1;
        if(maxIsotope != 0 && rMax > maxIsotope){
            rMax = maxIsotope;
        }
        Float[] result = new Float[rMax];
        for(int i = leftDistribution.size() - 1; i >= 0; i--){
            for(int j = Math.min(rMax - i, rightFormerResult.size()) - 1; j >= 0; j--){
                result[i + j] += leftDistribution.get(i) * rightFormerResult.get(j);
            }
        }
        return result;
    }

    private void calculateBYIonScore(){}

    private void getBYSeries(Annotation annotation, HashMap<Integer, String> unimodHashMap, String sequence, int charge){
        double monoWeight = Constants.PROTON_MASS_U * charge;
        if(unimodHashMap.containsKey(0)){
            Unimod unimod = new UnimodDAO().getUnimod(unimodHashMap.get(0));
            if(unimod != null){
                monoWeight += unimod.getMonoMass();
            }
        }

        List<Double> bIonMzList = new ArrayList<>();
        char[] acidCodeArray = sequence.toCharArray();
        for (int i=0; i<acidCodeArray.length - 1; i++) {
            AminoAcid aa = new AminoAcidDAO().getAminoAcidByCode(String.valueOf(acidCodeArray[i]));
            if (aa == null) {
                continue;
            }
            if(i == 0){
                monoWeight += aa.getMonoIsotopicMass();
                continue;
            }
            monoWeight += aa.getMonoIsotopicMass();

        }
//        monoWeight +=

    }

    private void calculateElutionModelScore(List<ExperimentFeature> experimentFeatures){
        for(ExperimentFeature feature: experimentFeatures){
            RtIntensityPairs hullPoints = prepareElutionFit(feature);
            float sum = 0.0f;
            Float[] intArray = hullPoints.getIntensityArray();
            Float[] rtArray = hullPoints.getRtArray();
            for(float intens: intArray){
                sum += intens;
            }

            int medianIndex = 0;
            float count = 0f;
            for(int i=0; i<intArray.length; i++){
                count += intArray[i];
                if(count > sum /2f){
                    medianIndex = i-1;
                    break;
                }
            }
            float height = intArray[medianIndex];
            float retention = rtArray[medianIndex];
            boolean symmetric = false;
            float symmetry;
            if(rtArray[medianIndex] - rtArray[0] == 0f){
                symmetric = true;
                symmetry = 10;
            }else {
                symmetry = Math.abs((rtArray[rtArray.length - 1] - rtArray[medianIndex]) / (rtArray[medianIndex] - rtArray[0]));
            }
            if(symmetry < 1){
                symmetry +=5;
            }
            float width = symmetry;
            if(!symmetric){

            }

        }

    }

    private RtIntensityPairs prepareElutionFit(ExperimentFeature feature){
        List<Float> rtArray = feature.getHullRt();
        List<Float> intArray = feature.getHullInt();


        //get rt distance average
        float sum = rtArray.get(rtArray.size()-1) - rtArray.get(0);
        float rtDistanceAverage = sum / (rtArray.size() - 1);
        float rightSideRt = rtArray.get(rtArray.size()-1) + rtDistanceAverage;
        float leftSideRt = rtArray.get(0) - rtDistanceAverage;

        //get new List
        Float[] newRtArray = new Float[rtArray.size() + 6];
        Float[] newIntArray = new Float[intArray.size() + 6];
        assert intArray.size() == rtArray.size();

        for(int i=0; i<newRtArray.length; i++){
            if(i<3){
                newRtArray[i] = leftSideRt;
                newIntArray[i] = 0.0f;
            } else if(i>newRtArray.length - 4){
                newRtArray[i] = rightSideRt;
                newIntArray[i] = 0.0f;
            } else {
                newRtArray[i] = rtArray.get(i - 3);
                newIntArray[i] = intArray.get(i - 3);
            }
        }

        RtIntensityPairs rtIntensityPairs = new RtIntensityPairs();
        rtIntensityPairs.setRtArray(newRtArray);
        rtIntensityPairs.setIntensityArray(newIntArray);

        return rtIntensityPairs;
    }

    /**
     * Get the XCorrMatrix with experiment Features
     * @param experimentFeatures features in mrmFeature
     * @return Table<Integer, Integer, Float[]> xcorrMatrix
     */
    private Table<Integer, Integer, Float[]> initializeXCorrMatrix(List<ExperimentFeature> experimentFeatures){
        int listLength = experimentFeatures.size();
        Table<Integer, Integer, Float[]> xcorrMatrix = HashBasedTable.create();
        float[] intensityi, intensityj;
        for(int i=0; i<listLength;i++){
            for(int j=i; j<listLength;j++){
                intensityi = MathUtil.standardizeData(experimentFeatures.get(i).getHullInt());
                intensityj = MathUtil.standardizeData(experimentFeatures.get(j).getHullInt());
                xcorrMatrix.put(i,j,calculateCrossCorrelation(intensityi, intensityj));
            }
        }
        return xcorrMatrix;
    }

    private Float[] calculateCrossCorrelation(float[] data1, float[] data2){
        int maxDelay = data1.length;
        Float[] output = new Float[maxDelay * 2 + 1];
        double sxy;
        int j;
        for(int delay = - maxDelay; delay <= maxDelay; delay ++){
            sxy = 0;
            for(int i = 0; i < maxDelay; i++){
                j = i + delay;
                if(j < 0 || j >= maxDelay){
                    continue;
                }
                sxy += (data1[i] * data2[j]);
            }
            output[delay + maxDelay] = (float) sxy / maxDelay;
        }
        return output;
    }

    private float[] normalizeSum(List libraryIntensity){
        float[] normalizedLibraryIntensity = new float[libraryIntensity.size()];
        float sum = 0f;
        for(Object intensity: libraryIntensity){
            sum += (float)intensity;
        }

        if(sum == 0f){
            sum += 0.000001;
        }

        for(int i = 0; i<libraryIntensity.size(); i++){
            normalizedLibraryIntensity[i] = (float)libraryIntensity.get(i) / sum;
        }
        return normalizedLibraryIntensity;
    }
    /**
     * The score that is really matter to final pairs selection.
     * @param scores pre-calculated
     * @return final score
     */
    private float calculateLdaPrescore(RTNormalizationScores scores){
        return  scores.getVarLibraryCorr()              * -0.34664267f +
                scores.getVarLibraryRsmd()              *  2.98700722f +
                scores.getVarXcorrCoelution()           *  0.09445371f +
                scores.getVarXcorrShape()               * -5.71823862f +
                scores.getVarLogSnScore()               * -0.72989582f;
    }

}
