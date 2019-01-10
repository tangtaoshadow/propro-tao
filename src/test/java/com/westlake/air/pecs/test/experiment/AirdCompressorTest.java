package com.westlake.air.pecs.test.experiment;

import com.westlake.air.pecs.parser.AirdFileParser;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.ScanIndexService;
import com.westlake.air.pecs.test.BaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class AirdCompressorTest extends BaseTest {

    @Autowired
    AirdFileParser airdFileParser;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    ScanIndexService scanIndexService;

    @Test
    public void test(){
        String expId = "5bee5d97fc6f9e11c84b877f";
//        scanIndexService.getSwath

    }
}
