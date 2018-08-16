package com.westlake.air.pecs.scorer;

import com.westlake.air.pecs.domain.bean.score.ExperimentFeature;
import com.westlake.air.pecs.domain.bean.score.RTNormalizationScores;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.utils.MathUtil;
import com.westlake.air.pecs.utils.ScoreUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-15 16:06
 */
public class LibraryScorer {
    /**
     * scores.library_corr
     * scores.library_norm_manhattan
     * @param experimentFeatures get experimentIntensity: from features extracted
     * @param libraryIntensity get libraryIntensity: from transitions
     * @param scores library_corr, library_norm_manhattan
     */
    public void calculateLibraryScores(List<ExperimentFeature> experimentFeatures, List<Float> libraryIntensity, RTNormalizationScores scores, SlopeIntercept slopeIntercept, float groupRt){
        List<Float> experimentIntensity = new ArrayList<>();
        for(ExperimentFeature experimentFeature: experimentFeatures){
            experimentIntensity.add(experimentFeature.getIntensity());
        }
        assert experimentIntensity.size() == libraryIntensity.size();

        //library_norm_manhattan
        float sum = 0.0f;
        float[] x = ScoreUtil.normalizeSum(libraryIntensity);
        float[] y = ScoreUtil.normalizeSum(experimentIntensity);
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
        float normalizedExperimentalRt = ScoreUtil.trafoApplier(slopeIntercept, experimentalRt);
        if(groupRt <= -1000f){
            scores.setVarNormRtScore(0);
        }else {
            scores.setVarNormRtScore(Math.abs(normalizedExperimentalRt - groupRt));
        }
    }

    public void calculateIntensityScore(List<ExperimentFeature> experimentFeatures, RTNormalizationScores scores){
        float intensitySum = 0.0f;
        for(ExperimentFeature feature: experimentFeatures){
            intensitySum += feature.getIntensity();
        }
        float totalXic = experimentFeatures.get(0).getTotalXic();
        scores.setVarIntensityScore(intensitySum / totalXic);
    }
}
