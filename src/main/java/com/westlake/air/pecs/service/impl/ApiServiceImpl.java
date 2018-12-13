package com.westlake.air.pecs.service.impl;

import com.westlake.air.pecs.algorithm.Airus;
import com.westlake.air.pecs.domain.bean.airus.AirusParams;
import com.westlake.air.pecs.domain.bean.airus.FinalResult;
import com.westlake.air.pecs.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("apiService")
public class ApiServiceImpl implements ApiService {

    @Autowired
    Airus airus;

    @Override
    public FinalResult doAirus(String overviewId) {
        return airus.doAirus(overviewId, new AirusParams());
    }

    @Override
    public FinalResult doAirus(String overviewId, AirusParams airusParams) {
        return airus.doAirus(overviewId, airusParams);
    }
}
