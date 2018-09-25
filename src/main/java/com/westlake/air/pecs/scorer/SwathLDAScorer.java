package com.westlake.air.pecs.scorer;

import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import org.springframework.stereotype.Component;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-19 21:05
 */
@Component("swathLDAScorer")
public class SwathLDAScorer {

    /**
     * -scores.calculate_swath_lda_prescore
     * @param scores
     * @return
     */
    public void calculateSwathLdaPrescore(FeatureScores scores){
        scores.setMainVarXxSwathPrelimScore(
                scores.getVarLibraryCorr()              * -0.19011762 +
                scores.getVarLibraryRsmd()              *  2.47298914 +
                scores.getVarNormRtScore()              *  5.63906731 +
                scores.getVarIsotopeCorrelationScore()  * -0.62640133 +
                scores.getVarIsotopeOverlapScore()      *  0.36006925 +
                scores.getVarMassdevScore()             *  0.08814003 +
                scores.getVarXcorrCoelution()           *  0.13978311 +
                scores.getVarXcorrShape()               * -1.16475032 +
                scores.getVarYseriesScore()             * -0.19267813 +
                scores.getVarLogSnScore()               * -0.61712054);
    }
}
