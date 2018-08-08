package com.westlake.air.pecs.dao;

import com.mongodb.BasicDBObject;
import com.westlake.air.pecs.domain.db.simple.*;
import com.westlake.air.pecs.domain.db.TransitionDO;
import com.westlake.air.pecs.domain.query.AnalyseDataQuery;
import com.westlake.air.pecs.domain.query.TransitionQuery;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

    public List<TransitionDO> getAllByLibraryId(String libraryId) {
        Query query = new Query(where("libraryId").is(libraryId));
        return mongoTemplate.find(query, TransitionDO.class, CollectionName);
    }

    public List<TransitionDO> getAllByLibraryIdAndIsDecoy(String libraryId, boolean isDecoy) {
        Query query = new Query(where("libraryId").is(libraryId));
        query.addCriteria(where("isDecoy").is(isDecoy));
        return mongoTemplate.find(query, TransitionDO.class, CollectionName);
    }

    public List<TransitionDO> getList(TransitionQuery query) {
        return mongoTemplate.find(buildQuery(query), TransitionDO.class, CollectionName);
    }

    public List<TargetTransition> getTTAll(TransitionQuery query) {
        return mongoTemplate.find(buildQueryWithoutPage(query), TargetTransition.class, CollectionName);
    }

    public long count(TransitionQuery query) {
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
        Query query = new Query(where("id").is(id));
        mongoTemplate.remove(query, TransitionDO.class, CollectionName);
    }

    public void deleteAllByLibraryId(String libraryId) {
        Query query = new Query(where("libraryId").is(libraryId));
        mongoTemplate.remove(query, TransitionDO.class, CollectionName);
    }

    public void deleteAllDecoyByLibraryId(String libraryId) {
        Query query = new Query(where("libraryId").is(libraryId));
        query.addCriteria(where("isDecoy").is(true));
        mongoTemplate.remove(query, TransitionDO.class, CollectionName);
    }

    public List<Protein> getProteinList(TransitionQuery query) {
        AggregationResults<Protein> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        Protein.class,
                        Aggregation.match(where("libraryId").is(query.getLibraryId())),
                        Aggregation.group("proteinName").
                                first("proteinName").as("proteinName").
                                first("id").as("transitionId").
                                first("libraryId").as("libraryId").
                                first("libraryName").as("libraryName"),
                        Aggregation.skip((query.getPageNo() - 1) * query.getPageSize()),
                        Aggregation.limit(query.getPageSize())).withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build()), CollectionName,
                Protein.class);
        return a.getMappedResults();
    }

    public List<Peptide> getPeptideList(TransitionQuery query) {
        AggregationResults<Peptide> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        TransitionDO.class,
                        Aggregation.match(where("libraryId").is(query.getLibraryId())),
                        Aggregation.group("peptideRef").
                                first("proteinName").as("proteinName").
                                first("peptideRef").as("peptideRef").
                                first("libraryId").as("libraryId").
                                first("libraryName").as("libraryName").
                                first("id").as("transitionId").
                                first("rt").as("rt").
                                first("intensity").as("intensity").
                                first("isDecoy").as("isDecoy"),
                        Aggregation.skip((query.getPageNo() - 1) * query.getPageSize()),
                        Aggregation.limit(query.getPageSize())
                ).withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build()), CollectionName,
                Peptide.class);

        return a.getMappedResults();
    }

    public List<IntensityGroup> getIntensityGroup(String libraryId) {
        Document queryDoc = new Document();
        if (libraryId != null) {
            queryDoc.put("libraryId", libraryId);
        }

        Document fieldsDoc = new Document();
        fieldsDoc.put("peptideRef", true);
        fieldsDoc.put("proteinName", true);
        fieldsDoc.put("intensity", true);

        Query query = new BasicQuery(queryDoc, fieldsDoc);

        List<TransitionDO> list = mongoTemplate.find(query, TransitionDO.class, CollectionName);
        HashMap<String, IntensityGroup> hashMap = new HashMap<>();
        List<IntensityGroup> intensityGroups = new ArrayList<>();
        for (TransitionDO transition : list) {
            IntensityGroup group = hashMap.get(transition.getPeptideRef());
            if (group == null) {
                group = new IntensityGroup();
                group.setPeptideRef(transition.getPeptideRef());
                group.setProteinName(transition.getProteinName());
                intensityGroups.add(group);
                hashMap.put(transition.getPeptideRef(), group);
            }

            group.getIntensityList().add(Float.parseFloat(transition.getIntensity().toString()));
        }

        return intensityGroups;
    }

    public long countByProteinName(String libraryId) {
        AggregationResults<BasicDBObject> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        TransitionDO.class,
                        Aggregation.match(where("libraryId").is(libraryId)),
                        Aggregation.group("proteinName").count().as("count")).withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build()), CollectionName,
                BasicDBObject.class);
        return (long) a.getMappedResults().size();
    }

    public long countByPeptideRef(String libraryId) {
        AggregationResults<BasicDBObject> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        TransitionDO.class,
                        Aggregation.match(where("libraryId").is(libraryId)),
                        Aggregation.group("peptideRef").count().as("count")).withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build()), CollectionName,
                BasicDBObject.class);

        return (long) a.getMappedResults().size();
    }

    public long countByName(String libraryId) {
        AggregationResults<BasicDBObject> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        TransitionDO.class,
                        Aggregation.match(where("libraryId").is(libraryId)),
                        Aggregation.group("name").count().as("count")).withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build()), CollectionName,
                BasicDBObject.class);
        return (long) a.getMappedResults().size();
    }

    private Query buildQuery(TransitionQuery transitionQuery) {
        Query query = buildQueryWithoutPage(transitionQuery);

        query.skip((transitionQuery.getPageNo() - 1) * transitionQuery.getPageSize());
        query.limit(transitionQuery.getPageSize());
        //默认没有排序功能(排序会带来极大的性能开销)
        if (transitionQuery.getOrderBy() != null) {
            query.with(new Sort(transitionQuery.getOrderBy(), transitionQuery.getSortColumn()));
        }
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
        if (transitionQuery.getFullName() != null) {
            query.addCriteria(where("fullName").regex(transitionQuery.getFullName(), "i"));
        }
        if (transitionQuery.getLibraryId() != null) {
            query.addCriteria(where("libraryId").is(transitionQuery.getLibraryId()));
        }
        if (transitionQuery.getSequence() != null) {
            query.addCriteria(where("sequence").regex(transitionQuery.getSequence(), "i"));
        }
        if (transitionQuery.getProteinName() != null) {
            query.addCriteria(where("proteinName").is(transitionQuery.getProteinName()));
        }
        if (transitionQuery.getName() != null) {
            query.addCriteria(where("name").regex(transitionQuery.getName(), "i"));
        }
        if (transitionQuery.getPrecursorMzStart() != null) {
            query.addCriteria(where("precursorMz").gte(transitionQuery.getPrecursorMzStart()).lte(transitionQuery.getPrecursorMzEnd()));
        }
        return query;
    }

}
