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

    //存放aird文件的位置
    String airdFilePath;
    //存放library库文件的地方,包括TSV和TraML格式
    String libraryFilePath;
    //卷积文件导出位置
    String exportAnalyseFilePath;
    //子分数文件导出位置
    String exportScoresFilePath = "D:/";
    //压缩文件默认前缀
    String prefixForCompressorFile = "airus_";
    //存放mzxml文件的位置
    String mzxmlFilePath;

    Date createDate;

    Date lastModifiedDate;
}
