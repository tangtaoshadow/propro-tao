package com.westlake.air.propro.domain.bean.airus;

import com.westlake.air.propro.constants.ScoreType;
import com.westlake.air.propro.domain.bean.score.FeatureScores;
import com.westlake.air.propro.domain.db.simple.SimpleScores;
import lombok.Data;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-18 23:16
 */
@Data
public class TrainData {

    List<SimpleScores> targets;
    List<SimpleScores> decoys;

    public TrainData() {}

    public TrainData(List<SimpleScores> targets, List<SimpleScores> decoys) {
        this.targets = targets;
        this.decoys = decoys;
    }
}
