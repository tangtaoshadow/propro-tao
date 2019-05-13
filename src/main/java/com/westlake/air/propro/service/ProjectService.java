package com.westlake.air.propro.service;

import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.ProjectDO;
import com.westlake.air.propro.domain.query.ProjectQuery;

import java.util.List;

public interface ProjectService {

    ResultDO<List<ProjectDO>> getList(ProjectQuery query);

    List<ProjectDO> getAll();

    ResultDO insert(ProjectDO project);

    ResultDO update(ProjectDO projectDO);

    ResultDO delete(String id);

    ResultDO<ProjectDO> getById(String id);

    ResultDO<ProjectDO> getByName(String name);

    long count(ProjectQuery query);
}
