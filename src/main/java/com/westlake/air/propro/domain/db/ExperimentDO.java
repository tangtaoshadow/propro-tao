package com.westlake.air.propro.domain.db;

import com.westlake.air.propro.constants.SuffixConst;
import com.westlake.air.propro.domain.BaseDO;
import com.westlake.air.propro.domain.bean.aird.*;
import com.westlake.air.propro.domain.bean.irt.IrtResult;
import com.westlake.air.propro.utils.RepositoryUtil;
import lombok.Data;
import org.apache.commons.io.FilenameUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-04 13:46
 */
@Data
@Document(collection = "experiment")
public class ExperimentDO extends BaseDO {

    private static final long serialVersionUID = -3258829839160856625L;

    @Id
    String id;

    @Indexed
    String projectId;

    @Indexed
    String ownerName;

    //仪器设备信息
    Instrument instrument;

    //处理的软件信息
    List<Software> softwares;

    //处理前的文件信息
    List<ParentFile> parentFiles;

    //[核心字段]数组压缩策略
    List<Compressor> compressors;

    //核心字段, Swath窗口列表
    List<WindowRange> windowRanges;

    //关联的项目名称
    String projectName;

    //必填,实验名称,与Aird文件同名
    String name;

    //别名,默认为空
    String alias;

    //DIA_SWATH, PRM, @see Constants
    String type;

    //Aird文件大小,单位byte
    Long airdSize;

    //Aird索引文件的大小,单位byte
    Long airdIndexSize;

    //原始文件的大小,单位byte
    Long vendorFileSize;

    //实验的描述
    String description;

    //实验的创建日期
    Date createDate;

    //最后修改日期
    Date lastModifiedDate;

    //对应的irt校准库的id
    String iRtLibraryId;

    IrtResult irtResult;

    //转byte时的编码顺序,由于C#默认采用LITTLE_ENDIAN,Aird文件由Propro-Client(C#端)转换而来,因此也采用LITTLE_ENDIAN的编码
    String features;

    public Compressor fetchCompressor(String target) {
        for (Compressor c : compressors) {
            if (c.getTarget().equals(target)) {
                return c;
            }
        }
        return null;
    }

    public String getAirdPath() {
        return FilenameUtils.concat(FilenameUtils.concat(RepositoryUtil.getRepo(), projectName), name) + SuffixConst.AIRD;
    }

    public String getAirdIndexPath() {
        return FilenameUtils.concat(FilenameUtils.concat(RepositoryUtil.getRepo(), projectName), name) + SuffixConst.JSON;
    }
}
