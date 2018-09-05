package com.westlake.air.pecs.test.rtnormalizer;

import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.score.*;
import com.westlake.air.pecs.feature.*;
import com.westlake.air.pecs.rtnormalizer.RTNormalizerScorer;
import com.westlake.air.pecs.test.BaseTest;
import com.westlake.air.pecs.utils.FileUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-09-04 19:15
 */
public class ScoreTest extends BaseTest {
    @Autowired
    GaussFilter gaussFilter;
    @Autowired
    PeakPicker peakPicker;
    @Autowired
    ChromatogramPicker chromatogramPicker;
    @Autowired
    SignalToNoiseEstimator signalToNoiseEstimator;
    @Autowired
    FeatureFinder featureFinder;
    @Autowired
    RTNormalizerScorer rtNormalizerScorer;

    @Test
    public void scoreTest() throws IOException {
        double groupRt = 0.44;

        List<Float> libraryIntensityList = new ArrayList<>();
        libraryIntensityList.add(1f);
        libraryIntensityList.add(2f);

        File fileTest1 = new File(PeakPickerTest.class.getClassLoader().getResource("data/scoreTest1.txt").getPath());
        BufferedReader readerTest1 = new BufferedReader(new FileReader(fileTest1));
        RtIntensityPairsDouble rtIntensityPairsDoubleTest1 = FileUtil.txtReader(readerTest1,"\t", 0,1);

        File fileTest2 = new File(PeakPickerTest.class.getClassLoader().getResource("data/scoreTest2.txt").getPath());
        BufferedReader readerTest2 = new BufferedReader(new FileReader(fileTest2));
        RtIntensityPairsDouble rtIntensityPairsDoubleTest2 = FileUtil.txtReader(readerTest2,"\t", 0,1);

        List<RtIntensityPairsDouble> rtIntensityPairsDoubleList = new ArrayList<>();
        rtIntensityPairsDoubleList.add(rtIntensityPairsDoubleTest1);
        rtIntensityPairsDoubleList.add(rtIntensityPairsDoubleTest2);

        List<RtIntensityPairsDouble> peakPickerList = new ArrayList<>();
        List<IntensityRtLeftRtRightPairs> chromatogramPickerList = new ArrayList<>();

        List<double[]> stn1000List = new ArrayList<>();
        for(int i=0; i<rtIntensityPairsDoubleList.size(); i++){
            RtIntensityPairsDouble gaussFilterResult = gaussFilter.filter(rtIntensityPairsDoubleList.get(i), 6.25f, 0.01f);

            double[] stn200 = signalToNoiseEstimator.computeSTN(gaussFilterResult, 200, 30);
            double[] stn1000 = signalToNoiseEstimator.computeSTN(gaussFilterResult, 1000, 30);
            double[] stnOrigin = signalToNoiseEstimator.computeSTN(rtIntensityPairsDoubleList.get(i), 1000, 30);

            stn1000List.add(stnOrigin);

            RtIntensityPairsDouble peakPickerResult = peakPicker.pickMaxPeak(gaussFilterResult, stn200);
            IntensityRtLeftRtRightPairs chromatogramPickerResult = chromatogramPicker.pickChromatogram(rtIntensityPairsDoubleList.get(i),gaussFilterResult, stn1000, peakPickerResult);

            peakPickerList.add(peakPickerResult);
            chromatogramPickerList.add(chromatogramPickerResult);
        }

        List<List<ExperimentFeature>> experimentFeature = featureFinder.findFeatures(rtIntensityPairsDoubleList, peakPickerList, chromatogramPickerList);

        FeatureByPep featureResult = new FeatureByPep();
        featureResult.setFeatureFound(true);
        featureResult.setExperimentFeatures(experimentFeature);
        featureResult.setLibraryIntensityList(libraryIntensityList);
        featureResult.setRtIntensityPairsOriginList(rtIntensityPairsDoubleList);
        featureResult.setNoise1000List(stn1000List);

        List<ScoreRtPair> scoreRtPairs = rtNormalizerScorer.score(featureResult.getRtIntensityPairsOriginList(), featureResult.getExperimentFeatures(), featureResult.getLibraryIntensityList(), featureResult.getNoise1000List(), new SlopeIntercept(), groupRt);

    }

}
