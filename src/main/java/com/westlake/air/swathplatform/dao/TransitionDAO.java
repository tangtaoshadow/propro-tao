package com.westlake.air.swathplatform.dao;

import com.mongodb.BasicDBObject;
import com.westlake.air.swathplatform.domain.db.TransitionDO;
import com.westlake.air.swathplatform.domain.query.TransitionQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-07 20:50
 */
@Service
public class TransitionDAO {

    public static String CollectionName = "transition";

    @Autowired
    MongoTemplate mongoTemplate;

    public List<TransitionDO> getAllByLibraryId(String libraryId){
        Query query = new Query(where("libraryId").is(libraryId));
        return mongoTemplate.find(query, TransitionDO.class, CollectionName);
    }

    public List<TransitionDO> getAllByLibraryIdAndIsDecoy(String libraryId, boolean isDecoy){
        Query query = new Query(where("libraryId").is(libraryId));
        query.addCriteria(where("isDecoy").is(isDecoy));
        return mongoTemplate.find(query, TransitionDO.class, CollectionName);
    }

    public List<TransitionDO> getList(TransitionQuery query) {
        return mongoTemplate.find(buildQuery(query), TransitionDO.class, CollectionName);
    }

    public long count(TransitionQuery query){
        return mongoTemplate.count(buildQueryWithoutPage(query), TransitionDO.class, CollectionName);
    }

    public TransitionDO getById(String id) {
        return mongoTemplate.findById(id, TransitionDO.class, CollectionName);
    }

    public TransitionDO insert(TransitionDO transitionDO) {
        mongoTemplate.insert(transitionDO, CollectionName);
        return transitionDO;
    }

    public List<TransitionDO> insert(List<TransitionDO> transitions) {
        mongoTemplate.insert(transitions, CollectionName);
        return transitions;
    }

    public TransitionDO update(TransitionDO transitionDO) {
        mongoTemplate.save(transitionDO, CollectionName);
        return transitionDO;
    }

    public void delete(String id) {
        mongoTemplate.remove(id, CollectionName);
    }

    public void deleteAllByLibraryId(String libraryId) {
        Query query = new Query(where("libraryId").is(libraryId));
        mongoTemplate.remove(query, TransitionDO.class, CollectionName);
    }

    public Integer countByProteinName(String libraryId) {
        AggregationResults<BasicDBObject> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        TransitionDO.class,
                        Aggregation.match(where("libraryId").is(libraryId)),
                        Aggregation.group("proteinName").count().as("count1")), CollectionName,
                BasicDBObject.class);
        return a.getMappedResults().size();
    }

    public Integer countByPeptideSequence(String libraryId) {
        AggregationResults<BasicDBObject> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        TransitionDO.class,
                        Aggregation.match(where("libraryId").is(libraryId)),
                        Aggregation.group("peptideSequence").count().as("count")), CollectionName,
                BasicDBObject.class);
        return a.getMappedResults().size();
    }

    public Integer countByTransitionName(String libraryId) {
        AggregationResults<BasicDBObject> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        TransitionDO.class,
                        Aggregation.match(where("libraryId").is(libraryId)),
                        Aggregation.group("transitionName").count().as("count")), CollectionName,
                BasicDBObject.class);
        return a.getMappedResults().size();
    }

    private Query buildQuery(TransitionQuery transitionQuery) {
        Query query = buildQueryWithoutPage(transitionQuery);

        query.skip((transitionQuery.getPageNo() - 1) * transitionQuery.getPageSize());
        query.limit(transitionQuery.getPageSize());
        query.with(new Sort(transitionQuery.getOrderBy(), transitionQuery.getSortColumn()));
        return query;
    }

    private Query buildQueryWithoutPage(TransitionQuery transitionQuery) {
        Query query = new Query();
        if (transitionQuery.getId() != null) {
            query.addCriteria(where("id").is(transitionQuery.getId()));
        }
        if (transitionQuery.getIsDecoy() != null) {
            query.addCriteria(where("isDecoy").is(transitionQuery.getIsDecoy()));
        }
        if (transitionQuery.getFullUniModPeptideName() != null) {
            query.addCriteria(where("fullUniModPeptideName").regex(transitionQuery.getFullUniModPeptideName(), "i"));
        }
        if (transitionQuery.getLibraryId() != null) {
            query.addCriteria(where("libraryId").is(transitionQuery.getLibraryId()));
        }
        if (transitionQuery.getPeptideSequence() != null) {
            query.addCriteria(where("peptideSequence").regex(transitionQuery.getPeptideSequence(), "i"));
        }
        if (transitionQuery.getProteinName() != null) {
            query.addCriteria(where("proteinName").is(transitionQuery.getProteinName()));
        }
        if (transitionQuery.getTransitionName() != null) {
            query.addCriteria(where("transitionName").regex(transitionQuery.getTransitionName(),"i"));
        }

        return query;
    }

}
