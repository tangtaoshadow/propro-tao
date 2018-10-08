package com.westlake.air.pecs.test.scorer;


import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.score.ExperimentFeature;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.scorer.ChromatographicScorer;
import com.westlake.air.pecs.scorer.DIAScorer;
import com.westlake.air.pecs.scorer.ElutionScorer;
import com.westlake.air.pecs.scorer.LibraryScorer;
import com.westlake.air.pecs.test.BaseTest;
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
        //List<RtIntensityPairsDouble> chromatograms, List<ExperimentFeature> experimentFeatures, List<Float> libraryIntensity, List<double[]> signalToNoiseList, FeatureScores scores
        List<RtIntensityPairsDouble> chromatograms = new ArrayList<>();
        List<ExperimentFeature> experimentFeatures = prepareChromatogramTestFeature();
        List<Double> libraryIntensity = new ArrayList<>();
        libraryIntensity.add(0.5d);
        libraryIntensity.add(0.5d);
        List<double[]> signalToNoiseList = new ArrayList<>();
        FeatureScores scores = new FeatureScores();

        chromatographicScorer.calculateChromatographicScores(experimentFeatures, libraryIntensity, scores);
        assert isSimilar(scores.getVarXcorrCoelution(), 1d + Math.sqrt(3.0d), Math.pow(10, -6));
        System.out.println("VarXcorrCoelution Test PASSED.");
        assert isSimilar(scores.getVarXcorrCoelutionWeighted(), 1.5, Math.pow(10, -6));
        System.out.println("VarXcorrCoelutionWeighted Test PASSED.");
        assert isSimilar(scores.getVarXcorrShape(), (1.0 + 0.3969832 + 1.0)/3.0, Math.pow(10, -6));
        System.out.println("VarXcorrShape Test PASSED.");
        assert isSimilar(scores.getVarXcorrShapeWeighted(), 0.6984916, Math.pow(10, -6));
        System.out.println("VarXcorrShapeWeighted Test PASSED.");
    }

    @Test
    public void calcLibraryScoreTest(){
        System.out.println("------ Library Score Test ------");
        //List<ExperimentFeature> experimentFeatures, List<Float> libraryIntensity, FeatureScores scores
        List<ExperimentFeature> experimentFeatures = prepareLibraryTestFeature();

        List<Double> libraryIntensity = new ArrayList<>();
        libraryIntensity.add(1d);
        libraryIntensity.add(10000d);
        libraryIntensity.add(2000d);

        FeatureScores scores = new FeatureScores();

        libraryScorer.calculateLibraryScores(experimentFeatures, libraryIntensity, scores);
        assert isSimilar(scores.getVarLibraryCorr(), -0.654591316, Math.pow(10, -6));
        System.out.println("VarLibraryCorr Test PASSED.");
        assert isSimilar(scores.getVarLibraryRsmd(), 0.5800337593, Math.pow(10, -6));
        System.out.println("VarLibraryRsmd Test PASSED.");
    }

    @Test
    public void calcLogSnScoreTest(){
        //List<RtIntensityPairsDouble> chromatograms, List<ExperimentFeature> experimentFeatures, List<double[]> signalToNoiseList, FeatureScores scores
        double[] stn1 = {500};
        double[] stn2 = {1500};
        List<double[]> signalToNoiseList = new ArrayList<>();
        signalToNoiseList.add(stn1);
        signalToNoiseList.add(stn2);
        Double[] rt = {0d};
        Double[] intens = {0d};
        RtIntensityPairsDouble rtIntensityPairsDouble = new RtIntensityPairsDouble(rt, intens);
        List<RtIntensityPairsDouble> chromatogram = new ArrayList<>();
        chromatogram.add(rtIntensityPairsDouble);
        chromatogram.add(rtIntensityPairsDouble);
        List<ExperimentFeature> experimentFeatures = prepareLogSnScoreTestFeature();
        FeatureScores scores = new FeatureScores();
        chromatographicScorer.calculateLogSnScore(chromatogram, experimentFeatures, signalToNoiseList, scores);
        assert isSimilar(scores.getVarLogSnScore(), Math.log(1000), Math.pow(10, -6));
        System.out.println("VarLogSnScore Test PASSED.");
    }

    @Test
    public void calcNormRtScoreTest(){
        //List<ExperimentFeature> experimentFeatures, SlopeIntercept slopeIntercept, double groupRt, FeatureScores scores
        List<ExperimentFeature> experimentFeatures1 = prepareNormRtTestFeature().get(0);
        List<ExperimentFeature> experimentFeatures2 = prepareNormRtTestFeature().get(1);
        SlopeIntercept slopeIntercept = new SlopeIntercept();
        slopeIntercept.setSlope(1);
        FeatureScores scores = new FeatureScores();
        libraryScorer.calculateNormRtScore(experimentFeatures1, slopeIntercept, 100, scores);
        assert isSimilar(scores.getVarNormRtScore(), 0d, Math.pow(10, -6));
        libraryScorer.calculateNormRtScore(experimentFeatures2, slopeIntercept, 100, scores);
        assert isSimilar(scores.getVarNormRtScore(), 100d, Math.pow(10, -6));
        System.out.println("VarNormRtScore Test PASSED.");
    }

    @Test
    public void calcBYIonScoreTest(){
        //List<Float> spectrumMzArray, List<Float> spectrumIntArray, HashMap<Integer, String> unimodHashMap, String sequence, int charge, FeatureScores scores
        List<List<Float>> spectrum = prepareBYIonScoreTestSpectrum();
        List<Float> spectrumMz = spectrum.get(0);
        List<Float> spectrumInt = spectrum.get(1);
        String sequence = "SYVAWDR";
        int charge = 1;
        HashMap<Integer, String> unimodHashMap = new HashMap<>();
        FeatureScores scores = new FeatureScores();
//        Constants.IS_TEST = true;
        diaScorer.calculateBYIonScore(spectrumMz, spectrumInt, unimodHashMap, sequence, charge, scores);
        assert isSimilar(scores.getVarBseriesScore(), 0d, Math.pow(10, -6));
        assert isSimilar(scores.getVarYseriesScore(), 0d, Math.pow(10, -6));
        System.out.println("VarBYseriesScore Test PASSED.");
    }

    @Test
    public void calcDiaIsotopeScoreTest(){
        //List<ExperimentFeature> experimentFeatures, List<Double> productMzArray, List<Float> spectrumMzArray, List<Float> spectrumIntArray, List<Integer> productCharge, FeatureScores scores
        //List<ExperimentFeature> experimentFeatures = ;
        List<Float> spectrumMzArray = prepareDIASpectrum().get(0);
        List<Float> spectrumIntArray = prepareDIASpectrum().get(1);
        List<Double> productMzArray = new ArrayList<>();
        productMzArray.add(500d);
        productMzArray.add(600d);
        ExperimentFeature experimentFeature1 = new ExperimentFeature();
        ExperimentFeature experimentFeature2 = new ExperimentFeature();
        experimentFeature1.setIntensity(0.3);
        experimentFeature2.setIntensity(0.7);
        List<ExperimentFeature> experimentFeatures = new ArrayList<>();
        experimentFeatures.add(experimentFeature1);
        experimentFeatures.add(experimentFeature2);
        List<Integer> productCharge = new ArrayList<>();
        productCharge.add(1);
        productCharge.add(1);
        FeatureScores scores = new FeatureScores();
        diaScorer.calculateDiaIsotopeScores(experimentFeatures, productMzArray, spectrumMzArray, spectrumIntArray, productCharge, scores);

        assert isSimilar(scores.getVarIsotopeCorrelationScore(), 0.995335798317618 * 0.7 +  0.959692139694113 * 0.3, Math.pow(10, -3));
        assert isSimilar(scores.getVarIsotopeOverlapScore(), 0.3, Math.pow(10, -6));
        System.out.println("VarIsotopeScore Test PASSED.");
    }

    @Test
    public void calcDiaMassDiffScore(){
        //List<Double> productMzArray, List<Float> spectrumMzArray, List<Float> spectrumIntArray, List<Float> libraryIntensity, FeatureScores scores
        List<Float> spectrumMzArray = prepareDIAShiftedSpectrum().get(0);
        List<Float> spectrumIntArray = prepareDIAShiftedSpectrum().get(1);
        List<Double> productMzArray = new ArrayList<>();
        productMzArray.add(500d);
        productMzArray.add(600d);
        List<Float> libraryIntensity = new ArrayList<>();
        libraryIntensity.add(0.7f);
        libraryIntensity.add(0.3f);
        FeatureScores scores = new FeatureScores();
        diaScorer.calculateDiaMassDiffScore(productMzArray, spectrumMzArray, spectrumIntArray, libraryIntensity, scores);

        assert isSimilar(scores.getVarMassdevScore(), 13.33d, Math.pow(10, -1));
        assert isSimilar(scores.getVarMassdevScoreWeighted(), 7.38d, Math.pow(10, -1));
        System.out.println("VarMassdevScore Test PASSED.");

    }

    @Test
    public void calcElutionScore(){
        List<ExperimentFeature> experimentFeatures = prepareElutionScoreTestFeature();
        FeatureScores scores = new FeatureScores();
        elutionScorer.calculateElutionModelScore(experimentFeatures, scores);
        assert isSimilar(scores.getVarElutionModelFitScore(), 0.92436583836873376, Math.pow(10, -5));
    }


    private List<ExperimentFeature> prepareChromatogramTestFeature(){
        Double[] arr1 = {
                5.97543668746948, 4.2749171257019, 3.3301842212677, 4.08597040176392, 5.50307035446167, 5.24326848983765,
                8.40812492370605, 2.83419919013977, 6.94378805160522, 7.69957494735718, 4.08597040176392};

        Double[] arr2 = {
                15.8951349258423, 41.5446395874023, 76.0746307373047, 109.069435119629, 111.90364074707, 169.79216003418,
                121.043930053711, 63.0136985778809, 44.6150207519531, 21.4926776885986, 7.93575811386108};

        List<ExperimentFeature> experimentFeatureList = new ArrayList<>();
        ExperimentFeature experimentFeature1 = new ExperimentFeature();
        ExperimentFeature experimentFeature2 = new ExperimentFeature();
        experimentFeature1.setHullInt(Arrays.asList(arr1));
        experimentFeature2.setHullInt(Arrays.asList(arr2));
        experimentFeatureList.add(experimentFeature1);
        experimentFeatureList.add(experimentFeature2);
        return experimentFeatureList;
    }

    private List<ExperimentFeature> prepareLibraryTestFeature(){
        List<ExperimentFeature> experimentFeatures = new ArrayList<>();
        ExperimentFeature experimentFeature1 = new ExperimentFeature();
        ExperimentFeature experimentFeature2 = new ExperimentFeature();
        ExperimentFeature experimentFeature3 = new ExperimentFeature();
        experimentFeature1.setIntensity(782.38073);
        experimentFeature2.setIntensity(58.384506);
        experimentFeature3.setIntensity(58.384506);
        experimentFeatures.add(experimentFeature1);
        experimentFeatures.add(experimentFeature2);
        experimentFeatures.add(experimentFeature3);
        return experimentFeatures;
    }

    private List<ExperimentFeature> prepareLogSnScoreTestFeature(){
        List<ExperimentFeature> experimentFeatures = new ArrayList<>();
        ExperimentFeature experimentFeature = new ExperimentFeature();
        List<Double> rt = new ArrayList<>();
        rt.add(1200d);
        experimentFeature.setHullRt(rt);
        experimentFeatures.add(experimentFeature);
        experimentFeatures.add(experimentFeature);
        return experimentFeatures;
    }
    private List<List<ExperimentFeature>> prepareNormRtTestFeature(){
        List<ExperimentFeature> experimentFeatures1 = new ArrayList<>();
        List<ExperimentFeature> experimentFeatures2 = new ArrayList<>();
        ExperimentFeature experimentFeature1 = new ExperimentFeature();
        ExperimentFeature experimentFeature2 = new ExperimentFeature();
        experimentFeature1.setRt(100);
        experimentFeature2.setRt(0);
        experimentFeatures1.add(experimentFeature1);
        experimentFeatures2.add(experimentFeature2);
        List<List<ExperimentFeature>> experimentFeaturesList = new ArrayList<>();
        experimentFeaturesList.add(experimentFeatures1);
        experimentFeaturesList.add(experimentFeatures2);
        return experimentFeaturesList;
    }

    //getBYSeries Test Passed
    private List<List<Float>> prepareDIASpectrum(){
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
        List<Float> spectrumInt = Arrays.asList(intensity);
        List<Float> spectrumMz = Arrays.asList(mz);
        List<List<Float>> spectrum = new ArrayList<>();
        spectrum.add(spectrumMz);
        spectrum.add(spectrumInt);
        return spectrum;
    }

    private List<List<Float>> prepareDIAShiftedSpectrum(){
        List<List<Float>> spectrum = prepareDIASpectrum();
        for(int i=0; i<spectrum.get(0).size()/2; i++){
            spectrum.get(0).set(i,spectrum.get(0).get(i) + spectrum.get(0).get(i)/1000000 * 15);
        }
        for(int i= spectrum.get(0).size()/2; i< spectrum.get(0).size(); i++){
            spectrum.get(0).set(i,spectrum.get(0).get(i) + spectrum.get(0).get(i)/1000000 * 10);
        }
        return spectrum;
    }

    private List<List<Float>> prepareBYIonScoreTestSpectrum() {
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
        List<Float> spectrumInt = Arrays.asList(intensity);
        List<Float> spectrumMz = Arrays.asList(mz);
        List<List<Float>> spectrum = new ArrayList<>();
        spectrum.add(spectrumMz);
        spectrum.add(spectrumInt);
        return spectrum;
    }

    private List<ExperimentFeature> prepareElutionScoreTestFeature(){
        ExperimentFeature experimentFeature1 = new ExperimentFeature();
        Double[] rt1 =        { 3103.13, 3106.56, 3109.98, 3113.41, 3116.84, 3120.26, 3123.69, 3127.11, 3130.54, 3133.97, 3137.4 };
        Double[] intensity1 = { 5.97544, 4.27492, 3.33018, 4.08597, 5.50307, 5.24327, 8.40812, 2.8342 , 6.94379, 7.69957, 4.08597};
        experimentFeature1.setHullRt(Arrays.asList(rt1));
        experimentFeature1.setHullInt(Arrays.asList(intensity1));
        experimentFeature1.setIntensity(58.38450);
        experimentFeature1.setRt(3120d);
        experimentFeature1.setIntensitySum(973.122);

        ExperimentFeature experimentFeature2 = new ExperimentFeature();
        Double[] rt2 =          { 3103.13, 3106.56, 3109.98, 3113.41, 3116.84, 3120.26, 3123.69, 3127.11, 3130.54, 3133.97, 3137.4 };
        Double[] intensity2 =   { 15.8951, 41.5446, 76.0746, 109.069, 111.904, 169.792, 121.044, 63.0137, 44.615 , 21.4927, 7.93576};
        experimentFeature2.setHullRt(Arrays.asList(rt2));
        experimentFeature2.setHullInt(Arrays.asList(intensity2));
        experimentFeature2.setIntensity(782.38073);
        experimentFeature2.setRt(3120d);
        experimentFeature2.setIntensitySum(973.122);

        ExperimentFeature experimentFeature3 = new ExperimentFeature();
        Double[] rt3 =          { 3103.13, 3106.56, 3109.98, 3113.41, 3116.84, 3120.26, 3123.69, 3127.11, 3130.54, 3133.97, 3137.4};
        Double[] intensity3 =   { 5.73925, 6.7076 , 2.85782, 5.0307 , 8.95135, 14.4544, 20.9731, 24.3033, 20.6897, 13.7459, 8.90411};
        experimentFeature3.setHullRt(Arrays.asList(rt3));
        experimentFeature3.setHullInt(Arrays.asList(intensity3));
        experimentFeature3.setIntensity(58.38450);
        experimentFeature3.setRt(3120d);
        experimentFeature3.setIntensitySum(973.122);

        List<ExperimentFeature> experimentFeatures = new ArrayList<>();
        experimentFeatures.add(experimentFeature1);
        experimentFeatures.add(experimentFeature2);
        experimentFeatures.add(experimentFeature3);

        return experimentFeatures;
    }

    private boolean isSimilar(Double a, Double b, Double tolerance ) {
        if (Math.abs(a-b) < tolerance) {
            return true;
        } else {
            return false;
        }
    }
}
