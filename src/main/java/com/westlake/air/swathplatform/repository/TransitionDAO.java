package com.westlake.air.swathplatform.repository;

import com.mongodb.BasicDBObject;
import com.westlake.air.swathplatform.domain.db.TransitionDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-07 20:50
 */
@Service
public class TransitionDAO {

    @Autowired
    MongoTemplate mongoTemplate;

    public TransitionDO getById(String id) {
        return mongoTemplate.findById(id, TransitionDO.class);
    }

    public TransitionDO insert(TransitionDO transitionDO) {
        mongoTemplate.insert(transitionDO);
        return transitionDO;
    }

    public TransitionDO update(TransitionDO transitionDO) {
        mongoTemplate.save(transitionDO);
        return transitionDO;
    }

    public List<TransitionDO> insertAll(List<TransitionDO> transitions) {
        mongoTemplate.insertAll(transitions);
        return transitions;
    }

    public void delete(String id) {
        mongoTemplate.remove(id);
    }

    public void deleteAllByLibraryId(String libraryId) {
        Query query = new Query(where("libraryId").is(libraryId));
        mongoTemplate.remove(query, TransitionDO.class);
    }

    public Integer countByProteinName(String libraryId){
        AggregationResults<BasicDBObject> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        TransitionDO.class,
                        Aggregation.match(where("libraryId").is(libraryId)),
                        Aggregation.group("proteinName").count().as("count1")),
                        BasicDBObject.class);
        return a.getMappedResults().size();
    }

    public Integer countByPeptideSequence(String libraryId){
        AggregationResults<BasicDBObject> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        TransitionDO.class,
                        Aggregation.match(where("libraryId").is(libraryId)),
                        Aggregation.group("peptideSequence").count().as("count")),
                BasicDBObject.class);
        return a.getMappedResults().size();
    }

    public Integer countByTransitionName(String libraryId){
        AggregationResults<BasicDBObject> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        TransitionDO.class,
                        Aggregation.match(where("libraryId").is(libraryId)),
                        Aggregation.group("transitionName").count().as("count")),
                BasicDBObject.class);
        return a.getMappedResults().size();
    }
}
