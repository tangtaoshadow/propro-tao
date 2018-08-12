package com.westlake.air.pecs.test.algorithm;

import com.westlake.air.pecs.algorithm.Airus;
import com.westlake.air.pecs.domain.bean.airus.FinalResult;
import com.westlake.air.pecs.domain.bean.airus.ScoreData;
import com.westlake.air.pecs.parser.ScoreTsvParser;
import com.westlake.air.pecs.test.BaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-12 21:18
 */
public class AirusTest extends BaseTest {

    @Autowired
    Airus airus;
    @Autowired
    ScoreTsvParser scoreTsvParser;

    @Test
    public void airusTest(){
        ScoreData scoreDataMap = scoreTsvParser.getScoreData(new File(this.getClass().getClassLoader().getResource("scores_data.tsv").getPath()));
        if(scoreDataMap != null) {
            long startTime = System.currentTimeMillis();
            FinalResult finalResult = airus.buildResult(scoreDataMap);
            System.out.println(finalResult.getSummaryErrorTable());
            long endTime = System.currentTimeMillis();
            System.out.println(endTime-startTime);
            assert true;
        }
    }
}
