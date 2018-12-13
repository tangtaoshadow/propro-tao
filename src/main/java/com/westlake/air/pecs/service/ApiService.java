package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.bean.airus.AirusParams;
import com.westlake.air.pecs.domain.bean.airus.FinalResult;

public interface ApiService {

    //iRT

    //合并打分
    FinalResult doAirus(String overviewId);
    FinalResult doAirus(String overviewId, AirusParams airusParams);


}
