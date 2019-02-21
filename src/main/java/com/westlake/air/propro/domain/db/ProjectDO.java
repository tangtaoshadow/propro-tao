package com.westlake.air.propro.domain.db;

import com.westlake.air.propro.domain.BaseDO;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "project")
public class ProjectDO extends BaseDO {

    @Id
    String id;

    //项目名称,唯一值
    @Indexed
    String name;

    //DIA_SWATH, PRM
    String type;

    //项目仓库路径(存储项目所有元数据的地方)
    String repository;

    //项目负责人ID
    String ownerId;

    //项目负责人名称
    String ownerName;

    //项目描述
    String description;

    //实验的创建日期
    Date createDate;

    //最后修改日期
    Date lastModifiedDate;

    public String getAirdPath(){
        return repository + "/aird/";
    }

    public String getAircPath(){
        return repository + "/airc/";
    }

    public String getExportPath(){
        return repository + "/export/";
    }
}
