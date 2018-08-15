package com.westlake.air.pecs.domain.query;

import lombok.Data;

import java.util.Date;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-04 21:16
 */
@Data
public class LibraryQuery extends PageQuery {

    private static final long serialVersionUID = -3258829839160856625L;

    String id;

    String name;

    Integer type;

    Date createDate;

    Date lastModifiedDate;

    public LibraryQuery(){}

    public LibraryQuery(int pageNo,int pageSize){
        super(pageNo, pageSize);
    }
}
