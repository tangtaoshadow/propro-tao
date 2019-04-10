package com.westlake.air.propro.domain.db;

import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.domain.BaseDO;
import com.westlake.air.propro.domain.bean.analyse.WindowRange;
import com.westlake.air.propro.domain.bean.compressor.Strategy;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.nio.ByteOrder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.westlake.air.propro.constants.Constants.EXP_TYPE_DIA_SWATH;
import static com.westlake.air.propro.constants.Constants.EXP_TYPE_PRM;

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

    //所属的公开组织名称
    @Indexed
    String publicName;

    //实验项目名称
    @Indexed
    String projectName;

    //批次名
    @Indexed
    String batchName;

    //必填,实验名称
    String name;

    //DIA-Swath, PRM, @see Constants
    String type;

    //mzxml的文件路径
    String filePath;

    //原始文件是否已经被压缩为aird文件了
//    Boolean hasAirusFile;
    //转换压缩后的aird的文件名称
    String airdPath;
    //转换压缩后aird的文件路径
    String airdIndexPath;

    //实验的描述
    String description;

    //实验的创建者
    String creator = "Admin";

    //实验的创建日期
    Date createDate;

    //最后修改日期
    Date lastModifiedDate;

    //Swaht的各个窗口间的重叠部分
    Float overlap;

    //对应的irt校准库的id
    String iRtLibraryId;

    //计算irt后得到的斜率
    Double slope;

    //计算irt后得到的截距
    Double intercept;

    //转byte时的编码顺序,由于C#默认采用LITTLE_ENDIAN,Aird文件由Propro-Client(C#端)转换而来,因此也采用LITTLE_ENDIAN的编码
    String byteOrder;

    //MZ数组和Intensity数组分别采用的压缩策略,Propro1.0采用的是mz:pfor,zlib:1000;intensity:zlib:1000
    HashMap<String, Strategy> strategies = new HashMap<>();

    //新增的三个字段,用以支持最新的数据格式,仅支持MzXML格式的文件
    String compressionType;
    //压缩的数值精度,一般为32或者64,代表Float类型和Double类型
    String precision;

    //Swath窗口列表
    List<WindowRange> windowRanges;

    public ByteOrder getByteOrderClass(){
        if("LITTLE_ENDIAN".equals(byteOrder)){
            return ByteOrder.LITTLE_ENDIAN;
        }else{
            return ByteOrder.BIG_ENDIAN;
        }
    }

}
