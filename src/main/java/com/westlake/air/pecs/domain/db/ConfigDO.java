package com.westlake.air.pecs.domain.db;

import com.westlake.air.pecs.domain.BaseDO;
import lombok.Data;

import java.util.Date;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-14 10:04
 */
@Data
public class ConfigDO extends BaseDO {

    String id;

    //文件的仓库地址
    String repository;

    Date createDate;

    Date lastModifiedDate;

    //默认的airc文件会放在airc文件夹下
    public String getAircFilePath(){
        return repository+"/airc";
    }

    //默认的aird文件会放在aird文件夹下
    public String getAirdFilePath(){
        return repository+"/aird";
    }
}
