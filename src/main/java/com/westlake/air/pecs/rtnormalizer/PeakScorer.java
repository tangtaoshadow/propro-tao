package com.westlake.air.pecs.rtnormalizer;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.westlake.air.pecs.dao.AminoAcidDAO;
import com.westlake.air.pecs.dao.UnimodDAO;
import com.westlake.air.pecs.domain.bean.math.BisectionLowHigh;
import com.westlake.air.pecs.domain.bean.score.*;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.dao.ElementsDAO;
import com.westlake.air.pecs.domain.bean.*;
import com.westlake.air.pecs.domain.bean.transition.Annotation;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.simple.IntensityGroup;
import com.westlake.air.pecs.domain.db.simple.TransitionGroup;
import com.westlake.air.pecs.parser.model.chemistry.AminoAcid;
import com.westlake.air.pecs.parser.model.chemistry.Element;
import com.westlake.air.pecs.parser.model.chemistry.Unimod;
import com.westlake.air.pecs.scorer.ChromatograpicScorer;
import com.westlake.air.pecs.scorer.LibraryScorer;
import com.westlake.air.pecs.utils.MathUtil;
import org.jcp.xml.dsig.internal.DigesterOutputStream;
import org.jcp.xml.dsig.internal.dom.DOMUtils;
import org.omg.PortableServer.ServantLocatorPOA;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-05 17:18
 */
@Component("peakScorer")
public class PeakScorer {

//    private float windowLength = 1000;
//    private int binCount = 30;

//    private float rtNormalizationFactor = 1.0f;
//    private int addUpSpectra = 1;
//    private float spacingForSpectraResampling = 0.005f;
//    //score use params


    /**
     *        return scores.library_corr                     * -0.34664267 +
     *               scores.library_norm_manhattan           *  2.98700722 +
     *               scores.norm_rt_score                    *  7.05496384 +
     *               scores.xcorr_coelution_score            *  0.09445371 +
     *               scores.xcorr_shape_score                * -5.71823862 +
     *               scores.log_sn_score                     * -0.72989582 +
     *               scores.elution_model_fit_score          *  1.88443209;
     * @param chromatograms chromatogramList in transitionGroup
     * @param experimentFeatures features extracted from chromatogramList in transitionGroup
     * @param libraryIntensity intensity in transitionList in transitionGroup
     * @return List of overallQuality
     */
    public List<ScoreRtPair> score(List<RtIntensityPairs> chromatograms, List<List<ExperimentFeature>> experimentFeatures, List<Float> libraryIntensity, SlopeIntercept slopeIntercept, float groupRt, float windowLength, int binCount){

        //get signal to noise list
        List<float[]> signalToNoiseList = new ArrayList<>();
        for(RtIntensityPairs chromatogram: chromatograms) {
            float[] signalToNoise = new SignalToNoiseEstimator().computeSTN(chromatogram, windowLength, binCount);
            signalToNoiseList.add(signalToNoise);
        }

        List<ScoreRtPair> finalScores = new ArrayList<>();
        for(List<ExperimentFeature> features: experimentFeatures){
            RTNormalizationScores scores = new RTNormalizationScores();
            new ChromatograpicScorer().calculateChromatographicScores(chromatograms, features, libraryIntensity, signalToNoiseList, scores);
            new LibraryScorer().calculateLibraryScores(features,libraryIntensity, scores, slopeIntercept, groupRt);
            float ldaScore = calculateLdaPrescore(scores);
            ScoreRtPair scoreRtPair = new ScoreRtPair();
            scoreRtPair.setRt(features.get(0).getRt());
            scoreRtPair.setScore(ldaScore);
            finalScores.add(scoreRtPair);
        }

        return finalScores;
    }


    /**
     * The score that is really matter to final pairs selection.
     * @param scores pre-calculated
     * @return final score
     */
    private float calculateLdaPrescore(RTNormalizationScores scores){
        return  scores.getVarLibraryCorr()              * -0.34664267f +
                scores.getVarLibraryRsmd()              *  2.98700722f +
                scores.getVarXcorrCoelution()           *  0.09445371f +
                scores.getVarXcorrShape()               * -5.71823862f +
                scores.getVarLogSnScore()               * -0.72989582f;
    }


    private void calculateElutionModelScore(List<ExperimentFeature> experimentFeatures){
        for(ExperimentFeature feature: experimentFeatures){
            RtIntensityPairs hullPoints = prepareElutionFit(feature);
            float sum = 0.0f;
            Float[] intArray = hullPoints.getIntensityArray();
            Float[] rtArray = hullPoints.getRtArray();
            for(float intens: intArray){
                sum += intens;
            }

            int medianIndex = 0;
            float count = 0f;
            for(int i=0; i<intArray.length; i++){
                count += intArray[i];
                if(count > sum /2f){
                    medianIndex = i-1;
                    break;
                }
            }
            float height = intArray[medianIndex];
            float retention = rtArray[medianIndex];
            boolean symmetric = false;
            float symmetry;
            if(rtArray[medianIndex] - rtArray[0] == 0f){
                symmetric = true;
                symmetry = 10;
            }else {
                symmetry = Math.abs((rtArray[rtArray.length - 1] - rtArray[medianIndex]) / (rtArray[medianIndex] - rtArray[0]));
            }
            if(symmetry < 1){
                symmetry +=5;
            }
            float width = symmetry;
            if(!symmetric){

            }

        }

    }

    private RtIntensityPairs prepareElutionFit(ExperimentFeature feature){
        List<Float> rtArray = feature.getHullRt();
        List<Float> intArray = feature.getHullInt();


        //get rt distance average
        float sum = rtArray.get(rtArray.size()-1) - rtArray.get(0);
        float rtDistanceAverage = sum / (rtArray.size() - 1);
        float rightSideRt = rtArray.get(rtArray.size()-1) + rtDistanceAverage;
        float leftSideRt = rtArray.get(0) - rtDistanceAverage;

        //get new List
        Float[] newRtArray = new Float[rtArray.size() + 6];
        Float[] newIntArray = new Float[intArray.size() + 6];
        assert intArray.size() == rtArray.size();

        for(int i=0; i<newRtArray.length; i++){
            if(i<3){
                newRtArray[i] = leftSideRt;
                newIntArray[i] = 0.0f;
            } else if(i>newRtArray.length - 4){
                newRtArray[i] = rightSideRt;
                newIntArray[i] = 0.0f;
            } else {
                newRtArray[i] = rtArray.get(i - 3);
                newIntArray[i] = intArray.get(i - 3);
            }
        }

        RtIntensityPairs rtIntensityPairs = new RtIntensityPairs();
        rtIntensityPairs.setRtArray(newRtArray);
        rtIntensityPairs.setIntensityArray(newIntArray);

        return rtIntensityPairs;
    }


}
