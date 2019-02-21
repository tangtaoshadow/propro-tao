package com.westlake.air.propro.dao;

import com.westlake.air.propro.domain.db.simple.SimpleScanIndex;
import com.westlake.air.propro.domain.db.ScanIndexDO;
import com.westlake.air.propro.domain.query.ScanIndexQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
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

    public List<ScanIndexDO> getAllByExperimentId(String experimentId) {
        Query query = new Query(where("experimentId").is(experimentId));
        return mongoTemplate.find(query, ScanIndexDO.class, CollectionName);
    }

    public List<ScanIndexDO> getList(ScanIndexQuery query) {
        return mongoTemplate.find(buildQuery(query), ScanIndexDO.class, CollectionName);
    }

    public List<ScanIndexDO> getAll(ScanIndexQuery query) {
        return mongoTemplate.find(buildQueryWithoutPage(query), ScanIndexDO.class, CollectionName);
    }

    public ScanIndexDO getOne(ScanIndexQuery query) {
        return mongoTemplate.findOne(buildQueryWithoutPage(query), ScanIndexDO.class, CollectionName);
    }

    public List<ScanIndexDO> getAllForOutput(ScanIndexQuery query) {
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
        mongoTemplate.remove(query, ScanIndexDO.class, CollectionName);
    }

    public void deleteAllByExperimentId(String experimentId) {
        Query query = new Query(where("experimentId").is(experimentId));
        mongoTemplate.remove(query, ScanIndexDO.class, CollectionName);
    }

    public void deleteAllSwathIndexByExperimentId(String experimentId) {
        Query query = new Query();
        query.addCriteria(where("experimentId").is(experimentId));
        query.addCriteria(where("msLevel").is(0));
        mongoTemplate.remove(query, ScanIndexDO.class, CollectionName);
    }

    private Query buildQuery(ScanIndexQuery scanIndexQuery) {
        Query query = buildQueryWithoutPage(scanIndexQuery);
        query.skip((scanIndexQuery.getPageNo() - 1) * scanIndexQuery.getPageSize());
        query.limit(scanIndexQuery.getPageSize());
//        query.with(new Sort(scanIndexQuery.getOrderBy(), scanIndexQuery.getSortColumn()));
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
        if (scanIndexQuery.getMsLevel() != null) {
            query.addCriteria(where("msLevel").is(scanIndexQuery.getMsLevel()));
        }
        if (scanIndexQuery.getRt() != null) {
            query.addCriteria(where("rt").is(scanIndexQuery.getRt()));
        }
        Criteria c = null;
        if (scanIndexQuery.getRtStart() != null) {
            c = where("rt").gte(scanIndexQuery.getRtStart());
        }
        if (scanIndexQuery.getRtEnd() != null) {
            if (c != null) {
                c.lte(scanIndexQuery.getRtEnd());
            } else {
                c = where("rt").lte(scanIndexQuery.getRtEnd());
            }
        }
        if (c != null) {
            query.addCriteria(c);
        }
        if (scanIndexQuery.getParentNum() != null && scanIndexQuery.getParentNum() != null) {
            query.addCriteria(where("parentNum").is(scanIndexQuery.getParentNum()));
        }
        if (scanIndexQuery.getTargetPrecursorMz() != null) {
            query.addCriteria(where("precursorMzStart").lte(scanIndexQuery.getTargetPrecursorMz()));
            query.addCriteria(where("precursorMzEnd").gte(scanIndexQuery.getTargetPrecursorMz()));
        }
        if (scanIndexQuery.getPrecursorMzStart() != null && scanIndexQuery.getPrecursorMzStart() != null) {
            query.addCriteria(where("precursorMzStart").is(scanIndexQuery.getPrecursorMzStart()));
        }
        if (scanIndexQuery.getPrecursorMzEnd() != null && scanIndexQuery.getPrecursorMzEnd() != null) {
            query.addCriteria(where("precursorMzEnd").is(scanIndexQuery.getPrecursorMzEnd()));
        }
        return query;
    }
}
