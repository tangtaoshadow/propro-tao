package com.westlake.air.pecs.test.rtnormalizer;

import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.test.BaseTest;
import com.westlake.air.pecs.feature.GaussFilter;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by Song Jian
 * Time: 2018-08-09 14-31
 */
public class GauseFilterTest extends BaseTest {

    @Autowired
    GaussFilter gauseFilter;

    @Test
    public void filterTest_1() {
        Float[] rtArray = new Float[5];
        Float[] intensityArray = new Float[5];
        for (int i = 0; i < 5; i++) {
             intensityArray[i] = 1.0F;
             rtArray[i]= 500.0F + 0.2F * i;
        }

        RtIntensityPairsDouble input = new RtIntensityPairsDouble(rtArray, intensityArray);
        RtIntensityPairsDouble output = null;
        output = gauseFilter.filter(input, new SigmaSpacing(1.0f/8, 0.01f));
        for (int i = 0; i < 5; i++) {
            assert isSimilar(output.getIntensityArray()[0], 1.0F, 1e-5F);
        }
    }

    @Test
    public void filterTest_2() {
        Float[] rtArray = new Float[9];
        Float[] intensityArray = new Float[9];
        for (int i = 0; i < 9; i++) {
            intensityArray[i] = 0.0F;
            rtArray[i]= 500.0F + 0.03F * i;
            if (i == 3) {
                intensityArray[i] = 1.0F;
            }
            if (i == 4) {
                intensityArray[i] = 0.8F;
            }
            if (i == 5) {
                intensityArray[i] = 1.2F;
            }
        }
        RtIntensityPairsDouble input = new RtIntensityPairsDouble(rtArray, intensityArray);
        RtIntensityPairsDouble output = null;
        output = gauseFilter.filter(input, new SigmaSpacing(0.2f/8, 0.01f));
        assert isSimilar(output.getIntensityArray()[0], 0.000734827F, 0.01F);
        assert isSimilar(output.getIntensityArray()[1], 0.0543746F, 1e-2F);
        assert isSimilar(output.getIntensityArray()[2], 0.298025F, 1e-2F);
        assert isSimilar(output.getIntensityArray()[3], 0.707691F, 1e-2F);
        assert isSimilar(output.getIntensityArray()[4], 0.8963F, 1e-2F);
        assert isSimilar(output.getIntensityArray()[5], 0.799397F, 1e-2F);
        assert isSimilar(output.getIntensityArray()[6], 0.352416F, 1e-2F);
        assert isSimilar(output.getIntensityArray()[7], 0.065132F, 1e-2F);
        assert isSimilar(output.getIntensityArray()[8], 0.000881793F, 1e-2F);
    }

    private boolean isSimilar(Double a, Float b, Float tolerance ) {
        Float TOLERANCE = tolerance;
        if (Math.abs(a-b) < TOLERANCE) {
            return true;
        } else {
            return false;
        }
    }
}
