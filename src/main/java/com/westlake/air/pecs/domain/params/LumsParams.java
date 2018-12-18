package com.westlake.air.pecs.domain.params;

import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import lombok.Data;

import java.util.HashSet;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-29 13:55
 */
@Data
public class LumsParams {

    ExperimentDO experimentDO;

    String overviewId;

    String iRtLibraryId;

    String libraryId;

    Float mzExtractWindow;

    Float rtExtractWindow;

    /**
     * iRT求得的斜率和截距
     */
    SlopeIntercept slopeIntercept;

    SigmaSpacing sigmaSpacing = SigmaSpacing.create();

    /**
     * 流程的创建人
     */
    String creator;

    HashSet<String> scoreTypes = new HashSet<>();

    boolean usedDIAScores = false;
    /**
     * 是否在卷积的时候同时完成选峰和打分
     * epps: extract, peakpick, score
     */
    boolean useEpps = false;

    public LumsParams(){
        scoreTypes.add(FeatureScores.ScoreType.IntensityScore.getTypeName());
        scoreTypes.add(FeatureScores.ScoreType.LibraryCorr.getTypeName());
        scoreTypes.add(FeatureScores.ScoreType.LibraryRsmd.getTypeName());
        scoreTypes.add(FeatureScores.ScoreType.LogSnScore.getTypeName());
        scoreTypes.add(FeatureScores.ScoreType.MassdevScore.getTypeName());
        scoreTypes.add(FeatureScores.ScoreType.MassdevScoreWeighted.getTypeName());
        scoreTypes.add(FeatureScores.ScoreType.XcorrShape.getTypeName());
        scoreTypes.add(FeatureScores.ScoreType.XcorrShapeWeighted.getTypeName());
        scoreTypes.add(FeatureScores.ScoreType.LibraryDotprod.getTypeName());
        scoreTypes.add(FeatureScores.ScoreType.LibraryManhattan.getTypeName());
        scoreTypes.add(FeatureScores.ScoreType.LibrarySangle.getTypeName());
        scoreTypes.add(FeatureScores.ScoreType.LibraryRootmeansquare.getTypeName());
        scoreTypes.add(FeatureScores.ScoreType.ManhattScore.getTypeName());
        scoreTypes.add(FeatureScores.ScoreType.NormRtScore.getTypeName());
        scoreTypes.add(FeatureScores.ScoreType.XcorrCoelution.getTypeName());
        scoreTypes.add(FeatureScores.ScoreType.XcorrCoelutionWeighted.getTypeName());
    }

}
