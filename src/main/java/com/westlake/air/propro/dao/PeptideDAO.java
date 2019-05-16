package com.westlake.air.propro.dao;

import com.mongodb.BasicDBObject;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.db.simple.Protein;
import com.westlake.air.propro.domain.db.simple.TargetPeptide;
import com.westlake.air.propro.domain.query.PeptideQuery;
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
public class PeptideDAO extends BaseDAO<PeptideDO, PeptideQuery>{

    public static String CollectionName = "peptide";

    @Override
    protected String getCollectionName() {
        return CollectionName;
    }

    @Override
    protected Class getDomainClass() {
        return PeptideDO.class;
    }

    @Override
    protected boolean allowSort() {
        return true;
    }

    @Override
    protected Query buildQueryWithoutPage(PeptideQuery peptideQuery) {
        Query query = new Query();
        if (peptideQuery.getId() != null) {
            query.addCriteria(where("id").is(peptideQuery.getId()));
        }
        if (peptideQuery.getIsDecoy() != null) {
            query.addCriteria(where("isDecoy").is(peptideQuery.getIsDecoy()));
        }
        if (peptideQuery.getIsUnique() != null) {
            query.addCriteria(where("isUnique").is(peptideQuery.getIsUnique()));
        }
        if (peptideQuery.getFullName() != null) {
            query.addCriteria(where("fullName").regex(peptideQuery.getFullName(), "i"));
        }
        if (peptideQuery.getLibraryId() != null) {
            query.addCriteria(where("libraryId").is(peptideQuery.getLibraryId()));
        }
        if (peptideQuery.getSequence() != null) {
            query.addCriteria(where("sequence").regex(peptideQuery.getSequence(), "i"));
        }
        if (peptideQuery.getPeptideRef() != null) {
            query.addCriteria(where("peptideRef").is(peptideQuery.getPeptideRef()));
        }
        if (peptideQuery.getProteinName() != null) {
            query.addCriteria(where("proteinName").is(peptideQuery.getProteinName()));
        }
        if (peptideQuery.getMzStart() != null) {
            query.addCriteria(where("mz").gte(peptideQuery.getMzStart()).lt(peptideQuery.getMzEnd()));
        }
        if (peptideQuery.getLikeSequence() != null) {
            query.addCriteria(where("sequence").regex(peptideQuery.getLikeSequence(), "i"));
        }
        return query;
    }


    public List<PeptideDO> getAllByLibraryId(String libraryId) {
        Query query = new Query(where("libraryId").is(libraryId));
        return mongoTemplate.find(query, PeptideDO.class, CollectionName);
    }

    public List<PeptideDO> getAllByLibraryIdAndIsDecoy(String libraryId, boolean isDecoy) {
        Query query = new Query(where("libraryId").is(libraryId));
        query.addCriteria(where("isDecoy").is(isDecoy));
        return mongoTemplate.find(query, PeptideDO.class, CollectionName);
    }
    public List<PeptideDO> getAllByLibraryIdAndProteinNameAndIsDecoy(String libraryId, String proteinName, boolean isDecoy) {
        Query query = new Query(where("libraryId").is(libraryId));
        query.addCriteria(where("proteinName").is(proteinName));
        query.addCriteria(where("isDecoy").is(isDecoy));
        return mongoTemplate.find(query, PeptideDO.class, CollectionName);
    }

    public PeptideDO getByLibraryIdAndPeptideRefAndIsDecoy(String libraryId, String peptideRef, boolean isDecoy) {
        Query query = new Query(where("libraryId").is(libraryId));
        query.addCriteria(where("isDecoy").is(isDecoy));
        query.addCriteria(where("peptideRef").is(peptideRef));
        return mongoTemplate.findOne(query, PeptideDO.class, CollectionName);
    }

    public List<TargetPeptide> getTPAll(PeptideQuery query) {
        Query q = buildQueryWithoutPage(query);
        return mongoTemplate.find(q, TargetPeptide.class, CollectionName);
    }

    public void deleteAllByLibraryId(String libraryId) {
        Query query = new Query(where("libraryId").is(libraryId));
        mongoTemplate.remove(query, PeptideDO.class, CollectionName);
    }

    public void deleteAllDecoyByLibraryId(String libraryId) {
        Query query = new Query(where("libraryId").is(libraryId));
        query.addCriteria(where("isDecoy").is(true));
        mongoTemplate.remove(query, PeptideDO.class, CollectionName);
    }

    //TODO 本接口后续可以通过使用缓存进行优化
    public List<Protein> getProteinList(PeptideQuery query) {
        AggregationResults<Protein> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        Protein.class,
                        Aggregation.match(where("libraryId").is(query.getLibraryId())),
                        Aggregation.group("proteinName").
                                first("proteinName").as("proteinName").
                                first("id").as("peptideId").
                                first("libraryId").as("libraryId").
                                first("libraryName").as("libraryName"),
                        Aggregation.skip((query.getPageNo() - 1) * query.getPageSize()),
                        Aggregation.limit(query.getPageSize())).withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build()), CollectionName,
                Protein.class);
        return a.getMappedResults();
    }

    public long countByProteinName(String libraryId) {
        AggregationResults<BasicDBObject> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        PeptideDO.class,
                        Aggregation.match(where("libraryId").is(libraryId)),
                        Aggregation.group("proteinName").count().as("count")).withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build()), CollectionName,
                BasicDBObject.class);
        return (long) a.getMappedResults().size();
    }
    public long countByUniqueProteinName(String libraryId) {
        AggregationResults<BasicDBObject> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        PeptideDO.class,
                        Aggregation.match(where("libraryId").is(libraryId)),
                        Aggregation.match(where("isUnique").is(true)),
                        Aggregation.group("proteinName").count().as("count")).withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build()), CollectionName,
                BasicDBObject.class);
        return (long) a.getMappedResults().size();
    }

    public long countByPeptideRef(String libraryId) {
        AggregationResults<BasicDBObject> a = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        PeptideDO.class,
                        Aggregation.match(where("libraryId").is(libraryId)),
                        Aggregation.group("peptideRef").count().as("count")).withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build()), CollectionName,
                BasicDBObject.class);

        return (long) a.getMappedResults().size();
    }
}
