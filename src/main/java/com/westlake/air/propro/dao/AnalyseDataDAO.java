package com.westlake.air.propro.dao;

import com.westlake.air.propro.domain.db.AnalyseDataDO;
import com.westlake.air.propro.domain.db.simple.MatchedPeptide;
import com.westlake.air.propro.domain.db.simple.SimpleScores;
import com.westlake.air.propro.domain.query.AnalyseDataQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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

    public List<SimpleScores> getSimpleScoresByOverviewId(String overviewId){
        AnalyseDataQuery query = new AnalyseDataQuery(overviewId);
        return mongoTemplate.find(buildQueryWithoutPage(query), SimpleScores.class, CollectionName);
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

    public List<MatchedPeptide> getAllMatchedPeptide(AnalyseDataQuery query) {
        return mongoTemplate.find(buildQueryWithoutPage(query), MatchedPeptide.class, CollectionName);
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

        if(!StringUtils.isEmpty(analyseDataQuery.getSortColumn())){
            query.with(new Sort(analyseDataQuery.getOrderBy(), analyseDataQuery.getSortColumn()));
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
            query.addCriteria(where("peptideId").is(analyseDataQuery.getTransitionId()));
        }
        if (analyseDataQuery.getMsLevel() != null) {
            query.addCriteria(where("msLevel").is(analyseDataQuery.getMsLevel()));
        }
        if (analyseDataQuery.getPeptideRef() != null) {
            query.addCriteria(where("peptideRef").is(analyseDataQuery.getPeptideRef()));
        }
        if (analyseDataQuery.getPeptideRef() != null) {
            query.addCriteria(where("proteinName").is(analyseDataQuery.getProteinName()));
        }
        if (analyseDataQuery.getIsDecoy() != null) {
            query.addCriteria(where("isDecoy").is(analyseDataQuery.getIsDecoy()));
        }
        if (analyseDataQuery.getMzStart() != null && analyseDataQuery.getMzEnd() != null) {
            query.addCriteria(where("mz").gte(analyseDataQuery.getMzStart()).lt(analyseDataQuery.getMzEnd()));
        }
        if (analyseDataQuery.getFdrStart() != null || analyseDataQuery.getFdrEnd() != null) {
            query.addCriteria(where("fdr").gte(analyseDataQuery.getFdrStart()==null?0:analyseDataQuery.getFdrStart()).lte(analyseDataQuery.getFdrEnd()==null?1:analyseDataQuery.getFdrEnd()));
        }
        if (analyseDataQuery.getIdentifiedStatus() != null) {
            query.addCriteria(where("identifiedStatus").in(analyseDataQuery.getIdentifiedStatus()));
        }
        return query;
    }

}
