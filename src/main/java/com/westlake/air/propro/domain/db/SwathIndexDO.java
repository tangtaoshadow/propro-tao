package com.westlake.air.propro.domain.db;

import com.westlake.aird.bean.SwathIndex;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document("swathIndex")
public class SwathIndexDO extends SwathIndex {

    @Id
    String id;

    @Indexed
    String expId;

    public SwathIndexDO(){}

    public SwathIndexDO(SwathIndex swathIndex){

        this.setLevel(swathIndex.getLevel());          //1: ms1 swath block, 2: ms2 swath block
        this.setStartPtr(swathIndex.getStartPtr());    //在文件中的开始位置
        this.setEndPtr(swathIndex.getEndPtr());        //在文件中的结束位置
        this.setRange(swathIndex.getRange());//SWATH块对应的WindowRange
        this.setNums(swathIndex.getNums());//当msLevel=1时,本字段为Swath Block中每一个MS1谱图的序号,,当msLevel=2时本字段为Swath Block中每一个MS2谱图对应的MS1谱图的序列号,MS2谱图本身不需要记录序列号
        this.setRts(swathIndex.getRts());//一个Swath块中所有子谱图的rt时间列表
        this.setMzs(swathIndex.getMzs());//一个Swath块中所有子谱图的mz的压缩后的大小列表
        this.setInts(swathIndex.getInts());//一个Swath块中所有子谱图的intenisty的压缩后的大小列表
        this.setFeatures(swathIndex.getFeatures());//用于存储KV键值对
    }
}
