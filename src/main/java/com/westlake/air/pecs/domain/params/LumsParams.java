package com.westlake.air.pecs.domain.params;

import com.westlake.air.pecs.constants.ScoreType;
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

    boolean usedDIAScores = true;
    /**
     * 是否在卷积的时候同时完成选峰和打分
     * epps: extract, peakpick, scoreForAll
     */
    boolean useEpps = false;

    public LumsParams(){
        scoreTypes.add(ScoreType.IntensityScore.getTypeName());
        scoreTypes.add(ScoreType.LibraryCorr.getTypeName());
        scoreTypes.add(ScoreType.LibraryRsmd.getTypeName());
        scoreTypes.add(ScoreType.LogSnScore.getTypeName());
        scoreTypes.add(ScoreType.MassdevScore.getTypeName());
        scoreTypes.add(ScoreType.MassdevScoreWeighted.getTypeName());
        scoreTypes.add(ScoreType.XcorrShape.getTypeName());
        scoreTypes.add(ScoreType.XcorrShapeWeighted.getTypeName());
        scoreTypes.add(ScoreType.LibraryDotprod.getTypeName());
        scoreTypes.add(ScoreType.LibraryManhattan.getTypeName());
        scoreTypes.add(ScoreType.LibrarySangle.getTypeName());
        scoreTypes.add(ScoreType.LibraryRootmeansquare.getTypeName());
        scoreTypes.add(ScoreType.ManhattScore.getTypeName());
        scoreTypes.add(ScoreType.NormRtScore.getTypeName());
        scoreTypes.add(ScoreType.XcorrCoelution.getTypeName());
        scoreTypes.add(ScoreType.XcorrCoelutionWeighted.getTypeName());
    }

}
