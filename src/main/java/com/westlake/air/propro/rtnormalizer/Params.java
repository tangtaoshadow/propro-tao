package com.westlake.air.propro.rtnormalizer;

import lombok.Data;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-29 07-26
 */

@Data
public class Params {

//    float confidenceInterval = 0.95f;

    float minRsq = 0.95f;

    float minCoverage = 0.6f;

    //MRMFeatureFinderScoring
    float rtExtractionWindow = -1.0f;
    float rtNormalizationFactor = 100.0f;
    float quantificationCutoff = 0.0f;
    boolean writeConvexHull = false;
//    int addUpSpectra = 1;
//    float spacingForSpectraResampling = 0.005f;
    int uisThresholdSn = 0;
    int uisThresholdPeakArea = 0;

    //MRMTransitionGroupPicker
//    int stopAfterFeature = -1;
//    float stopAfterIntensityRatio = 0.0001f;
    float minPeakWidth = 14.0f;
    String peakIntegration = "original";
    String backgroundSubtraction = "none";
    boolean recalculatePeaks = true;
    boolean usePrecursors = false;
    float recalculatePeaksMaxZ = 0.75f;
    float minimal_quality = -1.5f;
    float resampleBoundary = 15.0f;
    boolean computePeakQuality = false;
    boolean computePeakShapeMetrics = false;

    //PeakPickerMRM
    int sgolayFrameLength = 11;
    int sgolayPolynomialOrder = 3;
    float gaussWidth = 30.0f;
    boolean useGauss = false;
    float peakWidth = -1.0f;
    float signalToNoise = 1.0f;
//    float snWinLen = 1000.0f;
//    int snBinCount = 30;
    boolean writeSnLogMessages = false;
    boolean removeOverlappingPeaks = true;
    String method = "corrected";

    //DIAScoring
    float diaExtractionWindow = 0.05f;
    String diaExtractionUnit = "Th";
    boolean diaCentroided = false;
    float diaByseriesIntensityMin = 300.0f;
    float diaByseriesPpmDiff = 10.0f;
    int diaNrIsotopes = 4;
    int diaNrCharges = 4;
    float peakBeforeMonoMaxPpmDiff = 20.0f;

    //EMGScoring
    int maxIteration = 10;


    //scores
    boolean useShapeScore = true;
    boolean useCoelutionScore = true;
    boolean useRtScore = false;
    boolean useLibraryScore = true;
    boolean useElutionModelScore = false;
    boolean useIntensityScore = true;
    boolean useNrPeaksScore = true;
    boolean useTotalXicScore = true;
    boolean useSnScore = true;
    boolean useDiaScore = true;
    boolean useMs1Correlation = false;
    boolean useSonarScores = false;
    boolean useMs1Fullscan = false;
    boolean useUisScores = false;

    //RTNormalization
    String alignmentMethod = "linear";
    float span = (float)(2.0/3);
    int numNodes = 5;
    String outlierMethod = "iter_residual";
//    boolean useIterativeChauvenet = false;
    int ransacMaxIterations = 1000;
    int ransacMaxPercentRtThreshold = 3;
    int ransacSamplingSize = 10;
    boolean estimateBestPeptides = false;
    float initialQualityCutoff = 0.5f;
    float overallQualityCutoff = 5.5f;
    int nrRtBins = 10;
    int minPeptidesPerBin = 1;
    int minBinsFilled = 8;

}
