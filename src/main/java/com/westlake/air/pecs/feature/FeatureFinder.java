package com.westlake.air.pecs.feature;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.domain.bean.analyse.PeptideSpectrum;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.score.IntensityRtLeftRtRightPairs;
import com.westlake.air.pecs.domain.bean.score.PeakGroup;
import com.westlake.air.pecs.utils.MathUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-01 22：42
 */
@Component("featureFinder")
public class FeatureFinder {

    public final Logger logger = LoggerFactory.getLogger(FeatureFinder.class);

    /**
     * 1）找出pickedChrom下的最高峰，得到对应rt和rtLeft、rtRight
     * 2）将pickedChrom最高峰intensity设为0
     * 3）将最高峰对应的chromatogram设为masterChromatogram
     * 4）对每个chromatogram映射到masterChromatogram，得到usedChromatogram
     * 5）对usedChromatogram求feature
     * 6）同时累计所有chromatogram（isDetecting）的intensity为totalXIC
     *
     * @param peptideSpectrum      origin chromatogram
     * @param ionPeaks       maxPeaks picked and recalculated
     * @param ionPeakParams left right borders
     *                           totalXic : intensity sum of all chromatogram of peptideRef(not rastered and all interval)
     *                           HullPoints : rt intensity pairs of rastered chromatogram between rtLeft, rtRight;
     *                           ExperimentFeature::intensity: intensity sum of hullPoints' intensity
     * @return list of mrmFeature (mrmFeature is list of chromatogram feature)
     */
    public List<PeakGroup> findFeatures(PeptideSpectrum peptideSpectrum, HashMap<String, RtIntensityPairsDouble> ionPeaks, HashMap<String, IntensityRtLeftRtRightPairs> ionPeakParams) {

        //totalXIC
        double totalXic = 0.0d;
        for (Double[] intensityTmp: peptideSpectrum.getIntensitiesMap().values()) {
            for (double intensity : intensityTmp) {
                totalXic += intensity;
            }
        }

        //mrmFeature loop
//        List<Double> apexRtList = new ArrayList<>();
//        List<Double> bestLeftRtList = new ArrayList<>();
//        List<Double> bestRightRtList = new ArrayList<>();
//        List<HashMap<String,Double>> ionApexIntList = new ArrayList<>();
//        List<Double[]> ionHullRtList = new ArrayList<>();
//        List<HashMap<String,Double[]>> ionHullIntList = new ArrayList<>();
//        List<HashMap<String, Double>> ionIntensityList = new ArrayList<>();
//        List<Double> peakGroupIntList = new ArrayList<>();
        List<PeakGroup> peakGroupList = new ArrayList<>();
        while (true) {
            PeakGroup peakGroup = new PeakGroup();
            Pair<String, Integer> maxPeakLocation = findLargestPeak(ionPeaks);
            if (maxPeakLocation.getKey().equals("null")) {
                break;
            }
            String maxCutInfo = maxPeakLocation.getKey();
            int maxIndex = maxPeakLocation.getValue();
            double apexRt = ionPeaks.get(maxCutInfo).getRtArray()[maxIndex];
            double bestLeft = ionPeakParams.get(maxCutInfo).getRtLeftArray()[maxIndex];
            double bestRight = ionPeakParams.get(maxCutInfo).getRtRightArray()[maxIndex];

            peakGroup.setApexRt(apexRt);
            peakGroup.setBestLeftRt(bestLeft);
            peakGroup.setBestRightRt(bestRight);

            RtIntensityPairsDouble rtInt = ionPeaks.get(maxCutInfo);
            rtInt.getIntensityArray()[maxIndex] = 0.0d;

            removeOverlappingFeatures(ionPeaks, bestLeft, bestRight, ionPeakParams);

            Double[] rtArray = peptideSpectrum.getRtArray();
            int leftIndex = MathUtil.bisection(rtArray, bestLeft).getHigh();
            int rightIndex = MathUtil.bisection(rtArray, bestRight).getHigh();

            //取得[bestLeft,bestRight]对应范围的Rt
            Double[] rasteredRt = new Double[rightIndex - leftIndex + 1];
            System.arraycopy(rtArray, leftIndex, rasteredRt, 0, rightIndex - leftIndex + 1);

            //取得[bestLeft,bestRight]对应范围的Intensity
            HashMap<String, Double[]> ionHullInt = new HashMap<>();
            HashMap<String, Double> ionIntensity = new HashMap<>();
            HashMap<String, Double> ionApexInt = new HashMap<>();
            Double peakGroupInt = 0D;
            for(String cutInfo: peptideSpectrum.getIntensitiesMap().keySet()) {
                Double[] intArray = peptideSpectrum.getIntensitiesMap().get(cutInfo);
                //离子峰最大强度
                ionApexInt.put(cutInfo, intArray[maxIndex]);
                //离子峰
                Double[] rasteredInt = new Double[rightIndex - leftIndex + 1];
                System.arraycopy(intArray, leftIndex, rasteredInt, 0, rightIndex - leftIndex + 1);
                ionHullInt.put(cutInfo, rasteredInt);
                //peakGroup强度
                Double ionIntTemp = MathUtil.sum(rasteredInt);
                peakGroupInt += ionIntTemp;
                //离子峰强度
                ionIntensity.put(cutInfo, ionIntTemp);
            }
            peakGroup.setIonHullRt(rasteredRt);
            peakGroup.setIonHullInt(ionHullInt);
            peakGroup.setIonApexInt(ionApexInt);
            peakGroup.setPeakGroupInt(peakGroupInt);
            peakGroup.setTotalXic(totalXic);
            peakGroup.setIonIntensity(ionIntensity);
            peakGroupList.add(peakGroup);
            if(peakGroupInt > 0 && peakGroupInt/totalXic<Constants.STOP_AFTER_INTENSITY_RATIO){
                break;
            }
        }

        return peakGroupList;
    }

    /**
     * 从maxPeak list中选取最大peak对应的index
     *
     * @param peaksCoord maxPeaks
     * @return list index, pairs index
     */
    private Pair<String, Integer> findLargestPeak(HashMap<String, RtIntensityPairsDouble> peaksCoord) {
        double largest = 0.0d;
        Pair<String, Integer> maxPeakLoc = Pair.of("null",-1);

        for (String cutInfo: peaksCoord.keySet()) {
            for (int i = 0; i < peaksCoord.get(cutInfo).getRtArray().length; i++) {
                if (peaksCoord.get(cutInfo).getIntensityArray()[i] > largest) {
                    largest = peaksCoord.get(cutInfo).getIntensityArray()[i];
                    maxPeakLoc = Pair.of(cutInfo, i);
                }
            }
        }
        return maxPeakLoc;
    }


    /**
     * 过滤区间内的峰值，也可以理解成：以已经选取的高峰划分peak group
     *
     * 中心落在更高峰闭区间内的会被过滤掉
     * 边界落在更高峰开区间内的会被过滤掉
     * @param ionPeaks
     * @param bestLeft 按从高到低顺序选择的最高峰的RT范围
     * @param bestRight 同上
     * @param ionPeakParams
     */
    private void removeOverlappingFeatures(HashMap<String, RtIntensityPairsDouble> ionPeaks, double bestLeft, double bestRight, HashMap<String, IntensityRtLeftRtRightPairs> ionPeakParams) {
        for (String cutInfo: ionPeaks.keySet()) {
            Double[] intensity = ionPeaks.get(cutInfo).getIntensityArray();
            Double[] rt = ionPeaks.get(cutInfo).getRtArray();
            for (int j = 0; j < intensity.length; j++) {
                if (intensity[j] <= 0d) {
                    continue;
                }
                if (rt[j] >= bestLeft && rt[j] <= bestRight) {
                    intensity[j] = 0d;
                }
                double left = ionPeakParams.get(cutInfo).getRtLeftArray()[j];
                double right = ionPeakParams.get(cutInfo).getRtRightArray()[j];
                if ((left > bestLeft && left < bestRight) || (right > bestLeft && right < bestRight)) {
                    intensity[j] = 0d;
                }
            }
        }
    }

}
