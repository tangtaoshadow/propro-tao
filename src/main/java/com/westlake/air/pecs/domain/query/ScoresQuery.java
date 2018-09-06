package com.westlake.air.pecs.domain.query;

import lombok.Data;

@Data
public class ScoresQuery extends PageQuery{

    String id;

    String overviewId;

    String peptideRef;
}
