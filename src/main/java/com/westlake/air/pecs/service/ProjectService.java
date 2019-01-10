package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.ProjectDO;
import com.westlake.air.pecs.domain.query.ProjectQuery;

import java.util.List;

public interface ProjectService {

    ResultDO<List<ProjectDO>> getList(ProjectQuery query);

    List<ProjectDO> getAll();

    ResultDO insert(ProjectDO project);

    ResultDO update(ProjectDO projectDO);

    ResultDO delete(String id);

    ResultDO<ProjectDO> getById(String id);

    ResultDO<ProjectDO> getByName(String name);
}
