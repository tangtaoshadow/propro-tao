package com.westlake.air.pecs.domain.db;

import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-20 21:44
 */
@Data
@Document(collection = "scores")
public class ScoresDO {

    @Id
    String id;

    @Indexed
    String overviewId;

    @Indexed
    String peptideRef;

    @Indexed
    String proteinName;

    @Indexed
    Boolean isDecoy = false;

    @Indexed
    Boolean isIdentified = false;

    String analyseDataId;

    //该肽段片段的理论rt值,从标准库中冗余所得
    Float rt;

    List<FeatureScores> featureScoresList;

    Double bestRt;

    Date createDate;

    Date lastModifiedDate;
}
