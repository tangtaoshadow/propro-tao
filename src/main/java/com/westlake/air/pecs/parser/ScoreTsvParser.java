package com.westlake.air.pecs.parser;


import com.westlake.air.pecs.utils.ArrayUtils;
import com.westlake.air.pecs.domain.bean.airus.ScoreData;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@Component
public class ScoreTsvParser {

    public ScoreData getScoreData(File file) {
        try {
            Integer lines = getFileLineCount(file);
            ScoreData scoreDataMap = new ScoreData();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            Double[][] scoreData = new Double[lines][17];
            String[] groupId = new String[lines];
            Integer[] runId = new Integer[lines];
            Boolean[] isDecoy = new Boolean[lines];
            String[] readLine;
            String line = reader.readLine();
            readLine = line.split("\t");
            String[] scoreColumns = new String[17];
            System.arraycopy(readLine,3,scoreColumns,0,17);
            line = reader.readLine();
            int i = 0;
//            int decoySum=0;
            while (line != null) {
//                if(readLine[2].equals("1")){
//                    decoySum++;
//                }
                readLine = line.split("\t");
                groupId[i]=readLine[0];
                runId[i] = Integer.parseInt(readLine[1]);
                isDecoy[i] = readLine[2].equals("1");
                for (int j = 0; j < 17; j++) {
                    scoreData[i][j] = Double.parseDouble(readLine[j+3]);
                }
                line = reader.readLine();
                i++;
            }
            Integer[] groupNumId = ArrayUtils.getGroupNumId(groupId).getModel();
//            int sum = 0;
//            for (boolean j : isDecoy) {
//                if(j) sum++;
//            }
            int a=groupNumId[9164];
            //System.out.println(scoreData);
            scoreDataMap.setGroupId(groupId);
            scoreDataMap.setRunId(runId);
            scoreDataMap.setIsDecoy(isDecoy);
            scoreDataMap.setScoreData(scoreData);
            scoreDataMap.setScoreColumns(scoreColumns);
            scoreDataMap.setGroupNumId(groupNumId);

            return detectNull(scoreDataMap);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    private ScoreData detectNull(ScoreData scoreDataMap){
        for(int i=0;i<scoreDataMap.getRunId().length;i++){
            if(scoreDataMap.getRunId()[i] == null){
                return null;
            }else if(scoreDataMap.getGroupId()[i] == null){
                return null;
            }else if(scoreDataMap.getIsDecoy()[i] == null){
                return null;
            }else{
                for(int k=0;k<scoreDataMap.getScoreColumns().length;k++) {
                    if (scoreDataMap.getScoreData()[i][k] == null) {
                        return null;
                    }
                }
            }
        }
        for(int k=0;k<scoreDataMap.getScoreColumns().length;k++){
            if(scoreDataMap.getScoreColumns() == null){
                return null;
            }
        }
        return scoreDataMap;

    }

    private Integer getFileLineCount(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            reader.readLine();
            Integer fileLineCount = 0;
            while (line != null) {
                fileLineCount++;
                line = reader.readLine();
            }
            return fileLineCount;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }
}

