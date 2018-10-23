package com.westlake.air.pecs.test.algorithm;

import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.test.BaseTest;
import com.westlake.air.pecs.utils.CompressUtil;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CompressorTest extends BaseTest {

    @Autowired
    MzXMLParser mzXMLParser;

    @Test
    public void testSortedIntegerCompress(){

        int[] testArray = new int[10];
        for(int i=0;i<testArray.length;i++){
            testArray[i] = i*3;
        }

        testArray = CompressUtil.compressForSortedInt(testArray);
        testArray = CompressUtil.decompressForSortedInt(testArray);
        System.out.println(testArray.length);
        for(int j =0;j<testArray.length;j++){
            assert testArray[j] == j*3;
        }
    }

    @Test
    public void testUnsortedIntegerCompress(){

        int[] testArray = new int[10];
        testArray[0] = 123;
        testArray[1] = 23;
        testArray[2] = 1223;
        testArray[3] = 12233;
        testArray[4] = 123;
        testArray[5] = 3;
        testArray[6] = 11223;
        testArray[7] = 153;
        testArray[8] = 1923;
        testArray[9] = 1231;

        testArray = CompressUtil.compressForUnsortedInt(testArray);
        testArray = CompressUtil.decompressForUnsortedInt(testArray);
        System.out.println(testArray.length);
        assert testArray[0] == 123;
        assert testArray[1] == 23;
        assert testArray[2] == 1223;
        assert testArray[3] == 12233;
        assert testArray[4] == 123;
        assert testArray[5] == 3;
        assert testArray[6] == 11223;
        assert testArray[7] == 153;
        assert testArray[8] == 1923;
        assert testArray[9] == 1231;
    }
}
