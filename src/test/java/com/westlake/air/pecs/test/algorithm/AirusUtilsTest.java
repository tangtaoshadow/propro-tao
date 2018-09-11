package com.westlake.air.pecs.test.algorithm;

import com.westlake.air.pecs.test.BaseTest;
import com.westlake.air.pecs.utils.AirusUtils;
import org.junit.Test;

public class AirusUtilsTest extends BaseTest {

    @Test
    public void argSortTest(){
        Double[] d = new Double[5];
        d[0] = 1.0d;
        d[1] = 3.0d;
        d[2] = 2.0d;
        d[3] = 5.0d;
        d[4] = 4.0d;

        Integer[] argSort = AirusUtils.indexBeforeSort(d);

        assert argSort[0] == 0;
        assert argSort[1] == 2;
        assert argSort[2] == 1;
        assert argSort[3] == 4;
        assert argSort[4] == 3;
    }

    @Test
    public void argSortReverseTest(){
        Double[] d = new Double[5];
        d[0] = 1.0d;
        d[1] = 3.0d;
        d[2] = 2.0d;
        d[3] = 5.0d;
        d[4] = 4.0d;

        Integer[] argSort = AirusUtils.indexBeforeReversedSort(d);

        assert argSort[0] == 3;
        assert argSort[1] == 4;
        assert argSort[2] == 1;
        assert argSort[3] == 2;
        assert argSort[4] == 0;
    }
}
