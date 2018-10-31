package com.westlake.air.pecs.dao;

import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.query.ExperimentQuery;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-4 13:16
 */
@Service
public class ExperimentDAO {

    public static String CollectionName = "experiment";

    @Autowired
    MongoTemplate mongoTemplate;

    public long count(ExperimentQuery query) {
        return mongoTemplate.count(buildQueryWithoutPage(query), ExperimentDO.class);
    }

    public List<ExperimentDO> getList(ExperimentQuery query) {
        return mongoTemplate.find(buildQuery(query), ExperimentDO.class, CollectionName);
    }

    public List<ExperimentDO> getAll() {
        return mongoTemplate.findAll(ExperimentDO.class, CollectionName);
    }

    public List<ExperimentDO> getSimpleAll() {
        Document queryDoc = new Document();
        Document fieldsDoc = new Document();
        fieldsDoc.put("id",true);
        fieldsDoc.put("name",true);
        fieldsDoc.put("fileLocation",true);

        Query query = new BasicQuery(queryDoc, fieldsDoc);
        return mongoTemplate.find(query, ExperimentDO.class, CollectionName);
    }

    public ExperimentDO getById(String id) {
        return mongoTemplate.findById(id, ExperimentDO.class, CollectionName);
    }

    public ExperimentDO getByName(String name) {
        ExperimentQuery query = new ExperimentQuery();
        query.setName(name);
        return mongoTemplate.findOne(buildQuery(query), ExperimentDO.class, CollectionName);
    }

    public ExperimentDO insert(ExperimentDO experimentDO) {
        mongoTemplate.insert(experimentDO, CollectionName);
        return experimentDO;
    }

    public ExperimentDO update(ExperimentDO experimentDO) {
        mongoTemplate.save(experimentDO, CollectionName);
        return experimentDO;
    }

    public List<ExperimentDO> insertAll(List<ExperimentDO> experimentList) {
        mongoTemplate.insert(experimentList, CollectionName);
        return experimentList;
    }

    public void delete(String id) {
        Query query = new Query(where("id").is(id));
        mongoTemplate.remove(query, ExperimentDO.class, CollectionName);
    }

    private Query buildQuery(ExperimentQuery experimentQuery) {
        Query query = buildQueryWithoutPage(experimentQuery);
        query.skip((experimentQuery.getPageNo() - 1) * experimentQuery.getPageSize());
        query.limit(experimentQuery.getPageSize());
        query.with(new Sort(experimentQuery.getOrderBy(), experimentQuery.getSortColumn()));
        return query;
    }

    private Query buildQueryWithoutPage(ExperimentQuery experimentQuery) {
        Query query = new Query();
        if (experimentQuery.getId() != null) {
            query.addCriteria(where("id").is(experimentQuery.getId()));
        }
        if (experimentQuery.getName() != null) {
            query.addCriteria(where("name").regex(experimentQuery.getName()));
        }
        return query;
    }
}
