package com.westlake.air.pecs.domain.bean.airus;

import com.westlake.air.pecs.domain.db.simple.SimpleScores;
import lombok.Data;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-18 23:16
 */
@Data
public class TrainAndTest {
    Double[][] trainData;
    Integer[] trainId;
    Boolean[] trainIsDecoy;
    Double[][] testData;
    Integer[] testId;
    Boolean[] testIsDecoy;

    List<SimpleScores> trainTargets;
    List<SimpleScores> trainDecoys;
    List<SimpleScores> testTargets;
    List<SimpleScores> testDecoys;

    public TrainAndTest(){}

    public TrainAndTest(List<SimpleScores> trainTargets, List<SimpleScores> trainDecoys, List<SimpleScores> testTargets, List<SimpleScores> testDecoys){
        this.trainTargets = trainTargets;
        this.trainDecoys = trainDecoys;
        this.testTargets = testTargets;
        this.testDecoys = testDecoys;
    }
}
