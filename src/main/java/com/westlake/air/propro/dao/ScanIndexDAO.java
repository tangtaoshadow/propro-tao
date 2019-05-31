package com.westlake.air.propro.dao;

import com.westlake.air.propro.domain.db.simple.SimpleScanIndex;
import com.westlake.air.propro.domain.db.ScanIndexDO;
import com.westlake.air.propro.domain.query.ScanIndexQuery;
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
public class ScanIndexDAO extends BaseDAO<ScanIndexDO, ScanIndexQuery>{

    public static String CollectionName = "scanIndex";

    @Override
    protected String getCollectionName() {
        return CollectionName;
    }

    @Override
    protected Class getDomainClass() {
        return ScanIndexDO.class;
    }

    @Override
    protected boolean allowSort() {
        return false;
    }

    @Override
    protected Query buildQueryWithoutPage(ScanIndexQuery scanIndexQuery) {
        Query query = new Query();
        if (scanIndexQuery.getExpId() != null) {
            query.addCriteria(where("expId").is(scanIndexQuery.getExpId()));
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


    public List<ScanIndexDO> getAllByExpId(String experimentId) {
        Query query = new Query(where("expId").is(experimentId));
        return mongoTemplate.find(query, ScanIndexDO.class, CollectionName);
    }

    public List<SimpleScanIndex> getSimpleAll(ScanIndexQuery query) {
        return mongoTemplate.find(buildQueryWithoutPage(query), SimpleScanIndex.class, CollectionName);
    }

    public List<SimpleScanIndex> getSimpleList(ScanIndexQuery query) {
        return mongoTemplate.find(buildQuery(query), SimpleScanIndex.class, CollectionName);
    }

    public void deleteAllByExperimentId(String experimentId) {
        Query query = new Query(where("expId").is(experimentId));
        mongoTemplate.remove(query, ScanIndexDO.class, CollectionName);
    }


}
