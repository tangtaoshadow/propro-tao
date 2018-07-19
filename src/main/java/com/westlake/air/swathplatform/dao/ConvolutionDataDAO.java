package com.westlake.air.swathplatform.dao;

import com.mongodb.BasicDBObject;
import com.westlake.air.swathplatform.domain.bean.TargetTransition;
import com.westlake.air.swathplatform.domain.db.ConvolutionDataDO;
import com.westlake.air.swathplatform.domain.db.TransitionDO;
import com.westlake.air.swathplatform.domain.query.ConvolutionDataQuery;
import com.westlake.air.swathplatform.domain.query.TransitionQuery;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-07 20:50
 */
@Service
public class ConvolutionDataDAO {

    public static String CollectionName = "convolutionData";

    @Autowired
    MongoTemplate mongoTemplate;

    public List<ConvolutionDataDO> getAllByExperimentId(String expId){
        Query query = new Query(where("expId").is(expId));
        return mongoTemplate.find(query, ConvolutionDataDO.class, CollectionName);
    }

    public List<ConvolutionDataDO> getList(ConvolutionDataQuery query) {
        return mongoTemplate.find(buildQuery(query), ConvolutionDataDO.class, CollectionName);
    }

    public long count(ConvolutionDataQuery query){
        return mongoTemplate.count(buildQueryWithoutPage(query), ConvolutionDataDO.class, CollectionName);
    }

    public ConvolutionDataDO getById(String id) {
        return mongoTemplate.findById(id, ConvolutionDataDO.class, CollectionName);
    }

    public ConvolutionDataDO insert(ConvolutionDataDO convData) {
        mongoTemplate.insert(convData, CollectionName);
        return convData;
    }

    public List<ConvolutionDataDO> insert(List<ConvolutionDataDO> convList) {
        mongoTemplate.insert(convList, CollectionName);
        return convList;
    }

    public ConvolutionDataDO update(ConvolutionDataDO convolutionDataDO) {
        mongoTemplate.save(convolutionDataDO, CollectionName);
        return convolutionDataDO;
    }

    public void delete(String id) {
        Query query = new Query(where("id").is(id));
        mongoTemplate.remove(query,ConvolutionDataDO.class, CollectionName);
    }

    public void deleteAllByExpId(String expId) {
        Query query = new Query(where("expId").is(expId));
        mongoTemplate.remove(query, ConvolutionDataDO.class, CollectionName);
    }

    private Query buildQuery(ConvolutionDataQuery convolutionDataQuery) {
        Query query = buildQueryWithoutPage(convolutionDataQuery);

        query.skip((convolutionDataQuery.getPageNo() - 1) * convolutionDataQuery.getPageSize());
        query.limit(convolutionDataQuery.getPageSize());
        //默认没有排序功能(排序会带来极大的性能开销)
//        query.with(new Sort(transitionQuery.getOrderBy(), transitionQuery.getSortColumn()));
        return query;
    }

    private Query buildQueryWithoutPage(ConvolutionDataQuery convolutionDataQuery) {
        Query query = new Query();
        if (convolutionDataQuery.getId() != null) {
            query.addCriteria(where("id").is(convolutionDataQuery.getId()));
        }
        if (convolutionDataQuery.getExpId() != null) {
            query.addCriteria(where("expId").is(convolutionDataQuery.getExpId()));
        }
        if (convolutionDataQuery.getTransitionId() != null) {
            query.addCriteria(where("transitionId").is(convolutionDataQuery.getTransitionId()));
        }
        if (convolutionDataQuery.getMsLevel() != null) {
            query.addCriteria(where("msLevel").is(convolutionDataQuery.getMsLevel()));
        }

        return query;
    }

}
