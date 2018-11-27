package com.westlake.air.pecs.dao;

import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.ScoresDO;
import com.westlake.air.pecs.domain.db.simple.SimpleScores;
import com.westlake.air.pecs.domain.query.LibraryQuery;
import com.westlake.air.pecs.domain.query.ScoresQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class ScoresDAO {

    public static String CollectionName = "scores";

    @Autowired
    MongoTemplate mongoTemplate;

    public List<ScoresDO> getList(ScoresQuery scoresQuery) {
        return mongoTemplate.find(buildQuery(scoresQuery), ScoresDO.class, CollectionName);
    }

    public long count(ScoresQuery query) {
        return mongoTemplate.count(buildQueryWithoutPage(query), ScoresDO.class, CollectionName);
    }

    public ScoresDO getById(String id) {
        return mongoTemplate.findById(id, ScoresDO.class, CollectionName);
    }

    public ScoresDO getByPeptideRefAndIsDecoy(String overviewId, String peptideRef, Boolean isDecoy) {
        ScoresQuery query = new ScoresQuery();
        query.setOverviewId(overviewId);
        query.setPeptideRef(peptideRef);
        query.setIsDecoy(isDecoy);
        return mongoTemplate.findOne(buildQuery(query), ScoresDO.class, CollectionName);
    }

    public List<ScoresDO> getAllByOverviewId(String overviewId) {
        ScoresQuery query = new ScoresQuery();
        query.setOverviewId(overviewId);
        return mongoTemplate.find(buildQueryWithoutPage(query), ScoresDO.class, CollectionName);
    }

    public List<SimpleScores> getSimpleAllByOverviewId(String overviewId) {
        ScoresQuery query = new ScoresQuery();
        query.setOverviewId(overviewId);
        return mongoTemplate.find(buildQueryWithoutPage(query), SimpleScores.class, CollectionName);
    }

    public ScoresDO insert(ScoresDO scoresDO) {
        mongoTemplate.insert(scoresDO, CollectionName);
        return scoresDO;
    }

    public List<ScoresDO> insert(List<ScoresDO> scoresList) {
        mongoTemplate.insert(scoresList, CollectionName);
        return scoresList;
    }

    public ScoresDO update(ScoresDO scoresDO) {
        mongoTemplate.save(scoresDO, CollectionName);
        return scoresDO;
    }

    public void delete(String id) {
        Query query = new Query(where("id").is(id));
        mongoTemplate.remove(query, ScoresDO.class, CollectionName);
    }

    public void deleteAllByOverviewId(String overviewId) {
        Query query = new Query(where("overviewId").is(overviewId));
        mongoTemplate.remove(query, ScoresDO.class, CollectionName);
    }

    private Query buildQuery(ScoresQuery targetQuery) {
        Query query = buildQueryWithoutPage(targetQuery);

        query.skip((targetQuery.getPageNo() - 1) * targetQuery.getPageSize());
        query.limit(targetQuery.getPageSize());

        if(!StringUtils.isEmpty(targetQuery.getSortColumn())){
            query.with(new Sort(targetQuery.getOrderBy(), targetQuery.getSortColumn()));
        }
        return query;
    }

    private Query buildQueryWithoutPage(ScoresQuery targetQuery) {
        Query query = new Query();
        if (targetQuery.getId() != null) {
            query.addCriteria(where("id").is(targetQuery.getId()));
        }
        if (targetQuery.getOverviewId() != null) {
            query.addCriteria(where("overviewId").is(targetQuery.getOverviewId()));
        }
        if (targetQuery.getPeptideRef() != null) {
            query.addCriteria(where("peptideRef").is(targetQuery.getPeptideRef()));
        }
        if (targetQuery.getIsDecoy() != null) {
            query.addCriteria(where("isDecoy").is(targetQuery.getIsDecoy()));
        }

        return query;
    }
}
