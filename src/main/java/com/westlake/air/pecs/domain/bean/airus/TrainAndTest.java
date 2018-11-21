package com.westlake.air.pecs.domain.bean.airus;

import com.westlake.air.pecs.domain.db.ScoresDO;
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

    List<ScoresDO> trains;
    List<ScoresDO> tests;

    public TrainAndTest(){}

    public TrainAndTest(List<ScoresDO> trains, List<ScoresDO> tests){
        this.trains = trains;
        this.tests = tests;
    }
}
