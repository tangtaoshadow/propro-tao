package com.westlake.air.propro.scorer;

import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.ScoreType;
import com.westlake.air.propro.domain.bean.score.FeatureScores;
import com.westlake.air.propro.domain.bean.score.PeakGroup;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.utils.MathUtil;
import com.westlake.air.propro.utils.ScoreUtil;
import org.apache.commons.math3.util.FastMath;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
     * @param peakGroup get experimentIntensity: from features extracted
     * @param normedLibIntMap   get libraryIntensity: from transitions
     * @param scores             library_corr, library_norm_manhattan
     */
    public void calculateLibraryScores(PeakGroup peakGroup, HashMap<String, Double> normedLibIntMap, FeatureScores scores, HashSet<String> scoreTypes) {
        List<Double> experimentIntensity = new ArrayList<>(peakGroup.getIonIntensity().values());
        assert experimentIntensity.size() == normedLibIntMap.size();

        //library_norm_manhattan
        //占比差距平均
        List<Double> normedLibInt = new ArrayList<>(normedLibIntMap.values());
        double[] normedExpInt = ScoreUtil.normalizeSumDouble(experimentIntensity, peakGroup.getPeakGroupInt());
        if(scoreTypes == null || scoreTypes.contains(ScoreType.LibraryRsmd.getTypeName())){
            double sum = 0.0d;
            for (int i = 0; i < normedLibInt.size(); i++) {
                sum += Math.abs(normedLibInt.get(i) - normedExpInt[i]);
            }
            scores.put(ScoreType.LibraryRsmd,sum / normedLibInt.size());
        }


        double experimentSum = 0.0d, librarySum = 0.0d, experiment2Sum = 0.0d, library2Sum = 0.0d, dotprod = 0.0d;
        for (int i = 0; i < normedLibInt.size(); i++) {
            dotprod += experimentIntensity.get(i) * normedLibInt.get(i); //corr
            experimentSum += experimentIntensity.get(i); //sum of experiment
            librarySum += normedLibInt.get(i); //sum of library
            experiment2Sum += experimentIntensity.get(i) * experimentIntensity.get(i);// experiment ^2
            library2Sum += normedLibInt.get(i) * normedLibInt.get(i); // library ^2
        }
        //library_corr pearson 相关系数
        //需要的前置变量：dotprod, sum, 2sum
        if(scoreTypes == null || scoreTypes.contains(ScoreType.LibraryCorr.getTypeName())) {
            double expDeno = experiment2Sum - experimentSum * experimentSum / normedLibInt.size();
            double libDeno = library2Sum - librarySum * librarySum / normedLibInt.size();
            if (expDeno <= Constants.MIN_DOUBLE || libDeno <= Constants.MIN_DOUBLE){
                scores.put(ScoreType.LibraryCorr, 0d);
            }else {
                double pearsonR = dotprod - experimentSum * librarySum / normedLibInt.size();
                pearsonR /= FastMath.sqrt(expDeno * libDeno);
                if(Double.isNaN(pearsonR) || Double.isInfinite(pearsonR)){
                    System.out.println("");
                }
                scores.put(ScoreType.LibraryCorr, pearsonR);
            }

        }

        double[] expSqrt = new double[experimentIntensity.size()];
        double[] libSqrt = new double[normedLibInt.size()];
        for (int i = 0; i < expSqrt.length; i++) {
            expSqrt[i] = FastMath.sqrt(experimentIntensity.get(i));
            libSqrt[i] = FastMath.sqrt(normedLibInt.get(i));
        }

        //dotprodScoring
        //需要的前置变量：experimentSum, librarySum, expSqrt, libSqrt
        if(scoreTypes == null || scoreTypes.contains(ScoreType.LibraryDotprod.getTypeName())){
            double expVecNorm = FastMath.sqrt(experimentSum);
            double libVecNorm = FastMath.sqrt(librarySum);

            double[] expSqrtVecNormed = normalize(expSqrt, expVecNorm);
            double[] libSqrtVecNormed = normalize(libSqrt, libVecNorm);

            double sumOfMult = 0d;
            for (int i = 0; i < expSqrt.length; i++) {
                sumOfMult += expSqrtVecNormed[i] * libSqrtVecNormed[i];
            }
            scores.put(ScoreType.LibraryDotprod, sumOfMult);
        }

        //manhattan
        //需要的前置变量：expSqrt, libSqrt
        if(scoreTypes == null ||scoreTypes.contains(ScoreType.LibraryManhattan.getTypeName())){
            double expIntTotal = MathUtil.sum(expSqrt);
            double libIntTotal = MathUtil.sum(libSqrt);
            double[] expSqrtNormed = normalize(expSqrt, expIntTotal);
            double[] libSqrtNormed = normalize(libSqrt, libIntTotal);
            double sumOfDivide = 0;
            for (int i = 0; i < expSqrt.length; i++) {
                sumOfDivide += FastMath.abs(expSqrtNormed[i] - libSqrtNormed[i]);
            }
            scores.put(ScoreType.LibraryManhattan, sumOfDivide);
        }

        //spectral angle
        if(scoreTypes == null ||scoreTypes.contains(ScoreType.LibrarySangle.getTypeName())){
            double spectralAngle = FastMath.acos(dotprod / (FastMath.sqrt(experiment2Sum) * FastMath.sqrt(library2Sum)));
            scores.put(ScoreType.LibrarySangle, spectralAngle);
        }

        //root mean square

        if(scoreTypes == null ||scoreTypes.contains(ScoreType.LibraryRootmeansquare.getTypeName())){
            double rms = 0;
            for (int i = 0; i < normedLibInt.size(); i++) {
                rms += (normedLibInt.get(i) - normedExpInt[i]) * (normedLibInt.get(i) - normedExpInt[i]);
            }
            rms = Math.sqrt(rms / normedLibInt.size());
            scores.put(ScoreType.LibraryRootmeansquare, rms);
        }


    }

    public void calculateNormRtScore(PeakGroup peakGroup, SlopeIntercept slopeIntercept, double groupRt, FeatureScores scores) {
        //varNormRtScore
        double experimentalRt = peakGroup.getApexRt();
        double normalizedExperimentalRt = ScoreUtil.trafoApplier(slopeIntercept, experimentalRt);
        scores.put(ScoreType.NormRtScore, Math.abs(normalizedExperimentalRt - groupRt));
    }

    /**
     * scores.var_intensity_score
     * sum of intensitySum:
     * totalXic
     */
    public void calculateIntensityScore(PeakGroup peakGroup, FeatureScores scores) {
        double intensitySum = peakGroup.getPeakGroupInt();
        double totalXic = peakGroup.getTotalXic();
        scores.put(ScoreType.IntensityScore,(intensitySum / totalXic));
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
