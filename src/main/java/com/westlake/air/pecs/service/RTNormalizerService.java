package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;

import java.util.List;

public interface RTNormalizerService {

    ResultDO compute(String overviewId, Float sigma, Float space);

}
