package com.westlake.air.propro.test.scorer;


import com.westlake.air.propro.constants.ScoreType;
import com.westlake.air.propro.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.propro.domain.bean.score.FeatureScores;
import com.westlake.air.propro.domain.bean.score.PeakGroup;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.algorithm.feature.ChromatographicScorer;
import com.westlake.air.propro.algorithm.feature.DIAScorer;
import com.westlake.air.propro.algorithm.feature.ElutionScorer;
import com.westlake.air.propro.algorithm.feature.LibraryScorer;
import com.westlake.air.propro.test.BaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Intensity Score has no TEST CASE
 *
 * Created by Nico Wang Ruimin
 * Time: 2018-09-11 23:01
 */
public class ScorerTest extends BaseTest {
    @Autowired
    ChromatographicScorer chromatographicScorer;
    @Autowired
    LibraryScorer libraryScorer;
    @Autowired
    DIAScorer diaScorer;
    @Autowired
    ElutionScorer elutionScorer;

    @Test
    public void calcChromatogramScoreTest(){
        System.out.println("------ Chromatogram Score Test ------");
        List<RtIntensityPairsDouble> chromatograms = new ArrayList<>();
        PeakGroup peakGroup = prepareChromatogramTestFeature();
        HashMap<String,Double> libraryIntensity = new HashMap<>();
        libraryIntensity.put("1",0.5d);
        libraryIntensity.put("2",0.5d);
        List<double[]> signalToNoiseList = new ArrayList<>();
        FeatureScores scores = new FeatureScores();

        chromatographicScorer.calculateChromatographicScores(peakGroup, libraryIntensity, scores, null);
        assert isSimilar(scores.get(ScoreType.XcorrCoelution), 1d + Math.sqrt(3.0d), Math.pow(10, -6));
        System.out.println("XcorrCoelution Test PASSED.");
        assert isSimilar(scores.get(ScoreType.XcorrCoelutionWeighted), 1.5, Math.pow(10, -6));
        System.out.println("XcorrCoelutionWeighted Test PASSED.");
        assert isSimilar(scores.get(ScoreType.XcorrShape), (1.0 + 0.3969832 + 1.0)/3.0, Math.pow(10, -6));
        System.out.println("XcorrShape Test PASSED.");
        assert isSimilar(scores.get(ScoreType.XcorrShapeWeighted), 0.6984916, Math.pow(10, -6));
        System.out.println("XcorrShapeWeighted Test PASSED.");
    }

    @Test
    public void calcLibraryScoreTest(){
        System.out.println("------ Library Score Test ------");
        PeakGroup peakGroup = prepareLibraryTestFeature();

        HashMap<String, Double> libraryIntensity = new HashMap<>();
        libraryIntensity.put("1", 1d/12001d);
        libraryIntensity.put("2", 10000d/12001d);
        libraryIntensity.put("3", 2000d/12001d);

        FeatureScores scores = new FeatureScores();

        libraryScorer.calculateLibraryScores(peakGroup, libraryIntensity, scores, null);
        assert isSimilar(scores.get(ScoreType.LibraryCorr), -0.654591316, Math.pow(10, -6));
        System.out.println("LibraryCorr Test PASSED.");
        assert isSimilar(scores.get(ScoreType.LibraryRsmd), 0.5800337593, Math.pow(10, -6));
        System.out.println("LibraryRsmd Test PASSED.");
        assert isSimilar(scores.get(ScoreType.LibraryDotprod), 0.34514801, Math.pow(10, -6));
        System.out.println("LibraryDotprod Test PASSED.");
        assert isSimilar(scores.get(ScoreType.LibraryManhattan), 1.279644714, Math.pow(10, -6));
        System.out.println("LibraryManhattan Test PASSED.");
        assert isSimilar(scores.get(ScoreType.LibrarySangle), 1.483262, Math.pow(10, -6));
        System.out.println("LibrarySangle Test PASSED.");
        assert isSimilar(scores.get(ScoreType.LibraryRootmeansquare), 0.6727226674, Math.pow(10, -6));
        System.out.println("LibraryRootmeansquare Test PASSED.");
    }

    @Test
    public void calcLogSnScoreTest(){
        PeakGroup peakGroup = prepareLogSnScoreTestFeature();
        peakGroup.setSignalToNoiseSum(2000d);
        FeatureScores scores = new FeatureScores();
        chromatographicScorer.calculateLogSnScore(peakGroup, scores);
        assert isSimilar(scores.get(ScoreType.LogSnScore), Math.log(1000), Math.pow(10, -6));
        System.out.println("LogSnScore Test PASSED.");
    }

    @Test
    public void calcNormRtScoreTest(){
        PeakGroup peakGroup1 = prepareNormRtTestFeature().get(0);
        PeakGroup peakGroup2 = prepareNormRtTestFeature().get(1);
        SlopeIntercept slopeIntercept = new SlopeIntercept();
        slopeIntercept.setSlope(1d);
        slopeIntercept.setIntercept(0d);
        FeatureScores scores = new FeatureScores();
        libraryScorer.calculateNormRtScore(peakGroup1, slopeIntercept, 100, scores);
        assert isSimilar(scores.get(ScoreType.NormRtScore), 0d, Math.pow(10, -6));
        libraryScorer.calculateNormRtScore(peakGroup2, slopeIntercept, 100, scores);
        assert isSimilar(scores.get(ScoreType.NormRtScore), 100d, Math.pow(10, -6));
        System.out.println("NormRtScore Test PASSED.");
    }

    @Test
    public void calcBYIonScoreTest(){
        //List<Float> spectrumMzArray, List<Float> spectrumIntArray, HashMap<Integer, String> unimodHashMap, String sequence, int charge, FeatureScores scores
        List<Float[]> spectrum = prepareBYIonScoreTestSpectrum();
        Float[] spectrumMz = spectrum.get(0);
        Float[] spectrumInt = spectrum.get(1);
        String sequence = "SYVAWDR";
        int charge = 1;
        HashMap<Integer, String> unimodHashMap = new HashMap<>();
        FeatureScores scores = new FeatureScores();
//        Constants.IS_TEST = true;
        diaScorer.calculateBYIonScore(spectrumMz, spectrumInt, unimodHashMap, sequence, charge, scores);
        assert isSimilar(scores.get(ScoreType.BseriesScore), 0d, Math.pow(10, -6));
        assert isSimilar(scores.get(ScoreType.YseriesScore), 0d, Math.pow(10, -6));
        System.out.println("VarBYseriesScore Test PASSED.");
    }

    @Test
    public void calcDiaIsotopeScoreTest(){

        Float[] spectrumMzArray = prepareDIASpectrum().get(0);
        Float[] spectrumIntArray = prepareDIASpectrum().get(1);
        HashMap<String, Float> productMzMap = new HashMap<>();
        productMzMap.put("1", 500f);
        productMzMap.put("2", 600f);
        PeakGroup peakGroup = new PeakGroup();
        HashMap<String,Double> ionIntensityMap = new HashMap<>();
        ionIntensityMap.put("1", 0.3D);
        ionIntensityMap.put("2", 0.7D);
        peakGroup.setIonIntensity(ionIntensityMap);
        peakGroup.setPeakGroupInt(1D);
        HashMap<String,Integer> productChargeMap = new HashMap<>();
        productChargeMap.put("1",1);
        productChargeMap.put("2",1);
        FeatureScores scores = new FeatureScores();
        diaScorer.calculateDiaIsotopeScores(peakGroup, productMzMap, spectrumMzArray, spectrumIntArray, productChargeMap, scores);

        assert isSimilar(scores.get(ScoreType.IsotopeCorrelationScore), 0.995335798317618 * 0.7 +  0.959692139694113 * 0.3, Math.pow(10, -3));
        assert isSimilar(scores.get(ScoreType.IsotopeOverlapScore), 0.3, Math.pow(10, -6));
        System.out.println("VarIsotopeScore Test PASSED.");
    }

    @Test
    public void calcDiaMassDiffScore(){
        //List<Double> productMzArray, List<Float> spectrumMzArray, List<Float> spectrumIntArray, List<Float> libraryIntensity, FeatureScores scores
        Float[] spectrumMzArray = prepareDIAShiftedSpectrum().get(0);
        Float[] spectrumIntArray = prepareDIAShiftedSpectrum().get(1);
        HashMap<String, Float> productMzMap = new HashMap<>();
        productMzMap.put("1",500f);
        productMzMap.put("2",600f);
        HashMap<String, Double> normedLibIntMap = new HashMap<>();
        normedLibIntMap.put("1", 0.7d);
        normedLibIntMap.put("2", 0.3d);
    FeatureScores scores = new FeatureScores();
        diaScorer.calculateDiaMassDiffScore(productMzMap, spectrumMzArray, spectrumIntArray, normedLibIntMap, scores, null);

        assert isSimilar(scores.get(ScoreType.MassdevScore), 13.33d, Math.pow(10, -1));
        assert isSimilar(scores.get(ScoreType.MassdevScoreWeighted), 7.38d, Math.pow(10, -1));
        System.out.println("MassdevScore Test PASSED.");
}

    @Test
    public void calcElutionScore(){
        PeakGroup peakGroup = prepareElutionScoreTestFeature();
        FeatureScores scores = new FeatureScores();
        elutionScorer.calculateElutionModelScore(peakGroup, scores);
        assert isSimilar(scores.get(ScoreType.ElutionModelFitScore), 0.92436583836873376, Math.pow(10, -5));
    }

    private PeakGroup prepareChromatogramTestFeature(){
        Double[] arr1 = {
                5.97543668746948, 4.2749171257019, 3.3301842212677, 4.08597040176392, 5.50307035446167, 5.24326848983765,
                8.40812492370605, 2.83419919013977, 6.94378805160522, 7.69957494735718, 4.08597040176392};

        Double[] arr2 = {
                15.8951349258423, 41.5446395874023, 76.0746307373047, 109.069435119629, 111.90364074707, 169.79216003418,
                121.043930053711, 63.0136985778809, 44.6150207519531, 21.4926776885986, 7.93575811386108};

        PeakGroup peakGroup = new PeakGroup();
        HashMap<String,Double[]> hullIntMap = new HashMap<>();
        hullIntMap.put("1", arr1);
        hullIntMap.put("2", arr2);
        peakGroup.setIonHullInt(hullIntMap);
        return peakGroup;
    }

    private PeakGroup prepareLibraryTestFeature(){
        PeakGroup peakGroup = new PeakGroup();
        HashMap<String, Double> ionIntensity = new HashMap<>();
        ionIntensity.put("1",782.38073);
        ionIntensity.put("2",58.384506);
        ionIntensity.put("3",58.384506);
        peakGroup.setIonIntensity(ionIntensity);
        peakGroup.setPeakGroupInt(782.38073+58.384506*2);
        return peakGroup;
    }

    private PeakGroup prepareLogSnScoreTestFeature(){
        PeakGroup peakGroup = new PeakGroup();
        Double[] rt = new Double[]{1200d};
        peakGroup.setIonHullRt(rt);
        peakGroup.setIonCount(2);
        return peakGroup;
    }
    private List<PeakGroup> prepareNormRtTestFeature(){
        PeakGroup peakGroup1 = new PeakGroup();
        PeakGroup peakGroup2 = new PeakGroup();
        peakGroup1.setApexRt(100d);
        peakGroup2.setApexRt(0d);
        List<PeakGroup> peakGroupList = new ArrayList<>();
        peakGroupList.add(peakGroup1);
        peakGroupList.add(peakGroup2);
        return peakGroupList;
    }

    //getBYSeries Test Passed
    private List<Float[]> prepareDIASpectrum(){
        Float[] intensity = {
                10f, 20f, 50f, 100f, 50f, 20f, 10f, // peak at 499 -> 260-20 = 240 intensity within 0.05 Th
                3f, 7f, 15f, 30f, 15f, 7f, 3f,      // peak at 500 -> 80-6 = 74 intensity within 0.05 Th
                1f, 3f, 9f, 15f, 9f, 3f, 1f,        // peak at 501 -> 41-2 = 39 intensity within 0.05 Th
                3f, 9f, 3f,                     // peak at 502 -> 15 intensity within 0.05 Th

                10f, 20f, 50f, 100f, 50f, 20f, 10f, // peak at 600 -> 260-20 = 240 intensity within 0.05 Th
                3f, 7f, 15f, 30f, 15f, 7f, 3f,      // peak at 601 -> 80-6 = 74 intensity within 0.05 Th
                1f, 3f, 9f, 15f, 9f, 3f, 1f,        // peak at 602 -> sum([ 9, 15, 9, 3, 1]) = 37 intensity within 0.05 Th
                3f, 9f, 3f                      // peak at 603
        };
        Float[] mz = {
                498.97f, 498.98f, 498.99f, 499.0f, 499.01f, 499.02f, 499.03f,
                499.97f, 499.98f, 499.99f, 500.0f, 500.01f, 500.02f, 500.03f,
                500.97f, 500.98f, 500.99f, 501.0f, 501.01f, 501.02f, 501.03f,
                501.99f, 502.0f, 502.01f,

                599.97f, 599.98f, 599.99f, 600.0f, 600.01f, 600.02f, 600.03f,
                600.97f, 600.98f, 600.99f, 601.0f, 601.01f, 601.02f, 601.03f,
                // note that this peak at 602 is special since it is integrated from
                // [(600+2*1.0033548) - 0.025, (600+2*1.0033548)  + 0.025] = [601.9817096 to 602.0317096]
                601.97f, 601.98f, 601.99f, 602.0f, 602.01f, 602.02f, 602.03f,
                602.99f, 603.0f, 603.01f
        };
        List<Float[]> spectrum = new ArrayList<>();
        spectrum.add(mz);
        spectrum.add(intensity);
        return spectrum;
    }

    private List<Float[]> prepareDIAShiftedSpectrum(){
        List<Float[]> spectrum = prepareDIASpectrum();
        for(int i=0; i<spectrum.get(0).length/2; i++){
            spectrum.get(0)[i] = spectrum.get(0)[i] + spectrum.get(0)[i]/1000000 * 15;
        }
        for(int i= spectrum.get(0).length/2; i< spectrum.get(0).length; i++){
            spectrum.get(0)[i] = spectrum.get(0)[i] + spectrum.get(0)[i]/1000000 * 10;
        }
        return spectrum;
    }

    private List<Float[]> prepareBYIonScoreTestSpectrum() {
        Float[] intensity = {
                100f, 100f, 100f, 100f,
                100f, 100f, 100f
        };
        Float[] mz ={
                // four of the naked b/y ions
                // as well as one of the modified b and y ions ion each
                350.17164f, // b
                421.20875f, // b
                421.20875f + 79.9657f, // b + P
                547.26291f, // y
                646.33133f, // y
                809.39466f + 79.9657f // y + P
        };
        List<Float[]> spectrum = new ArrayList<>();
        spectrum.add(mz);
        spectrum.add(intensity);
        return spectrum;
    }

    private PeakGroup prepareElutionScoreTestFeature(){
        Double[] rt =        { 3103.13, 3106.56, 3109.98, 3113.41, 3116.84, 3120.26, 3123.69, 3127.11, 3130.54, 3133.97, 3137.4 };
        Double[] intensity1 = { 5.97544, 4.27492, 3.33018, 4.08597, 5.50307, 5.24327, 8.40812, 2.8342 , 6.94379, 7.69957, 4.08597};
        Double[] intensity2 =   { 15.8951, 41.5446, 76.0746, 109.069, 111.904, 169.792, 121.044, 63.0137, 44.615 , 21.4927, 7.93576};
        Double[] intensity3 =   { 5.73925, 6.7076 , 2.85782, 5.0307 , 8.95135, 14.4544, 20.9731, 24.3033, 20.6897, 13.7459, 8.90411};

        HashMap<String,Double[]> intensityMap = new HashMap<>();
        intensityMap.put("1",intensity1);
        intensityMap.put("2",intensity2);
        intensityMap.put("3",intensity3);
        HashMap<String,Double> ionIntensityMap = new HashMap<>();
        ionIntensityMap.put("1",58.38450);
        ionIntensityMap.put("2",782.38073);
        ionIntensityMap.put("3",58.38450);
        PeakGroup peakGroup = new PeakGroup();
        peakGroup.setIonHullRt(rt);
        peakGroup.setIonHullInt(intensityMap);
        peakGroup.setIonIntensity(ionIntensityMap);
        peakGroup.setApexRt(3120d);
        peakGroup.setPeakGroupInt(973.122d);

        return peakGroup;
    }

    private boolean isSimilar(Double a, Double b, Double tolerance ) {
        if (Math.abs(a-b) < tolerance) {
            return true;
        } else {
            return false;
        }
    }
}
