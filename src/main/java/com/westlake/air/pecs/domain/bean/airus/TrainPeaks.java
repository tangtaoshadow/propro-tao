package com.westlake.air.pecs.domain.bean.airus;

import com.westlake.air.pecs.domain.bean.score.SimpleFeatureScores;
import lombok.Data;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-13 16:00
 */
@Data
public class TrainPeaks {

    Double[][] topDecoyPeaks;

    Double[][] bestTargetPeaks;

    List<SimpleFeatureScores> bestTargets;

    List<SimpleFeatureScores> topDecoys;
}
