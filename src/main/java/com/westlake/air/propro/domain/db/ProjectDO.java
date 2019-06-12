package com.westlake.air.propro.domain.db;

import com.westlake.air.propro.domain.BaseDO;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "project")
public class ProjectDO extends BaseDO {

    @Id
    String id;

    //项目名称,唯一值
    @Indexed
    String name;

    //是否设置为公共项目
    @Indexed
    boolean doPublic = false;

    //DIA_SWATH, PRM
    String type;

    //项目仓库路径(存储项目所有元数据的地方)
    String repository;

    //项目负责人名称
    String ownerName;

    //被授权的合作者
    List<String> collaborators;

    //项目标签
    List<String> labels = new ArrayList<>();

    //项目描述
    String description;

    //实验的创建日期
    Date createDate;

    //最后修改日期
    Date lastModifiedDate;

    public String getExportPath(){
        return repository + "/export/";
    }
}
