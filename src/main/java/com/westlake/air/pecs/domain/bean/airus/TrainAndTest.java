package com.westlake.air.pecs.domain.bean.airus;

import lombok.Data;

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

}
