package com.westlake.air.pecs.domain.bean.airus;

import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.domain.bean.score.SimpleFeatureScores;
import com.westlake.air.pecs.domain.db.simple.SimpleScores;
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

    public void removeWeightedTotalScore() {
        if(targets!=null){
            for (SimpleScores ss : targets) {
                for (FeatureScores sft : ss.getFeatureScoresList()) {
                    sft.getScoresMap().remove(FeatureScores.ScoreType.WeightedTotalScore.getTypeName());
                }
            }
        }

        if(decoys != null){
            for (SimpleScores ss : decoys) {
                for (FeatureScores sft : ss.getFeatureScoresList()) {
                    sft.getScoresMap().remove(FeatureScores.ScoreType.WeightedTotalScore.getTypeName());
                }
            }
        }

    }
}
