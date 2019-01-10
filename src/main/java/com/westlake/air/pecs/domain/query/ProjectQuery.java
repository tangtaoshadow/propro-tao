package com.westlake.air.pecs.domain.query;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "project")
public class ProjectQuery extends PageQuery {

    @Id
    String id;

    //项目名称
    @Indexed
    String name;

    //项目负责人ID
    String ownerId;

    //项目负责人名称
    String ownerName;

    public ProjectQuery(){}

    public ProjectQuery(int pageNo, int pageSize, Sort.Direction direction, String sortColumn){
        super(pageNo, pageSize, direction, sortColumn);
    }
}
