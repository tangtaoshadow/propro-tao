package com.westlake.air.pecs.domain.db;

import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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

    Boolean isDecoy = false;

    List<FeatureScores> featureScoresList;

    Date createDate;

    Date lastModifiedDate;
}
