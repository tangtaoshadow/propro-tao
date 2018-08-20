package com.westlake.air.pecs.test.extractor;

import com.westlake.air.pecs.parser.MzMLParser;
import com.westlake.air.pecs.parser.TransitionTraMLParser;
import com.westlake.air.pecs.test.BaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-20 14:11
 */
public class ExtractorTest extends BaseTest {

    @Autowired
    MzMLParser mzMLParser;
    @Autowired
    TransitionTraMLParser traMLParser;

    @Test
    public void extractorTest_1() {

    }

}
