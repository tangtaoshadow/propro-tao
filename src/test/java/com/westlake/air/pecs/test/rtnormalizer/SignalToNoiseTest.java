package com.westlake.air.pecs.test.rtnormalizer;

import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.feature.SignalToNoiseEstimator;
import com.westlake.air.pecs.test.BaseTest;
import com.westlake.air.pecs.utils.FileUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-30 17:01
 */
public class SignalToNoiseTest extends BaseTest {
    @Autowired
    SignalToNoiseEstimator signalToNoiseEstimator;

    @Test
    public void computeSTN() throws IOException {
        File fileTest = new File(PeakPickerTest.class.getClassLoader().getResource("data/signalToNoiseTest.txt").getPath());
        BufferedReader readerTest = new BufferedReader(new FileReader(fileTest));
        RtIntensityPairsDouble rtIntensityPairsDoubleTest = FileUtil.txtReader(readerTest," " ,0,1);

        File fileResult = new File(PeakPickerTest.class.getClassLoader().getResource("data/signalToNoiseOutput.txt").getPath());
        BufferedReader readerResult = new BufferedReader(new FileReader(fileResult));
        RtIntensityPairsDouble rtIntensityPairsDoubleResult = FileUtil.txtReader(readerResult, " ",0,1);

        double[] result = signalToNoiseEstimator.computeSTN(rtIntensityPairsDoubleTest, 40, 30);

        for(int i=0; i<result.length; i++){
            assert isSimilar(rtIntensityPairsDoubleResult.getIntensityArray()[i], result[i], Math.pow(10, -4));
        }
        System.out.println("computeSTN finished.");
    }

    private boolean isSimilar(Double a, Double b, Double tolerance ) {
        if (Math.abs(a-b) < tolerance) {
            return true;
        } else {
            return false;
        }
    }
}
