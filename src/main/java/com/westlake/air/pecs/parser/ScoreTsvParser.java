package com.westlake.air.pecs.parser;


import com.westlake.air.pecs.domain.bean.airus.ScoreData;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.domain.db.ScoresDO;
import com.westlake.air.pecs.utils.AirusUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@Component
public class ScoreTsvParser {

    public final Logger logger = LoggerFactory.getLogger(ScoreTsvParser.class);

    public static String SPLIT_COMMA = ",";
    public static String SPLIT_CHANGE_LINE = "\t";
    /**
     * Read Scores data from tsv file
     * @param file
     * @return
     */
    public ScoreData getScoreData(File file, String split) {
        try {
            Integer lines = getFileLineCount(file);
            ScoreData scoreDataMap = new ScoreData();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            Double[][] scoreData = new Double[lines][FeatureScores.SCORES_COUNT];
            String[] groupId = new String[lines];
            Integer[] runId = new Integer[lines];
            Boolean[] isDecoy = new Boolean[lines];
            String[] readLine;
            String line = reader.readLine();
            readLine = line.split(split);
            String[] scoreColumns = new String[FeatureScores.SCORES_COUNT];
            System.arraycopy(readLine, 3, scoreColumns, 0, FeatureScores.SCORES_COUNT);
            line = reader.readLine();
            int i = 0;
            while (line != null) {
                readLine = line.split(split);
                groupId[i] = readLine[0];
                runId[i] = Integer.parseInt(readLine[1]);
                isDecoy[i] = readLine[2].equals("1");
                for (int j = 0; j < FeatureScores.SCORES_COUNT; j++) {
                    scoreData[i][j] = Double.parseDouble(readLine[j + 3]);
                }
                line = reader.readLine();
                i++;
            }
            Integer[] groupNumId = AirusUtils.getGroupNumId(groupId);

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

    /**
     * Read Scores data from tsv file
     * key为 isDecoy_peptideRef
     * @param file
     * @return
     */
    public HashMap<String, ScoresDO> getScoreMap(File file, String split) {
        try {
            HashMap<String, ScoresDO> scoreDataMap = new HashMap<>();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String[] lineArray;
            //读取第一行label
            String line = reader.readLine();
            //读取第二行内容
            line = reader.readLine();

            while (line != null) {
                lineArray = line.split(split);
                String key = lineArray[2].equals("1") + "_" + lineArray[20]+"_"+lineArray[21];
                ScoresDO scoresDO = scoreDataMap.get(key);
                if(scoresDO == null){
                    scoresDO = new ScoresDO();
                    scoresDO.setIsDecoy(lineArray[2].equals("1"));
                    scoresDO.setPeptideRef(lineArray[20]+"_"+lineArray[21]);
                    scoreDataMap.put(key, scoresDO);
                }
                Double[] scores = new Double[FeatureScores.SCORES_COUNT];
                for (int j = 0; j < FeatureScores.SCORES_COUNT; j++) {
                    scores[j] = Double.parseDouble(lineArray[j + 3]);
                }
                FeatureScores featureScores = FeatureScores.toFeaturesScores(scores);
                scoresDO.addFeatureScores(featureScores);
                line = reader.readLine();
            }

            return scoreDataMap;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    private ScoreData detectNull(ScoreData scoreDataMap) {
        for (int i = 0; i < scoreDataMap.getRunId().length; i++) {
            if (scoreDataMap.getRunId()[i] == null) {
                return null;
            } else if (scoreDataMap.getGroupId()[i] == null) {
                return null;
            } else if (scoreDataMap.getIsDecoy()[i] == null) {
                return null;
            } else {
                for (int k = 0; k < scoreDataMap.getScoreColumns().length; k++) {
                    if (scoreDataMap.getScoreData()[i][k] == null) {
                        return null;
                    }
                }
            }
        }
        for (int k = 0; k < scoreDataMap.getScoreColumns().length; k++) {
            if (scoreDataMap.getScoreColumns() == null) {
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

