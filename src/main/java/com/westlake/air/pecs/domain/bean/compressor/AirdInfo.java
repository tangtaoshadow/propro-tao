package com.westlake.air.pecs.domain.bean.compressor;

import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class AirdInfo {

    String compressionType = "zlib";
    String byteOrder = "network";
    String precision = "32";

    //转换压缩后的aird的文件路径
    String airdPath;
    //实验的描述
    String description;
    //实验的创建者
    String creator = "Admin";
    //实验的创建日期
    Date createDate;
    //Swaht的各个窗口间的重叠部分
    Float overlap;

    /**
     * Store the window rangs which have been adjusted with experiment overlap
     */
    List<WindowRang> windowRangs = new ArrayList<>();

    /**
     * the whole new scan index for new format file
     */
    List<ScanIndexDO> scanIndex = new ArrayList<>();

    /**
     * the swath window location(start and and) for new format file
     */
    List<ScanIndexDO> swathIndexes = new ArrayList<>();
}
