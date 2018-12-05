package com.westlake.air.pecs.test.algorithm;

import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.test.BaseTest;
import com.westlake.air.pecs.utils.CompressUtil;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

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
}
