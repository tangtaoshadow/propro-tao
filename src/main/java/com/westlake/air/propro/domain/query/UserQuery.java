package com.westlake.air.propro.domain.query;

import lombok.Data;

@Data
public class UserQuery extends PageQuery {

    String id;

    String username;
}
