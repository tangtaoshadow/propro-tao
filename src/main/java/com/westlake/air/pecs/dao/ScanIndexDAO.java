package com.westlake.air.pecs.dao;

import com.westlake.air.pecs.domain.bean.SimpleScanIndex;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.domain.query.ScanIndexQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-11 20:04
 */
@Service
public class ScanIndexDAO {

    public static String CollectionName = "scanIndex";

    @Autowired
    MongoTemplate mongoTemplate;

    public long count(ScanIndexQuery query) {
        return mongoTemplate.count(buildQueryWithoutPage(query), ScanIndexDO.class);
    }

    public List<ScanIndexDO> getAllByExperimentId(String experimentId){
        Query query = new Query(where("experimentId").is(experimentId));
        return mongoTemplate.find(query, ScanIndexDO.class, CollectionName);
    }

    public List<ScanIndexDO> getList(ScanIndexQuery query) {
        return mongoTemplate.find(buildQuery(query), ScanIndexDO.class, CollectionName);
    }

    public List<ScanIndexDO> getAll(ScanIndexQuery query) {
        return mongoTemplate.find(buildQueryWithoutPage(query), ScanIndexDO.class, CollectionName);
    }

    public List<SimpleScanIndex> getSimpleAll(ScanIndexQuery query) {
        return mongoTemplate.find(buildQueryWithoutPage(query), SimpleScanIndex.class, CollectionName);
    }

    public List<SimpleScanIndex> getSimpleList(ScanIndexQuery query) {
        return mongoTemplate.find(buildQuery(query), SimpleScanIndex.class, CollectionName);
    }

    public ScanIndexDO getById(String id) {
        return mongoTemplate.findById(id, ScanIndexDO.class, CollectionName);
    }

    public ScanIndexDO insert(ScanIndexDO scanIndexDO) {
        mongoTemplate.insert(scanIndexDO, CollectionName);
        return scanIndexDO;
    }

    public List<ScanIndexDO> insert(List<ScanIndexDO> scanIndexList) {
        mongoTemplate.insert(scanIndexList, CollectionName);
        return scanIndexList;
    }

    public ScanIndexDO update(ScanIndexDO scanIndexDO) {
        mongoTemplate.save(scanIndexDO, CollectionName);
        return scanIndexDO;
    }

    public void delete(String id) {
        Query query = new Query(where("id").is(id));
        mongoTemplate.remove(query,ScanIndexDO.class, CollectionName);
    }

    public void deleteAllByExperimentId(String experimentId) {
        Query query = new Query(where("experimentId").is(experimentId));
        mongoTemplate.remove(query, ScanIndexDO.class, CollectionName);
    }

    private Query buildQuery(ScanIndexQuery scanIndexQuery) {
        Query query = buildQueryWithoutPage(scanIndexQuery);
        query.skip((scanIndexQuery.getPageNo() - 1) * scanIndexQuery.getPageSize());
        query.limit(scanIndexQuery.getPageSize());
        query.with(new Sort(scanIndexQuery.getOrderBy(), scanIndexQuery.getSortColumn()));
        return query;
    }

    private Query buildQueryWithoutPage(ScanIndexQuery scanIndexQuery) {
        Query query = new Query();
        if (scanIndexQuery.getExperimentId() != null) {
            query.addCriteria(where("experimentId").is(scanIndexQuery.getExperimentId()));
        }
        if (scanIndexQuery.getId() != null) {
            query.addCriteria(where("id").is(scanIndexQuery.getId()));
        }
        if (scanIndexQuery.getNumStart() != null) {
            query.addCriteria(where("num").gte(scanIndexQuery.getNumStart()).lte(scanIndexQuery.getNumEnd()));
        }
        if (scanIndexQuery.getMsLevel() != null) {
            query.addCriteria(where("msLevel").is(scanIndexQuery.getMsLevel()));
        }
        if (scanIndexQuery.getRtStart() != null && scanIndexQuery.getRtEnd() != null) {
            query.addCriteria(where("rt").gte(scanIndexQuery.getRtStart()).lte(scanIndexQuery.getRtEnd()));
        }
        return query;
    }
}