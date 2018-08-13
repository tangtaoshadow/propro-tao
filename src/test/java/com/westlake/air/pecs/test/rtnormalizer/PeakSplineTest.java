package com.westlake.air.pecs.test.rtnormalizer;

import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import com.westlake.air.pecs.rtnormalizer.PeakSpline;
import com.westlake.air.pecs.test.BaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by Song Jian
 * Time: 2018-08-13 17-22
 */
public class PeakSplineTest extends BaseTest {
    @Test
    public void evalTest() {
        PeakSpline peakSpline1 = new PeakSpline();
        Float[] rt = {486.784F, 486.787F, 486.790F, 486.793F, 486.795F, 486.797F, 486.800F, 486.802F, 486.805F, 486.808F, 486.811F};
        Float[] intensity = {0.0F, 154683.17F, 620386.5F, 1701390.12F, 2848879.25F, 3564045.5F, 2744585.7F, 1605583.0F, 1518984.0F, 1591352.21F, 1691345.1F};
        RtIntensityPairs input = new RtIntensityPairs(rt, intensity);
        peakSpline1.init(input, 0, 10);
        float tmp = peakSpline1.eval(486.785f);
        assert isSimilar(tmp, 35173.1841778984f, 1.0f);
        assert isSimilar(peakSpline1.eval(486.7794f), 2271426.93316241f, 1.0f);
        assert isSimilar(peakSpline1.eval(486.784f), 0.0f, 1.0f);
        assert isSimilar(peakSpline1.eval(486.790f), 620386.5f, 1.0f);
        assert isSimilar(peakSpline1.eval(486.808f), 1591352.21f, 1.0f);
        assert isSimilar(peakSpline1.eval(486.811f), 1691345.1f, 1.0f);

        PeakSpline peakSpline2 = new PeakSpline();
        Float[] x = new Float[11];
        Float[] y = new Float[11];
        Float x_min = -0.5F;
        Float x_max = 1.5F;
        for (int i = 0; i < 11; i++) {
            x[i] = (x_min + i / 10.0F * (x_max - x_min));
            y[i] = Float.valueOf((float)Math.sin((x_min + i / 10.0F * (x_max - x_min))));
        }
        RtIntensityPairs input2 = new RtIntensityPairs(x, y);
        peakSpline2.init(input2, 0, 10);
        for (int i = 0; i < 11; i++) {
            assert isSimilar(peakSpline2.eval(x[i]), y[i], 1.0f);
        }

        for (int i = 0; i < 16; i++) {
            float xx = x_min + i/15.0f*(x_max-x_min);
            assert isSimilar(peakSpline2.eval(xx), (float)Math.sin(xx), 1.0f);
        }
    }

    @Test
    public void derivativesTest() {
        PeakSpline peakSpline1 = new PeakSpline();
        Float[] rt = {486.784F, 486.787F, 486.790F, 486.793F, 486.795F, 486.797F, 486.800F, 486.802F, 486.805F, 486.808F, 486.811F};
        Float[] intensity = {0.0F, 154683.17F, 620386.5F, 1701390.12F, 2848879.25F, 3564045.5F, 2744585.7F, 1605583.0F, 1518984.0F, 1591352.21F, 1691345.1F};
        RtIntensityPairs input = new RtIntensityPairs(rt, intensity);
        peakSpline1.init(input, 0, 10);
        float tmp = peakSpline1.derivatives(486.785f);
        assert isSimilar(peakSpline1.derivatives(486.785f), 39270152.2996247f, 1.0f);
        assert isSimilar(peakSpline1.derivatives(486.794f), 594825947.154264f, 1.0f);

        PeakSpline peakSpline2 = new PeakSpline();
        Float[] x = new Float[11];
        Float[] y = new Float[11];
        Float x_min = -0.5F;
        Float x_max = 1.5F;
        for (int i = 0; i < 11; i++) {
            x[i] = (x_min + i / 10.0F * (x_max - x_min));
            y[i] = Float.valueOf((float)Math.sin((x_min + i / 10.0F * (x_max - x_min))));
        }
        RtIntensityPairs input2 = new RtIntensityPairs(x, y);
        peakSpline2.init(input2, 0, 10);
        for (int i = 2; i < 9; i++) {
            assert isSimilar(peakSpline2.derivatives(x[i]), (float)Math.cos(x[i]), 1.0f);
        }
        for (int i = 2; i < 14; i++) {
            float xx = x_min + i/15.0f*(x_max-x_min);
            assert isSimilar(peakSpline2.derivatives(xx), (float)Math.cos(xx), 1.0f);
        }

        // TODO:test boundary conditions y'' = 0
//        assert isSimilar(peakSpline2.derivatives(x[0], 2), 0.0f, 1.0f);
//        assert isSimilar(peakSpline2.derivatives(x[10], 2), 0.0f, 1.0f);
    }

    private boolean isSimilar(float a, float b, float tolerance ) {
        float TOLERANCE = tolerance;
        if (Math.abs(a-b) < TOLERANCE) {
            return true;
        } else {
            return false;
        }
    }
}
