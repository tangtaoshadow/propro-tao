package com.westlake.air.pecs.test.rtnormalizer;

import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.feature.PeakPicker;
import com.westlake.air.pecs.feature.SignalToNoiseEstimator;
import com.westlake.air.pecs.test.BaseTest;
import com.westlake.air.pecs.utils.FileUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-30 15:40
 */
public class PeakPickerTest extends BaseTest {
    @Autowired
    PeakPicker peakPicker;
    @Autowired
    SignalToNoiseEstimator signalToNoiseEstimator;

    @Test
    public void peakPickerTest_1 ()throws IOException {

        File fileTest = new File(PeakPickerTest.class.getClassLoader().getResource("data/peakPickerTest.txt").getPath());
        BufferedReader readerTest = new BufferedReader(new FileReader(fileTest));
        RtIntensityPairsDouble rtIntensityPairsDoubleTest = FileUtil.txtReader(readerTest,"\t", 1,2);

        File fileResult = new File(PeakPickerTest.class.getClassLoader().getResource("data/peakPickerOutput.txt").getPath());
        BufferedReader readerResult = new BufferedReader(new FileReader(fileResult));
        RtIntensityPairsDouble rtIntensityPairsDoubleResult = FileUtil.txtReader(readerResult,"\t", 1, 2);
        System.out.println("test Begin");

        double[] signalToNoise200 = signalToNoiseEstimator.computeSTN(rtIntensityPairsDoubleTest, 200, 30);

        RtIntensityPairsDouble pickResult = peakPicker.pickMaxPeak(rtIntensityPairsDoubleTest, signalToNoise200);

        System.out.println("pick Finish");
    }

}
