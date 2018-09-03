package com.westlake.air.pecs.scorer;

import com.westlake.air.pecs.domain.bean.score.ExperimentFeature;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.utils.ScoreUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * scores.library_corr
 * scores.library_norm_manhattan
 *
 * scores.var_intensity_score
 *
 * Created by Nico Wang Ruimin
 * Time: 2018-08-15 16:06
 */
@Component("libraryScorer")
public class LibraryScorer {
    /**
     * scores.library_corr //对experiment和library intensity算Pearson相关系数
     * scores.library_norm_manhattan // 对experiment intensity 算平均占比差距
     *
     * @param experimentFeatures get experimentIntensity: from features extracted
     * @param libraryIntensity get libraryIntensity: from transitions
     * @param scores library_corr, library_norm_manhattan
     */
    public void calculateLibraryScores(List<ExperimentFeature> experimentFeatures, List<Float> libraryIntensity, SlopeIntercept slopeIntercept, double groupRt, FeatureScores scores){
        List<Double> experimentIntensity = new ArrayList<>();
        for(ExperimentFeature experimentFeature: experimentFeatures){
            experimentIntensity.add(experimentFeature.getIntensity());
        }
        assert experimentIntensity.size() == libraryIntensity.size();

        //library_norm_manhattan
        //平均占比差距
        float sum = 0.0f;
        float[] x = ScoreUtil.normalizeSum(libraryIntensity);
        float[] y = ScoreUtil.normalizeSumDouble(experimentIntensity);
        for(int i=0; i<x.length; i++){
            sum += Math.abs(x[i] - y[i]);
        }
        scores.setVarLibraryRsmd(sum / x.length);

        //library_corr
        //pearson 相关系数
        double corr = 0.0d, m1 = 0.0d, m2 = 0.0d, s1 = 0.0d, s2 = 0.0d;
        for(int i=0;i<libraryIntensity.size(); i++){
            corr += experimentIntensity.get(i) * libraryIntensity.get(i); //corr
            m1 += experimentIntensity.get(i); //sum of experiment
            m2 += libraryIntensity.get(i); //sum of library
            s1 += experimentIntensity.get(i) * experimentIntensity.get(i);// experiment ^2
            s2 += libraryIntensity.get(i) * libraryIntensity.get(i); // library ^2
        }
        m1 /= experimentIntensity.size(); //mean experiment intensity
        m2 /= libraryIntensity.size(); //mean library intensity
        s1 -= m1 * m1 * libraryIntensity.size();
        s2 -= m2 * m2 * libraryIntensity.size();
        if(s1 < Math.pow(1,-12) || s2 < Math.pow(1,-12)){
            scores.setVarLibraryCorr(0.0d);
        }else {
            corr -= m1 * m2 * libraryIntensity.size();
            corr /= Math.sqrt(s1 * s2);
            scores.setVarLibraryCorr(corr);
        }

        //varNormRtScore
        double experimentalRt = experimentFeatures.get(0).getRt();
        double normalizedExperimentalRt = ScoreUtil.trafoApplier(slopeIntercept, experimentalRt);
        if(groupRt <= -1000d){
            scores.setVarNormRtScore(0);
        }else {
            scores.setVarNormRtScore(Math.abs(normalizedExperimentalRt - groupRt));
        }
    }

    /**
     * scores.var_intensity_score
     * sum of intensitySum:
     * totalXic
     */
    public void calculateIntensityScore(List<ExperimentFeature> experimentFeatures, FeatureScores scores){
        double intensitySum = 0.0d;
        for(ExperimentFeature feature: experimentFeatures){
            intensitySum += feature.getIntensity();
        }
        double totalXic = experimentFeatures.get(0).getTotalXic();
        scores.setVarIntensityScore((intensitySum / totalXic));
    }
}
