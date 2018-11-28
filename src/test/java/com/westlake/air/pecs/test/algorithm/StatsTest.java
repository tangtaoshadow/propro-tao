package com.westlake.air.pecs.test.algorithm;

import com.westlake.air.pecs.algorithm.Stats;
import com.westlake.air.pecs.domain.bean.score.SimpleFeatureScores;
import com.westlake.air.pecs.test.BaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class StatsTest extends BaseTest {

    @Autowired
    Stats stats;

    @Test
    public void qvalue_test() {
        Double[] d = new Double[5];
        d[0] = 0.2d;
        d[1] = 0.3d;
        d[2] = 0.4d;
        d[3] = 0.4d;
        d[4] = 0.6d;
        Double[] result1 = stats.qvalue(d, 0.2, false);

        List<SimpleFeatureScores> scoresList = new ArrayList<>();

        SimpleFeatureScores score0 = new SimpleFeatureScores();
        score0.setPValue(0.2d);

        SimpleFeatureScores score1 = new SimpleFeatureScores();
        score1.setPValue(0.3d);

        SimpleFeatureScores score2 = new SimpleFeatureScores();
        score2.setPValue(0.4d);

        SimpleFeatureScores score3 = new SimpleFeatureScores();
        score3.setPValue(0.4d);

        SimpleFeatureScores score4 = new SimpleFeatureScores();
        score4.setPValue(0.6d);

        scoresList.add(score0);
        scoresList.add(score1);
        scoresList.add(score2);
        scoresList.add(score3);
        scoresList.add(score4);
        stats.qvalue(scoresList, 0.2, false);

    }
}
