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

    Boolean isDecoy = false;

    List<FeatureScores> featureScoresList;

    Date createDate;

    Date lastModifiedDate;

    //本字段仅在排序的时候使用,不保存在数据库中
    String groupId;
}
