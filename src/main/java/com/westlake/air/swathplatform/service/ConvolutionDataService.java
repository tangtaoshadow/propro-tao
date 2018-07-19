package com.westlake.air.swathplatform.service;

import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.db.ConvolutionDataDO;
import com.westlake.air.swathplatform.domain.query.ConvolutionDataQuery;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:02
 */
public interface ConvolutionDataService {

    List<ConvolutionDataDO> getAllByExpId(String expId);

    Long count(ConvolutionDataQuery query);

    ResultDO<List<ConvolutionDataDO>> getList(ConvolutionDataQuery convQuery);

    ResultDO insert(ConvolutionDataDO convData);

    ResultDO insertAll(List<ConvolutionDataDO> convList, boolean isDeleteOld);

    ResultDO delete(String id);

    ResultDO deleteAllByExpId(String expId);

    ResultDO<ConvolutionDataDO> getById(String id);
}
