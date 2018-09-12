package com.westlake.air.pecs.domain.bean.airus;

import lombok.Data;

@Data
public class ScoreData {

    /**
     * groupId represents which peak group the row data belongs to.
     */
    String[] groupId;

    Integer[] groupNumId;

    String[] scoreColumns;

    Integer[] runId;

    /**
     * isDecoy == False , row value is from target peptide.
     * isDecoy == True, row value is from decoy peptide.
     */
    Boolean[] isDecoy;

    /**
     * scores
     */
    Double[][] scoreData ;

}
