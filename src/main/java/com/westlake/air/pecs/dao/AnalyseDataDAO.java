package com.westlake.air.pecs.dao;

import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.query.AnalyseDataQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-07 20:50
 */
@Service
public class AnalyseDataDAO {

    public static String CollectionName = "analyseData";

    @Autowired
    MongoTemplate mongoTemplate;

    public List<AnalyseDataDO> getAllByOverviewId(String overviewId) {
        Query query = new Query(where("overviewId").is(overviewId));
        return mongoTemplate.find(query, AnalyseDataDO.class, CollectionName);
    }

    public AnalyseDataDO getMS1Data(String overviewId, String peptideRef) {
        Query query = new Query(where("overviewId").is(overviewId));
        query.addCriteria(where("peptideRef").is(peptideRef));
        return mongoTemplate.findOne(query, AnalyseDataDO.class, CollectionName);
    }

    public AnalyseDataDO getMS2Data(String overviewId, String peptideRef, String cutInfo) {
        Query query = new Query(where("overviewId").is(overviewId));
        query.addCriteria(where("peptideRef").is(peptideRef));
        query.addCriteria(where("cutInfo").is(cutInfo));
        return mongoTemplate.findOne(query, AnalyseDataDO.class, CollectionName);
    }

    public List<AnalyseDataDO> getAll(AnalyseDataQuery query) {
        return mongoTemplate.find(buildQueryWithoutPage(query), AnalyseDataDO.class, CollectionName);
    }

    public List<AnalyseDataDO> getList(AnalyseDataQuery query) {
        return mongoTemplate.find(buildQuery(query), AnalyseDataDO.class, CollectionName);
    }

    public long count(AnalyseDataQuery query) {
        return mongoTemplate.count(buildQueryWithoutPage(query), AnalyseDataDO.class, CollectionName);
    }

    public AnalyseDataDO getById(String id) {
        return mongoTemplate.findById(id, AnalyseDataDO.class, CollectionName);
    }

    public AnalyseDataDO insert(AnalyseDataDO convData) {
        mongoTemplate.insert(convData, CollectionName);
        return convData;
    }

    public List<AnalyseDataDO> insert(List<AnalyseDataDO> convList) {
        mongoTemplate.insert(convList, CollectionName);
        return convList;
    }

    public AnalyseDataDO update(AnalyseDataDO analyseDataDO) {
        mongoTemplate.save(analyseDataDO, CollectionName);
        return analyseDataDO;
    }

    public void delete(String id) {
        Query query = new Query(where("id").is(id));
        mongoTemplate.remove(query, AnalyseDataDO.class, CollectionName);
    }

    public void deleteAllByOverviewId(String overviewId) {
        Query query = new Query(where("overviewId").is(overviewId));
        mongoTemplate.remove(query, AnalyseDataDO.class, CollectionName);
    }

    private Query buildQuery(AnalyseDataQuery analyseDataQuery) {
        Query query = buildQueryWithoutPage(analyseDataQuery);

        query.skip((analyseDataQuery.getPageNo() - 1) * analyseDataQuery.getPageSize());
        if(analyseDataQuery.getPageSize() != -1){
            query.limit(analyseDataQuery.getPageSize());
        }
        //默认没有排序功能(排序会带来极大的性能开销)
//        query.with(new Sort(transitionQuery.getOrderBy(), transitionQuery.getSortColumn()));
        return query;
    }

    private Query buildQueryWithoutPage(AnalyseDataQuery analyseDataQuery) {
        Query query = new Query();
        if (analyseDataQuery.getId() != null) {
            query.addCriteria(where("id").is(analyseDataQuery.getId()));
        }
        if (analyseDataQuery.getOverviewId() != null) {
            query.addCriteria(where("overviewId").is(analyseDataQuery.getOverviewId()));
        }
        if (analyseDataQuery.getTransitionId() != null) {
            query.addCriteria(where("transitionId").is(analyseDataQuery.getTransitionId()));
        }
        if (analyseDataQuery.getMsLevel() != null) {
            query.addCriteria(where("msLevel").is(analyseDataQuery.getMsLevel()));
        }
        if (analyseDataQuery.getPeptideRef() != null) {
            query.addCriteria(where("peptideRef").is(analyseDataQuery.getPeptideRef()));
        }
        if (analyseDataQuery.getIsHit() != null) {
            query.addCriteria(where("isHit").is(analyseDataQuery.getIsHit()));
        }

        return query;
    }

}
