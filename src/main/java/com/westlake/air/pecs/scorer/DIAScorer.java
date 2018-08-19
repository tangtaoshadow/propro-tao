package com.westlake.air.pecs.scorer;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.dao.AminoAcidDAO;
import com.westlake.air.pecs.dao.ElementsDAO;
import com.westlake.air.pecs.dao.UnimodDAO;
import com.westlake.air.pecs.domain.bean.score.BYSeries;
import com.westlake.air.pecs.domain.bean.score.ExperimentFeature;
import com.westlake.air.pecs.domain.bean.score.IntegrateWindowMzIntensity;
import com.westlake.air.pecs.domain.bean.score.RTNormalizationScores;
import com.westlake.air.pecs.domain.bean.transition.Annotation;
import com.westlake.air.pecs.parser.model.chemistry.AminoAcid;
import com.westlake.air.pecs.parser.model.chemistry.Element;
import com.westlake.air.pecs.parser.model.chemistry.Unimod;
import com.westlake.air.pecs.utils.MathUtil;
import com.westlake.air.pecs.utils.ScoreUtil;

import java.util.*;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-15 16:09
 *
 * scores.massdev_score
 * scores.weighted_massdev_score
 *
 * scores.isotope_correlation
 * scores.isotope_overlap
 */
public class DIAScorer {

    /**
     * scores.massdev_score
     * scores.weighted_massdev_score
     *
     * @param productMzArray 根据transitionGroup获得存在transition中的productMz，存成Float array
     * @param spectrumMzArray 根据transitionGroup选取的RT选择的最近的Spectrum对应的mzArray
     * @param spectrumIntArray 根据transitionGroup选取的RT选择的最近的Spectrum对应的intensityArray
     * @param libraryIntensity unNormalized library intensity(in transition)
     * @param scores score for JProphet
     */
    public void calculateDiaMassDiffScore(Float[] productMzArray, List<Float> spectrumMzArray, List<Float> spectrumIntArray, List<Float> libraryIntensity, RTNormalizationScores scores){

        float ppmScore = 0.0f;
        float ppmScoreWeighted = 0.0f;
        for(int i=0; i< productMzArray.length; i++){
            float left = productMzArray[i] - Constants.DIA_EXTRACT_WINDOW / 2f;
            float right = productMzArray[i] + Constants.DIA_EXTRACT_WINDOW/ 2f;

            //integrate window
            IntegrateWindowMzIntensity mzIntensity = ScoreUtil.integrateWindow(spectrumMzArray, spectrumIntArray, left, right);
            if(mzIntensity.isSignalFound()){
                float diffPpm = Math.abs(mzIntensity.getMz() - productMzArray[i]) * 1000000f / productMzArray[i];
                ppmScore += diffPpm;
                ppmScoreWeighted += diffPpm * libraryIntensity.get(i);
            }
        }
        scores.setVarMassdevScore(ppmScore);
        scores.setVarMassdevScoreWeighted(ppmScoreWeighted);
    }


    /**
     * scores.isotope_correlation
     * scores.isotope_overlap
     *
     * @param experimentFeatures single mrmFeature
     * @param productMzArray mz of transition
     * @param spectrumMzArray mz array of selected Rt
     * @param spectrumIntArray intensity array of selected Rt
     * @param productCharge charge in transition
     * @param scores score for JProphet
     */
    public void calculateDiaIsotopeScores(List<ExperimentFeature> experimentFeatures, List<Float> productMzArray, List<Float> spectrumMzArray, List<Float> spectrumIntArray, List<Integer> productCharge, RTNormalizationScores scores){
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
                float left = productMzArray.get(i) + iso * Constants.C13C12_MASSDIFF_U / putativeFragmentCharge;
                float right = left;
                left -= Constants.DIA_EXTRACT_WINDOW / 2f;
                right += Constants.DIA_EXTRACT_WINDOW/ 2f;

                //integrate window
                IntegrateWindowMzIntensity mzIntensity = ScoreUtil.integrateWindow(spectrumMzArray, spectrumIntArray, left, right);
                if(mzIntensity.getIntensity() > maxIntensity){
                    maxIntensity = mzIntensity.getIntensity();
                }
                isotopesIntList.add(mzIntensity.getIntensity());
            }

            //get scores.isotope_correlation
            List<Float> isotopeDistList;
            float averageWeight = Math.abs(productMzArray.get(i) * putativeFragmentCharge);
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


            //get scores.isotope_overlap
            int nrOccurences = 0;
            float ratio, maxRatio = 0.0f;
            for(int ch = 1; ch <= Constants.DIA_NR_CHARGES; ch ++){
                float left = productMzArray.get(i) - Constants.C13C12_MASSDIFF_U/(float)ch;
                float right = left;
                left -= Constants.DIA_EXTRACT_WINDOW / 2f;
                right += Constants.DIA_EXTRACT_WINDOW/ 2f;

                IntegrateWindowMzIntensity mzIntensity = ScoreUtil.integrateWindow(spectrumMzArray, spectrumIntArray, left, right);
                if(mzIntensity.isSignalFound()){
                    if(isotopesIntList.get(0)!=0){
                        ratio = mzIntensity.getIntensity() / isotopesIntList.get(0);
                    }else {
                        ratio = 0f;
                    }
                    if(ratio > maxRatio){
                        maxRatio = ratio;
                    }
                    float ddiffPpm = (float) Math.abs(mzIntensity.getMz() - (productMzArray.get(i) - 1.0/(float)ch)) * 1000000f / productMzArray.get(i);
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

    public void calculateBYIonScore(List<Float> spectrumMzArray, List<Float> spectrumIntArray, Annotation annotation, HashMap<Integer, String> unimodHashMap, String sequence, int charge, RTNormalizationScores scores){
        BYSeries bySeries = getBYSeries(annotation, unimodHashMap, sequence, charge);
        List<Double> bSeriesList = bySeries.getBSeries();
        int bSeriesScore = getSeriesScore(bSeriesList, spectrumMzArray, spectrumIntArray);

        List<Double> ySeriesList = bySeries.getYSeries();
        int ySeriesScore = getSeriesScore(ySeriesList, spectrumMzArray, spectrumIntArray);

        scores.setVarBseriesScore(bSeriesScore);
        scores.setVarYseriesScore(ySeriesScore);
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
    private BYSeries getBYSeries(Annotation annotation, HashMap<Integer, String> unimodHashMap, String sequence, int charge){

        BYSeries bySeries = new BYSeries();

        //bSeries
        List<Double> bSeries = new ArrayList<>();
        double monoWeight = Constants.PROTON_MASS_U * charge;
        if(unimodHashMap.containsKey(0)){
            Unimod unimod = new UnimodDAO().getUnimod(unimodHashMap.get(0));
            if(unimod != null){
                monoWeight += unimod.getMonoMass();
            }
        }

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
            bSeries.add(monoWeight);
        }

        //ySeries
        List<Double> ySeries = new ArrayList<>();
        monoWeight = Constants.PROTON_MASS_U * charge;
        if(unimodHashMap.containsKey(acidCodeArray.length-1)){
            Unimod unimod = new UnimodDAO().getUnimod(unimodHashMap.get(acidCodeArray.length-1));
            if(unimod != null){
                monoWeight += unimod.getMonoMass();
            }
        }

        for (int i = acidCodeArray.length - 1; i > 0; i--) {
            AminoAcid aa = new AminoAcidDAO().getAminoAcidByCode(String.valueOf(acidCodeArray[i]));
            if (aa == null) {
                continue;
            }
            monoWeight += aa.getMonoIsotopicMass();
            monoWeight += new ElementsDAO().getMonoWeight("H:2,O:1");
            ySeries.add(monoWeight);
        }

        bySeries.setBSeries(bSeries);
        bySeries.setYSeries(ySeries);

        return bySeries;
    }


    /**
     * score unit of BYSeries scores
     * @param seriesList ySeriesList or bSeriesList
     * @param spectrumMzArray mzArray of certain spectrum
     * @param spectrumIntArray intArray of certain spectrum
     * @return score of b or y
     */
    private int getSeriesScore(List<Double> seriesList, List<Float> spectrumMzArray, List<Float> spectrumIntArray){
        int seriesScore = 0;
        for(double seriesMz: seriesList){
            double left = seriesMz - Constants.DIA_EXTRACT_WINDOW / 2f;
            double right = seriesMz + Constants.DIA_EXTRACT_WINDOW/ 2f;

            IntegrateWindowMzIntensity mzIntensity = ScoreUtil.integrateWindow(spectrumMzArray, spectrumIntArray, left, right);
            double ppmDiff = Math.abs(seriesMz - mzIntensity.getMz()) * 1000000 / seriesMz;
            if(mzIntensity.isSignalFound() &&
                    ppmDiff < Constants.DIA_BYSERIES_PPM_DIFF &&
                    mzIntensity.getIntensity() > Constants.DIA_BYSERIES_INTENSITY_MIN){
                seriesScore ++;
            }
        }
        return seriesScore;
    }
}
