package com.westlake.air.propro.service;

import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.params.LumsParams;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.domain.query.ExperimentQuery;

import java.io.File;
import java.util.HashMap;
import java.util.List;


/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 09:36
 */
public interface ExperimentService {

    ResultDO<List<ExperimentDO>> getList(ExperimentQuery query);

    long count(ExperimentQuery query);

    List<ExperimentDO> getAll(ExperimentQuery query);

    List<ExperimentDO> getAllByProjectId(String projectId);

    List<ExperimentDO> getAllByProjectName(String projectName);

    ResultDO insert(ExperimentDO experimentDO);

    ResultDO update(ExperimentDO experimentDO);

    ResultDO delete(String id);

    ResultDO<ExperimentDO> getById(String id);

    ResultDO<ExperimentDO> getByName(String name);

    HashMap<Float, Float[]> getPrmRtWindowMap(String expId);

    public HashMap<Float, Float[]> getPrmRtWindowMap(List<SwathIndexDO> ms2SwathIndexes);

    void uploadAirdFile(ExperimentDO experimentDO, String airdFilePath, TaskDO taskDO);

}
