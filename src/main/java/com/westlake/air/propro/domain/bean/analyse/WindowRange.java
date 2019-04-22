package com.westlake.air.propro.domain.bean.analyse;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-26 23:32
 */
@Data
public class WindowRange {

    //precursor mz start
    Float start;
    //precursor mz end
    Float end;
    //precursor mz interval
    Float interval;

    Float mz;

    public WindowRange() {}

    public WindowRange(Float start, Float end){
        this.start = start;
        this.end = end;
    }

    public WindowRange(Float start, Float end, Float interval){
        this.start = start;
        this.end = end;
        this.interval = interval;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null){
            return false;
        }

        if(obj instanceof WindowRange){
            WindowRange windowRange = (WindowRange) obj;
            if(start == null || end == null || windowRange.getStart() == null || windowRange.getEnd()==null){
                return false;
            }

            return (this.start.equals(windowRange.getStart()) && this.end.equals(windowRange.getEnd()));
        }else{
            return false;
        }

    }
}
