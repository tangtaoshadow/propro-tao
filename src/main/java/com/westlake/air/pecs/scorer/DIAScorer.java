package com.westlake.air.pecs.scorer;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.dao.AminoAcidDAO;
import com.westlake.air.pecs.dao.ElementsDAO;
import com.westlake.air.pecs.dao.UnimodDAO;
import com.westlake.air.pecs.domain.bean.score.BYSeries;
import com.westlake.air.pecs.domain.bean.score.ExperimentFeature;
import com.westlake.air.pecs.domain.bean.score.IntegrateWindowMzIntensity;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.domain.bean.transition.Annotation;
import com.westlake.air.pecs.parser.model.chemistry.AminoAcid;
import com.westlake.air.pecs.parser.model.chemistry.Element;
import com.westlake.air.pecs.parser.model.chemistry.Unimod;
import com.westlake.air.pecs.utils.MathUtil;
import com.westlake.air.pecs.utils.ScoreUtil;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
 *
 * scores.bseries_score
 * scores.yseries_score
 */
@Component("diaScorer")
public class DIAScorer {

    @Autowired
    UnimodDAO unimodDAO;
    @Autowired
    ElementsDAO elementsDAO;
    @Autowired
    AminoAcidDAO aminoAcidDAO;

    /**
     * scores.massdev_score 按spectrum intensity加权的mz与product mz的偏差ppm百分比之和
     * scores.weighted_massdev_score 按spectrum intensity加权的mz与product mz的偏差ppm百分比按libraryIntensity加权之和
     *
     * @param productMzArray 根据transitionGroup获得存在transition中的productMz，存成Float array
     * @param spectrumMzArray 根据transitionGroup选取的RT选择的最近的Spectrum对应的mzArray
     * @param spectrumIntArray 根据transitionGroup选取的RT选择的最近的Spectrum对应的intensityArray
     * @param libraryIntensity unNormalized library intensity(in transition)
     * @param scores score for JProphet
     */
    public void calculateDiaMassDiffScore(List<Double> productMzArray, List<Float> spectrumMzArray, List<Float> spectrumIntArray, List<Float> libraryIntensity, FeatureScores scores){

        double ppmScore = 0.0d;
        double ppmScoreWeighted = 0.0d;
        for(int i=0; i< productMzArray.size(); i++){
            double productMz = productMzArray.get(i);
            double left = productMz - Constants.DIA_EXTRACT_WINDOW / 2d;
            double right = productMz + Constants.DIA_EXTRACT_WINDOW/ 2d;

            //integrate window
            IntegrateWindowMzIntensity mzIntensity = ScoreUtil.integrateWindow(spectrumMzArray, spectrumIntArray, left, right);
            if(mzIntensity.isSignalFound()){
                double diffPpm = Math.abs(mzIntensity.getMz() - productMz) * 1000000d / productMz;
                ppmScore += diffPpm;
                ppmScoreWeighted += diffPpm * libraryIntensity.get(i);
            }
        }
        scores.setVarMassdevScore(ppmScore);
        scores.setVarMassdevScoreWeighted(ppmScoreWeighted);
    }


    /**
     * scores.isotope_correlation
     * scores.isotope_overlap //feature intensity加权的可能（带电量1-4）无法区分同位素峰值的平均发生次数之和
     *
     * @param experimentFeatures single mrmFeature
     * @param productMzArray mz of transition
     * @param spectrumMzArray mz array of selected Rt
     * @param spectrumIntArray intensity array of selected Rt
     * @param productCharge charge in transition
     * @param scores score for JProphet
     */
    public void calculateDiaIsotopeScores(List<ExperimentFeature> experimentFeatures, List<Double> productMzArray, List<Float> spectrumMzArray, List<Float> spectrumIntArray, List<Integer> productCharge, FeatureScores scores){
        double isotopeCorr = 0d;
        double isotopeOverlap = 0d;

        //getFirstIsotopeRelativeIntensities
        double relIntensity;//feature intensity / mrmfeature intensity
        double intensitySum = 0.0d;

        //intensity sum of feature
        for(ExperimentFeature feature: experimentFeatures){
            intensitySum += feature.getIntensity();
        }

        for(int i=0; i<experimentFeatures.size(); i++){
            relIntensity = (experimentFeatures.get(i).getIntensity() / intensitySum);
            int putativeFragmentCharge = 1;
            if(productCharge.get(i) > 1){
                putativeFragmentCharge = productCharge.get(i);
            }
            List<Double> isotopesIntList = new ArrayList<>();
            double maxIntensity = 0.0d;
            for(int iso=0; iso<=Constants.DIA_NR_ISOTOPES; iso++){
                double left = productMzArray.get(i) + iso * Constants.C13C12_MASSDIFF_U / putativeFragmentCharge;
                double right = left;
                left -= Constants.DIA_EXTRACT_WINDOW / 2d;
                right += Constants.DIA_EXTRACT_WINDOW/ 2d;

                //integrate window
                IntegrateWindowMzIntensity mzIntensity = ScoreUtil.integrateWindow(spectrumMzArray, spectrumIntArray, left, right);
                if(mzIntensity.getIntensity() > maxIntensity){
                    maxIntensity = mzIntensity.getIntensity();
                }
                isotopesIntList.add(mzIntensity.getIntensity());
            }

            //get scores.isotope_correlation
            List<Double> isotopeDistList;
            double averageWeight = Math.abs(productMzArray.get(i) * putativeFragmentCharge);
            //TODO @Nico no need to get each loop
            double averageWeightC = elementsDAO.getElementBySymbol(Element.C).getAverageWeight();
            double averageWeightH = elementsDAO.getElementBySymbol(Element.H).getAverageWeight();
            double averageWeightN = elementsDAO.getElementBySymbol(Element.N).getAverageWeight();
            double averageWeightO = elementsDAO.getElementBySymbol(Element.O).getAverageWeight();
            double averageWeightS = elementsDAO.getElementBySymbol(Element.S).getAverageWeight();
            double averageWeightP = elementsDAO.getElementBySymbol(Element.P).getAverageWeight();
            double avgTotal = Constants.C * averageWeightC +
                    Constants.H * averageWeightH +
                    Constants.N * averageWeightN +
                    Constants.O * averageWeightO +
                    Constants.S * averageWeightS +
                    Constants.P * averageWeightP;
            double factor = averageWeight / avgTotal;
            HashMap<String, Long> formula = new HashMap<>(); //等比放大算多少个？
            formula.put("C", Math.round(Constants.C * factor));
            formula.put("N", Math.round(Constants.N * factor));
            formula.put("O", Math.round(Constants.O * factor));
            formula.put("S", Math.round(Constants.S * factor));
            formula.put("P", Math.round(Constants.P * factor));

            double getAverageWeight = averageWeightC * formula.get("C") +
                    averageWeightN * formula.get("N") +
                    averageWeightO * formula.get("O") +
                    averageWeightS * formula.get("S") +
                    averageWeightP * formula.get("P");//模拟表达式的weight
            double remainingMass = averageWeight - getAverageWeight;
            formula.put("H", Math.round(remainingMass / averageWeightH));//residual添加H

            List<String> isotopeC = elementsDAO.getElementBySymbol(Element.C).getIsotopes();
            List<String> isotopeH = elementsDAO.getElementBySymbol(Element.H).getIsotopes();
            List<String> isotopeN = elementsDAO.getElementBySymbol(Element.N).getIsotopes();
            List<String> isotopeO = elementsDAO.getElementBySymbol(Element.O).getIsotopes();
            List<String> isotopeS = elementsDAO.getElementBySymbol(Element.S).getIsotopes();
            List<String> isotopeP = elementsDAO.getElementBySymbol(Element.P).getIsotopes();

            List<Double> isotopeDistributionC = getIsotopePercent(isotopeC);// percent
            List<Double> isotopeDistributionH = getIsotopePercent(isotopeH);
            List<Double> isotopeDistributionN = getIsotopePercent(isotopeN);
            List<Double> isotopeDistributionO = getIsotopePercent(isotopeO);
            List<Double> isotopeDistributionS = getIsotopePercent(isotopeS);
            List<Double> isotopeDistributionP = getIsotopePercent(isotopeP);

            List<Double> distributionResult = new ArrayList<>();
            distributionResult.add(1d);
            int maxIsotope = Constants.DIA_NR_ISOTOPES + 1;
            List<Double> isotopeDistributionConvolvedC = convolvePow(isotopeDistributionC, formula.get("C").intValue());
            List<Double> isotopeDistributionConvolvedH = convolvePow(isotopeDistributionH, formula.get("H").intValue());
            List<Double> isotopeDistributionConvolvedN = convolvePow(isotopeDistributionN, formula.get("N").intValue());
            List<Double> isotopeDistributionConvolvedO = convolvePow(isotopeDistributionO, formula.get("O").intValue());
            List<Double> isotopeDistributionConvolvedS = convolvePow(isotopeDistributionS, formula.get("S").intValue());
            List<Double> isotopeDistributionConvolvedP = convolvePow(isotopeDistributionP, formula.get("P").intValue());

            distributionResult = Arrays.asList(convolve(distributionResult, isotopeDistributionConvolvedH, maxIsotope));
            distributionResult = Arrays.asList(convolve(distributionResult, isotopeDistributionConvolvedN, maxIsotope));
            distributionResult = Arrays.asList(convolve(distributionResult, isotopeDistributionConvolvedP, maxIsotope));
            distributionResult = Arrays.asList(convolve(distributionResult, isotopeDistributionConvolvedS, maxIsotope));
            distributionResult = Arrays.asList(convolve(distributionResult, isotopeDistributionConvolvedO, maxIsotope));
            distributionResult = Arrays.asList(convolve(distributionResult, isotopeDistributionConvolvedC, maxIsotope));

            MathUtil.renormalize(distributionResult);
            double maxValueOfDistribution = distributionResult.get(MathUtil.findMaxIndex(distributionResult));
            for(int j=0; j<distributionResult.size(); j++){
                distributionResult.set(j, distributionResult.get(j) / maxValueOfDistribution);
            }

            isotopeDistList = distributionResult;

            double corr = 0.0d, m1 = 0.0d, m2 = 0.0d, s1 = 0.0d, s2 = 0.0d;
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
            if(s1 < Math.pow(10,-12) || s2 < Math.pow(10,-12)){
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
            double ratio;
            for(int ch = 1; ch <= Constants.DIA_NR_CHARGES; ch ++){
                double left = productMzArray.get(i) - Constants.C13C12_MASSDIFF_U/ch;
                double right = left;
                left -= Constants.DIA_EXTRACT_WINDOW / 2d;
                right += Constants.DIA_EXTRACT_WINDOW/ 2d;

                IntegrateWindowMzIntensity mzIntensity = ScoreUtil.integrateWindow(spectrumMzArray, spectrumIntArray, left, right);
                if(mzIntensity.isSignalFound()){
                    if(isotopesIntList.get(0)!=0){
                        ratio = mzIntensity.getIntensity() / isotopesIntList.get(0);
                    }else {
                        ratio = 0d;
                    }
                    //why 1.0 not Constants.C13C12_MASSDIFF_U
                    double ddiffPpm = Math.abs(mzIntensity.getMz() - (productMzArray.get(i) - 1.0/ch)) * 1000000d / productMzArray.get(i);
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

    /**
     * scores.bseries_score peptideRt对应的spectrumArray中，检测到的b离子的数量
     * scores.yseries_score peptideRt对应的spectrumArray中，检测到的y离子的数量
     *
     * @param spectrumMzArray
     * @param spectrumIntArray
     * @param unimodHashMap
     * @param sequence
     * @param charge
     * @param scores
     */
    public void calculateBYIonScore(List<Float> spectrumMzArray, List<Float> spectrumIntArray, HashMap<Integer, String> unimodHashMap, String sequence, int charge, FeatureScores scores){

        BYSeries bySeries = getBYSeries(unimodHashMap, sequence, charge);
        List<Double> bSeriesList = bySeries.getBSeries();
        int bSeriesScore = getSeriesScore(bSeriesList, spectrumMzArray, spectrumIntArray);

        List<Double> ySeriesList = bySeries.getYSeries();
        int ySeriesScore = getSeriesScore(ySeriesList, spectrumMzArray, spectrumIntArray);

        scores.setVarBseriesScore(bSeriesScore);
        scores.setVarYseriesScore(ySeriesScore);
    }



    private List<Double> getIsotopePercent(List<String> isotopeLog){
        List<Double> isotopePercentList = new ArrayList<>();
        for(String isotope : isotopeLog){
            isotopePercentList.add(Double.parseDouble(isotope.split(":")[0]) / 100d);
        }
//        Collections.sort(isotopePercentList);
//        Collections.reverse(isotopePercentList);
        return isotopePercentList;
    }

    /**
     *
     * @param distribution percent list of isotope
     * @param factor number of predicted element
     * @return
     */
    private List<Double> convolvePow(List<Double> distribution, int factor){

        if(factor == 1){
            return distribution;
        }
        int maxIsotope = Constants.DIA_NR_ISOTOPES + 1;

        int log2n = MathUtil.getLog2n(factor);//>

        List<Double> distributionResult = new ArrayList<>();
        if((factor & 1) == 1){
            distributionResult = distribution;
        }else {
            distributionResult.add(1.0d);
        }

        Double[] convolveSquared = convolveSquare(distribution, maxIsotope);
        List<Double> convolveSquaredList = Arrays.asList(convolveSquared);
            for(int i=1;; i++){
                if((factor & (1<<i)) == 1<<i){
                    Double[] result = convolve(distributionResult, convolveSquaredList, maxIsotope);
                    distributionResult = Arrays.asList(result);
                }
                if(i >= log2n){
                    break;
                }
                convolveSquaredList = Arrays.asList(convolveSquare(convolveSquaredList, maxIsotope));
            }
        return distributionResult;
    }

    /**
     *
     * @param isotopeDistribution percent list of isotope
     * @param maxIsotope Constants.DIA_NR_ISOTOPES + 1 = 5
     * @return
     */
    private Double[] convolveSquare(List<Double> isotopeDistribution, int maxIsotope){

        int rMax = 2 * isotopeDistribution.size() - 1;//3,5
        if(maxIsotope != 0 && maxIsotope + 1< rMax){
            rMax = maxIsotope + 1;
        }
        Double[] result = new Double[rMax];//5
        for(int i=0; i<rMax; i++){
            result[i] = 0d;
        }
        for(int i = isotopeDistribution.size() - 1; i>=0; i--){//each percent
            for(int j=Math.min(rMax - i, isotopeDistribution.size())-1; j>=0; j--){
                result[i+j] += isotopeDistribution.get(i) * isotopeDistribution.get(j);
            }
        }
        return result;
    }

    private Double[] convolve(List<Double> leftDistribution, List<Double> rightFormerResult, int maxIsotope){
        int rMax = leftDistribution.size() + rightFormerResult.size() - 1;
//        Collections.sort(leftDistribution);
//        Collections.reverse(leftDistribution);
//        Collections.sort(rightFormerResult);
//        Collections.reverse(rightFormerResult);
        if(maxIsotope != 0 && rMax > maxIsotope){
            rMax = maxIsotope;
        }
        Double[] result = new Double[rMax];
        for(int i=0; i<rMax; i++){
            result[i] = 0d;
        }
        for(int i = leftDistribution.size() - 1; i >= 0; i--){
            for(int j = Math.min(rMax - i, rightFormerResult.size()) - 1; j >= 0; j--){
                result[i + j] += leftDistribution.get(i) * rightFormerResult.get(j);
            }
        }
        return result;
    }

    //获得从B,Y开始的序列累加weight列表
    private BYSeries getBYSeries(HashMap<Integer, String> unimodHashMap, String sequence, int charge){

        BYSeries bySeries = new BYSeries();

        //bSeries 若要提高精度，提高json的精度
        List<Double> bSeries = new ArrayList<>();
        double monoWeight = Constants.PROTON_MASS_U * charge;
        if(unimodHashMap.containsKey(0)){
            Unimod unimod = unimodDAO.getUnimod(unimodHashMap.get(0));
            if(unimod != null){
                monoWeight += unimod.getMonoMass();
            }
        }

        char[] acidCodeArray = sequence.toCharArray();
        for (int i=0; i<acidCodeArray.length - 1; i++) {
            AminoAcid aa = aminoAcidDAO.getAminoAcidByCode(String.valueOf(acidCodeArray[i]));
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
            Unimod unimod = unimodDAO.getUnimod(unimodHashMap.get(acidCodeArray.length-1));
            if(unimod != null){
                monoWeight += unimod.getMonoMass();
            }
        }

        double h2oWeight = elementsDAO.getMonoWeight(ElementsDAO.H2O);
        for (int i = acidCodeArray.length - 1; i > 0; i--) {
            AminoAcid aa = aminoAcidDAO.getAminoAcidByCode(String.valueOf(acidCodeArray[i]));
            if (aa == null) {
                continue;
            }
            monoWeight += aa.getMonoIsotopicMass();
            ySeries.add(monoWeight + h2oWeight);
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
            double left = seriesMz - Constants.DIA_EXTRACT_WINDOW / 2d;
            double right = seriesMz + Constants.DIA_EXTRACT_WINDOW/ 2d;

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
