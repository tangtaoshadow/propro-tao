package com.westlake.air.swathplatform.dao;

import com.westlake.air.swathplatform.domain.db.AnalyseOverviewDO;
import com.westlake.air.swathplatform.domain.query.AnalyseOverviewQuery;
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
public class AnalyseOverviewDAO {

    public static String CollectionName = "analyseOverview";

    @Autowired
    MongoTemplate mongoTemplate;

    public List<AnalyseOverviewDO> getAllByExperimentId(String expId){
        Query query = new Query(where("expId").is(expId));
        return mongoTemplate.find(query, AnalyseOverviewDO.class, CollectionName);
    }

    public List<AnalyseOverviewDO> getList(AnalyseOverviewQuery query) {
        return mongoTemplate.find(buildQuery(query), AnalyseOverviewDO.class, CollectionName);
    }

    public long count(AnalyseOverviewQuery query){
        return mongoTemplate.count(buildQueryWithoutPage(query), AnalyseOverviewDO.class, CollectionName);
    }

    public AnalyseOverviewDO getById(String id) {
        return mongoTemplate.findById(id, AnalyseOverviewDO.class, CollectionName);
    }

    public AnalyseOverviewDO insert(AnalyseOverviewDO overviewDO) {
        mongoTemplate.insert(overviewDO, CollectionName);
        return overviewDO;
    }

    public List<AnalyseOverviewDO> insert(List<AnalyseOverviewDO> overviewList) {
        mongoTemplate.insert(overviewList, CollectionName);
        return overviewList;
    }

    public AnalyseOverviewDO update(AnalyseOverviewDO overviewDO) {
        mongoTemplate.save(overviewDO, CollectionName);
        return overviewDO;
    }

    public void delete(String id) {
        Query query = new Query(where("id").is(id));
        mongoTemplate.remove(query,AnalyseOverviewDO.class, CollectionName);
    }

    public void deleteAllByExperimentId(String expId) {
        Query query = new Query(where("expId").is(expId));
        mongoTemplate.remove(query, AnalyseOverviewDO.class, CollectionName);
    }

    private Query buildQuery(AnalyseOverviewQuery targetQuery) {
        Query query = buildQueryWithoutPage(targetQuery);

        query.skip((targetQuery.getPageNo() - 1) * targetQuery.getPageSize());
        query.limit(targetQuery.getPageSize());
        //默认没有排序功能(排序会带来极大的性能开销)
//        query.with(new Sort(transitionQuery.getOrderBy(), transitionQuery.getSortColumn()));
        return query;
    }

    private Query buildQueryWithoutPage(AnalyseOverviewQuery targetQuery) {
        Query query = new Query();
        if (targetQuery.getId() != null) {
            query.addCriteria(where("id").is(targetQuery.getId()));
        }
        if (targetQuery.getExpId() != null) {
            query.addCriteria(where("expId").is(targetQuery.getExpId()));
        }
        if (targetQuery.getLibraryId() != null) {
            query.addCriteria(where("libraryId").is(targetQuery.getLibraryId()));
        }

        return query;
    }

}
