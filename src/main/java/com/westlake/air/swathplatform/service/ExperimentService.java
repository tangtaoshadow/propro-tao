package com.westlake.air.swathplatform.service;

import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.bean.TargetTransition;
import com.westlake.air.swathplatform.domain.db.ExperimentDO;
import com.westlake.air.swathplatform.domain.query.ExperimentQuery;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;


/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 09:36
 */
public interface ExperimentService {

    ResultDO<List<ExperimentDO>> getList(ExperimentQuery query);

    List<ExperimentDO> getAll();

    ResultDO insert(ExperimentDO experimentDO);

    ResultDO update(ExperimentDO experimentDO);

    ResultDO delete(String id);

    ResultDO<ExperimentDO> getById(String id);

    ResultDO<ExperimentDO> getByName(String name);

    ResultDO extract(String expId, double rtExtractWindow, double mzExtractWindow, int buildType) throws IOException;

    void extractMS1(RandomAccessFile raf, String expId, String overviewId, List<TargetTransition> coordinates, double rtExtractWindow, double mzExtractWindow) throws IOException;

    void extractMS2(RandomAccessFile raf, String expId, String overviewId, List<TargetTransition> coordinates, double rtExtractWindow, double mzExtractWindow) throws IOException;

}
