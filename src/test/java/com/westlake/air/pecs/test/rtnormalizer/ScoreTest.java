package com.westlake.air.pecs.test.rtnormalizer;

import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.score.*;
import com.westlake.air.pecs.feature.*;
import com.westlake.air.pecs.rtnormalizer.RtNormalizerScorer;
import com.westlake.air.pecs.test.BaseTest;
import com.westlake.air.pecs.utils.FileUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-09-04 19:15
 */
public class ScoreTest extends BaseTest {
    @Autowired
    GaussFilter gaussFilter;
    @Autowired
    PeakPicker peakPicker;
    @Autowired
    ChromatogramPicker chromatogramPicker;
    @Autowired
    SignalToNoiseEstimator signalToNoiseEstimator;
    @Autowired
    FeatureFinder featureFinder;
    @Autowired
    RtNormalizerScorer rtNormalizerScorer;

}
