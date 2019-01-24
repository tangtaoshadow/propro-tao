package com.westlake.air.propro.service;

import com.westlake.air.propro.domain.bean.airus.AirusParams;
import com.westlake.air.propro.domain.bean.airus.FinalResult;

public interface ApiService {

    //iRT

    //合并打分
    FinalResult doAirus(String overviewId);
    FinalResult doAirus(String overviewId, AirusParams airusParams);

    //卷积

    //Aird压缩


}
