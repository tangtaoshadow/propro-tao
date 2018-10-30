package com.westlake.air.pecs.scorer;

import com.westlake.air.pecs.domain.bean.score.ExperimentFeature;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.utils.ArrayUtil;
import com.westlake.air.pecs.utils.ScoreUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * scores.library_corr
 * scores.library_norm_manhattan
 * <p>
 * scores.var_intensity_score
 * <p>
 * Created by Nico Wang Ruimin
 * Time: 2018-08-15 16:06
 */
@Component("libraryScorer")
public class LibraryScorer {
    /**
     * scores.library_corr //对experiment和library intensity算Pearson相关系数
     * scores.library_norm_manhattan // 对experiment intensity 算平均占比差距
     * scores.norm_rt_score //normalizedExperimentalRt与groupRt之差
     *
     * @param experimentFeatures get experimentIntensity: from features extracted
     * @param libraryIntensity   get libraryIntensity: from transitions
     * @param scores             library_corr, library_norm_manhattan
     */
    public void calculateLibraryScores(List<ExperimentFeature> experimentFeatures, List<Double> libraryIntensity, FeatureScores scores) {
        List<Double> experimentIntensity = new ArrayList<>();
        for (ExperimentFeature experimentFeature : experimentFeatures) {
            experimentIntensity.add(experimentFeature.getIntensity());
        }
        assert experimentIntensity.size() == libraryIntensity.size();

        //library_norm_manhattan
        //占比差距平均
        double sum = 0.0d;
        double[] x = ScoreUtil.normalizeSumDouble(libraryIntensity);
        double[] y = ScoreUtil.normalizeSumDouble(experimentIntensity);
        for (int i = 0; i < x.length; i++) {
            sum += Math.abs(x[i] - y[i]);
        }
        scores.put(FeatureScores.ScoreType.VarLibraryRsmd,sum / x.length);

        //library_corr
        //pearson 相关系数
        double corr = 0.0d, m1 = 0.0d, m2 = 0.0d, s1 = 0.0d, s2 = 0.0d;
        for (int i = 0; i < libraryIntensity.size(); i++) {
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
        if (s1 < Math.pow(10, -12) || s2 < Math.pow(10, -12)) {
            scores.put(FeatureScores.ScoreType.VarLibraryCorr, 0.0d);
        } else {
            corr -= m1 * m2 * libraryIntensity.size();
            corr /= Math.sqrt(s1 * s2);
            scores.put(FeatureScores.ScoreType.VarLibraryCorr, corr);
        }

        //dotprodScoring
        double[] expIntSqrt = new double[experimentIntensity.size()];
        double[] libIntSqrt = new double[libraryIntensity.size()];
        for (int i = 0; i < expIntSqrt.length; i++) {
            expIntSqrt[i] = Math.sqrt(experimentIntensity.get(i));
            libIntSqrt[i] = Math.sqrt(libraryIntensity.get(i));
        }
        double expIntNorm = norm(expIntSqrt);
        double libIntNorm = norm(libIntSqrt);

        double[] expIntSqrtDivided = normalize(expIntSqrt, expIntNorm);
        double[] libIntSqrtDivided = normalize(libIntSqrt, libIntNorm);

        double sumOfMult = 0;
        for (int i = 0; i < expIntSqrt.length; i++) {
            sumOfMult += expIntSqrtDivided[i] * libIntSqrtDivided[i];
        }
        scores.put(FeatureScores.ScoreType.VarLibraryDotprod, sumOfMult);

        //manhattan
        double expIntTotal = ArrayUtil.sumArray(expIntSqrt);
        double libIntTotal = ArrayUtil.sumArray(libIntSqrt);
        expIntSqrtDivided = normalize(expIntSqrt, expIntTotal);
        libIntSqrtDivided = normalize(libIntSqrt, libIntTotal);
        double sumOfDivide = 0;
        for (int i = 0; i < expIntSqrt.length; i++) {
            sumOfDivide += Math.abs(expIntSqrtDivided[i] - libIntSqrtDivided[i]);
        }
        scores.put(FeatureScores.ScoreType.VarLibraryManhattan, sumOfDivide);

        //spectral angle
        double dotprod = 0, xLen = 0, yLen = 0;
        for (int i = 0; i < libraryIntensity.size(); i++) {
            dotprod += experimentIntensity.get(i) * libraryIntensity.get(i);
            xLen += experimentIntensity.get(i) * experimentIntensity.get(i);
            yLen += libraryIntensity.get(i) * libraryIntensity.get(i);
        }
        double spectralAngle = Math.acos(dotprod / (Math.sqrt(xLen) * Math.sqrt(yLen)));
        scores.put(FeatureScores.ScoreType.VarLibrarySangle, spectralAngle);

        //root mean square
        if (x.length == 0) {
            scores.put(FeatureScores.ScoreType.VarLibraryRootmeansquare, 0d);
        } else {
            double rms = 0;
            for (int i = 0; i < x.length; i++) {
                rms += (x[i] - y[i]) * (x[i] - y[i]);
            }
            rms = Math.sqrt(rms / x.length);
            scores.put(FeatureScores.ScoreType.VarLibraryRootmeansquare, rms);
        }


    }

    public void calculateNormRtScore(List<ExperimentFeature> experimentFeatures, SlopeIntercept slopeIntercept, double groupRt, FeatureScores scores) {
        //varNormRtScore
        double experimentalRt = experimentFeatures.get(0).getRt();
        double normalizedExperimentalRt = ScoreUtil.trafoApplier(slopeIntercept, experimentalRt);
        if (groupRt <= -1000d) {
            scores.put(FeatureScores.ScoreType.VarNormRtScore, 0d);
        } else {
            scores.put(FeatureScores.ScoreType.VarNormRtScore, Math.abs(normalizedExperimentalRt - groupRt));
        }
    }

    /**
     * scores.var_intensity_score
     * sum of intensitySum:
     * totalXic
     */
    public void calculateIntensityScore(List<ExperimentFeature> experimentFeatures, FeatureScores scores) {
        double intensitySum = experimentFeatures.get(0).getIntensitySum();
        double totalXic = experimentFeatures.get(0).getTotalXic();
        scores.put(FeatureScores.ScoreType.VarIntensityScore,(intensitySum / totalXic));
    }

    private double norm(double[] array) {
        double sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i] * array[i];
        }
        return Math.sqrt(sum);
    }

    private double[] normalize(double[] array, double value) {
        if (value > 0) {
            for (int i = 0; i < array.length; i++) {
                array[i] /= value;
            }
        }
        return array;
    }

}
