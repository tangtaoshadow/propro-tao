package com.westlake.air.propro.domain.bean.analyse;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-26 23:32
 */
@Data
public class WindowRang {

    Float mzStart;
    Float mzEnd;
    Float ms2Interval;

    public WindowRang() {}

    public WindowRang(Float mzStart, Float mzEnd){
        this.mzStart = mzStart;
        this.mzEnd = mzEnd;
    }

    public WindowRang(Float mzStart, Float mzEnd, Float ms2Interval){
        this.mzStart = mzStart;
        this.mzEnd = mzEnd;
        this.ms2Interval = ms2Interval;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null){
            return false;
        }

        if(obj instanceof WindowRang){
            WindowRang windowRang = (WindowRang) obj;
            if(mzStart == null || mzEnd == null || windowRang.getMzStart() == null || windowRang.getMzEnd()==null){
                return false;
            }

            return (this.mzStart.equals(windowRang.getMzStart()) && this.mzEnd.equals(windowRang.getMzEnd()));
        }else{
            return false;
        }

    }
}
