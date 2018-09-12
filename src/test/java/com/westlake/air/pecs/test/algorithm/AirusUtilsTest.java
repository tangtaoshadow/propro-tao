package com.westlake.air.pecs.test.algorithm;

import com.westlake.air.pecs.test.BaseTest;
import com.westlake.air.pecs.utils.AirusUtils;
import org.junit.Test;

public class AirusUtilsTest extends BaseTest {

    @Test
    public void argSortTest() {
        Double[] d = new Double[6];
        d[0] = 1.0d;
        d[1] = 3.0d;
        d[2] = 2.0d;
        d[3] = 3.0d;
        d[4] = 5.0d;
        d[5] = 4.0d;

        Integer[] argSort = AirusUtils.indexBeforeSort(d);

        assert argSort[0] == 0;
        assert argSort[1] == 2;
        assert argSort[2] == 1;
        assert argSort[3] == 3;
        assert argSort[4] == 5;
        assert argSort[5] == 4;
    }

    @Test
    public void argSortReverseTest() {
        Double[] d = new Double[6];
        d[0] = 1.0d;
        d[1] = 3.0d;
        d[2] = 2.0d;
        d[3] = 3.0d;
        d[4] = 5.0d;
        d[5] = 4.0d;

        Integer[] argSort = AirusUtils.indexBeforeReversedSort(d);

        assert argSort[0] == 4;
        assert argSort[1] == 5;
        assert argSort[2] == 3;
        assert argSort[3] == 1;
        assert argSort[4] == 2;
        assert argSort[5] == 0;
    }
}
