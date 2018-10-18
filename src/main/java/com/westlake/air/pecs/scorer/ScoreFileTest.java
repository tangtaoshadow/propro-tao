package com.westlake.air.pecs.scorer;

import com.westlake.air.pecs.domain.bean.airus.ScoreData;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.domain.db.ScoresDO;
import com.westlake.air.pecs.parser.ScoreTsvParser;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * Created by Nico Wang
 * Time: 2018-10-16 20:33
 */
@Component("scoreFileTest")
public class ScoreFileTest{
    @Autowired
    ScoreTsvParser scoreTsvParser;

    public void ConsistencyTest(List<ScoresDO> pecsScoreList){
        ScoreData scoreData = scoreTsvParser.getScoreData(new File("E:\\SGS\\napedro_L120224_010_Water\\test_correspondence.tsv"), ScoreTsvParser.SPLIT_TAB);
        String[] groupId = scoreData.getGroupId();
        Double[][] scoreMatrix = scoreData.getScoreData();
        String tempId;
        for(int i=0 ; i<groupId.length; i++){
            tempId = groupId[i];
            if(tempId.equals(groupId[i])){

            }
        }
        for(String id: groupId){

            //get final Id
            String finalId;
            String[] idSplit = id.split("_");
            if(idSplit[0].equals("DECOY")){
                String[] oriIdSplit = idSplit[3].split("/");
                finalId = "DECOY_" + oriIdSplit[0] + "_" + oriIdSplit[1];
            }else {
                String[] oriIdSplit = idSplit[2].split("/");
                finalId = oriIdSplit[0] + "_" + oriIdSplit[1];
            }
            for(ScoresDO scoresDO: pecsScoreList){
                if(scoresDO.getPeptideRef().equals(finalId)){
                    List<FeatureScores> featureScores = scoresDO.getFeatureScoresList();
                    for(FeatureScores scores: featureScores){

                    }
                }
            }

        }
    }
}
