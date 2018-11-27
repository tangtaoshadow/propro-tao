package com.westlake.air.pecs.test.algorithm;

import com.alibaba.fastjson.JSON;
import com.westlake.air.pecs.algorithm.Airus;
import com.westlake.air.pecs.domain.bean.airus.FinalResult;
import com.westlake.air.pecs.domain.bean.airus.Params;
import com.westlake.air.pecs.domain.bean.airus.ScoreData;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.db.ScoresDO;
import com.westlake.air.pecs.service.ScoresService;
import com.westlake.air.pecs.test.BaseTest;
import com.westlake.air.pecs.utils.AirusUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-12 21:18
 */
public class AirusTest extends BaseTest {


    @Autowired
    Airus airus;
    @Autowired
    ScoresService scoresService;

    @Test
    public void scoreFromDBWork() {
        HashMap<String, ScoresDO> scoreMap = scoresService.getAllMapByOverviewId("5bbdaaf1fc6f9e1f2872d5ce");
        ScoreData scoreData = airus.trans(new ArrayList(scoreMap.values()));
        FinalResult finalResult = airus.doAirus(scoreData, new Params());

        int count = AirusUtil.checkFdr(finalResult);
        System.out.println(count);
        System.out.println(JSON.toJSONString(finalResult.getWeightsMap()));

        assert count >= 322;
    }

    private boolean isSimilar(Double[] array1, Double[] array2, Double tolerance) {
        if (array1.length != array2.length) return false;
        boolean result = true;
        for (int i = 0; i < array1.length; i++) {
            if (Math.abs(array1[i] - array2[i]) > tolerance) {
                result = false;
            }
        }
        return result;
    }

    private boolean isSimilar(double[] array1, double[] array2, Double tolerance) {
        if (array1.length != array2.length) return false;
        boolean result = true;
        for (int i = 0; i < array1.length; i++) {
            if (Math.abs(array1[i] - array2[i]) > tolerance) {
                result = false;
            }
        }
        return result;
    }

    @Test
    public void test() {
        List<RtIntensityPairsDouble> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            RtIntensityPairsDouble rtIntensityPairsDouble = new RtIntensityPairsDouble();
            Double[] rt = {(double) i};
            rtIntensityPairsDouble.setRtArray(rt);
            rtIntensityPairsDouble.setIntensityArray(rt);
            list.add(rtIntensityPairsDouble);
        }
        System.out.println("list ready");
        RtIntensityPairsDouble rtInt = list.get(1);
        Double[] intensity = {0d};
        rtInt.setIntensityArray(intensity);
        System.out.println("what now");
    }
}
