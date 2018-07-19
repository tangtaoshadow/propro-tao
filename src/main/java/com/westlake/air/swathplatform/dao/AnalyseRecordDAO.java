package com.westlake.air.swathplatform.dao;

import com.westlake.air.swathplatform.domain.db.AnalyseRecordDO;
import com.westlake.air.swathplatform.domain.query.AnalyseRecordQuery;
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
public class AnalyseRecordDAO {

    public static String CollectionName = "analyseRecord";

    @Autowired
    MongoTemplate mongoTemplate;

    public List<AnalyseRecordDO> getAllByExperimentId(String expId){
        Query query = new Query(where("expId").is(expId));
        return mongoTemplate.find(query, AnalyseRecordDO.class, CollectionName);
    }

    public List<AnalyseRecordDO> getList(AnalyseRecordQuery query) {
        return mongoTemplate.find(buildQuery(query), AnalyseRecordDO.class, CollectionName);
    }

    public long count(AnalyseRecordQuery query){
        return mongoTemplate.count(buildQueryWithoutPage(query), AnalyseRecordDO.class, CollectionName);
    }

    public AnalyseRecordDO getById(String id) {
        return mongoTemplate.findById(id, AnalyseRecordDO.class, CollectionName);
    }

    public AnalyseRecordDO insert(AnalyseRecordDO recordDO) {
        mongoTemplate.insert(recordDO, CollectionName);
        return recordDO;
    }

    public List<AnalyseRecordDO> insert(List<AnalyseRecordDO> recordList) {
        mongoTemplate.insert(recordList, CollectionName);
        return recordList;
    }

    public AnalyseRecordDO update(AnalyseRecordDO recordDO) {
        mongoTemplate.save(recordDO, CollectionName);
        return recordDO;
    }

    public void delete(String id) {
        Query query = new Query(where("id").is(id));
        mongoTemplate.remove(query,AnalyseRecordDO.class, CollectionName);
    }

    public void deleteAllByExperimentId(String expId) {
        Query query = new Query(where("expId").is(expId));
        mongoTemplate.remove(query, AnalyseRecordDO.class, CollectionName);
    }

    private Query buildQuery(AnalyseRecordQuery targetQuery) {
        Query query = buildQueryWithoutPage(targetQuery);

        query.skip((targetQuery.getPageNo() - 1) * targetQuery.getPageSize());
        query.limit(targetQuery.getPageSize());
        //默认没有排序功能(排序会带来极大的性能开销)
//        query.with(new Sort(transitionQuery.getOrderBy(), transitionQuery.getSortColumn()));
        return query;
    }

    private Query buildQueryWithoutPage(AnalyseRecordQuery targetQuery) {
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
