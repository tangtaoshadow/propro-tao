package com.westlake.air.propro.domain.params;

import com.westlake.air.propro.constants.ScoreType;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.ExperimentDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-29 13:55
 */
@Data
public class LumsParams {

    ExperimentDO experimentDO;

    String overviewId;

    LibraryDO iRtLibrary;

    LibraryDO library;

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
    String ownerName;

    /**
     * 用于打分的子分数模板快照,会和AnalyseDataDO中的每一个FeatureScore中的scores对象做一一映射
     */
    List<String> scoreTypes = new ArrayList<>();

    /**
     * 是否使用DIA打分,如果使用DIA打分的话,需要提前读取Aird文件中的谱图信息以提升系统运算速度
     */
    boolean usedDIAScores = true;

    /**
     * 是否在卷积的时候同时完成选峰和打分
     * epps: extract, peakpick, scoreForAll
     */
    boolean useEpps = false;

    boolean uniqueOnly = false;

    /**
     * shape的筛选阈值,一般建议在0.6左右
     */
    Float xcorrShapeThreshold;
    /**
     * shape的筛选阈值,一般建议在0.8左右
     */
    Float xcorrShapeWeightThreshold;

    //上下文备忘录
    String note;

    //用于PRM, <precursor mz, [rt start, rt end]>
    HashMap<Float, Float[]> rtRangeMap;

    public LumsParams(){
        scoreTypes.add(ScoreType.MainScore.getTypeName()); //存储的第一个位置默认存放的是MainScore
        scoreTypes.add(ScoreType.WeightedTotalScore.getTypeName()); //存储的第一个位置默认存放的是MainScore
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
