package com.westlake.air.propro.domain.bean.compressor;

import com.westlake.air.propro.domain.bean.analyse.WindowRange;
import com.westlake.air.propro.domain.db.ScanIndexDO;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class AirdInfo {

    //mz数组采用了pfor+zlib的压缩,intensity数组采用了zlib压缩
    String compressStrategy = "mz:pfor,zlib;intensity:zlib";
    //枚举值,LITTLE_ENDIAN和BIG_ENDIAN两种
    String byteOrder = "LITTLE_ENDIAN";

    //转换压缩后的aird的文件路径,默认读取的是同目录下同文件名的aird文件,如果不存在则读取本字段
    String airdPath;
    //实验的描述
    String description;
    //实验的创建者
    String creator = "Propro-Client";
    //实验的创建日期
    Date createDate;
    //Swaht的各个窗口间的重叠部分
    Float overlap;

    /**
     * Store the window rangs which have been adjusted with experiment overlap
     */
    List<WindowRange> rangeList = new ArrayList<>();

    /**
     * the whole new scan index for new format file
     */
    List<ScanIndexDO> scanIndexList = new ArrayList<>();

    /**
     * the swath window location(start and and) for new format file
     */
    List<ScanIndexDO> swathIndexList = new ArrayList<>();
}
