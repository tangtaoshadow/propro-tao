package com.westlake.air.pecs.test.rtnormalizer;

import com.westlake.air.pecs.domain.bean.score.ScoreRtPair;
import com.westlake.air.pecs.rtnormalizer.RTNormalizerScorer;
import com.westlake.air.pecs.test.BaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @Author: An Shaowei
 * @Time: 2018/8/15 9:50
 */
public class RTNormalizerScorerTest extends BaseTest {
    @Autowired
    RTNormalizerScorer RTNormalizerScorer;

    @Test
    public void scoreTest(){
        List<ScoreRtPair> result;
        assert true;
    }

    @Test
    public void calculateChromatographicScoresTest(){
        assert true;
    }

    @Test
    public void calculateLibraryScoresTest(){
        assert true;
    }

    @Test
    public void calculateIntensityScoreTest(){
        assert true;
    }

    @Test
    public void calculateDiaMassDiffScoreTest(){
        assert true;
    }

}
