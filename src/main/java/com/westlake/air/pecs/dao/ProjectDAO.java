package com.westlake.air.pecs.dao;

import com.westlake.air.pecs.domain.db.ProjectDO;
import com.westlake.air.pecs.domain.query.ProjectQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Created by James Lu MiaoShan
 * Time: 2019-01-10 13:16
 */
@Service
public class ProjectDAO {

    public static String CollectionName = "project";

    @Autowired
    MongoTemplate mongoTemplate;

    public long count(ProjectQuery query) {
        return mongoTemplate.count(buildQueryWithoutPage(query), ProjectDO.class);
    }

    public List<ProjectDO> getList(ProjectQuery query) {
        return mongoTemplate.find(buildQuery(query), ProjectDO.class, CollectionName);
    }

    public List<ProjectDO> getAll() {
        return mongoTemplate.findAll(ProjectDO.class, CollectionName);
    }

    public ProjectDO getById(String id) {
        return mongoTemplate.findById(id, ProjectDO.class, CollectionName);
    }

    public ProjectDO getByName(String name) {
        ProjectQuery query = new ProjectQuery();
        query.setName(name);
        return mongoTemplate.findOne(buildQuery(query), ProjectDO.class, CollectionName);
    }

    public ProjectDO insert(ProjectDO project) {
        mongoTemplate.insert(project, CollectionName);
        return project;
    }

    public ProjectDO update(ProjectDO project) {
        mongoTemplate.save(project, CollectionName);
        return project;
    }

    public List<ProjectDO> insertAll(List<ProjectDO> projectList) {
        mongoTemplate.insert(projectList, CollectionName);
        return projectList;
    }

    public void delete(String id) {
        Query query = new Query(where("id").is(id));
        mongoTemplate.remove(query, ProjectDO.class, CollectionName);
    }

    private Query buildQuery(ProjectQuery projectQuery) {
        Query query = buildQueryWithoutPage(projectQuery);
        query.skip((projectQuery.getPageNo() - 1) * projectQuery.getPageSize());
        query.limit(projectQuery.getPageSize());
        if(projectQuery.getSortColumn() != null){
            query.with(new Sort(projectQuery.getOrderBy(), projectQuery.getSortColumn()));
        }
        return query;
    }

    private Query buildQueryWithoutPage(ProjectQuery projectQuery) {
        Query query = new Query();
        if (projectQuery.getId() != null) {
            query.addCriteria(where("id").is(projectQuery.getId()));
        }
        if (projectQuery.getName() != null) {
            query.addCriteria(where("name").is(projectQuery.getName()));
        }
        if (projectQuery.getOwnerId() != null) {
            query.addCriteria(where("ownerId").is(projectQuery.getOwnerId()));
        }
        if (projectQuery.getOwnerName() != null) {
            query.addCriteria(where("ownerName").is(projectQuery.getOwnerName()));
        }
        return query;
    }
}
