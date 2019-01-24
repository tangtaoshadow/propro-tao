package com.westlake.air.propro.dao;

import com.westlake.air.propro.domain.db.AnalyseOverviewDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.domain.query.TaskQuery;
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
public class TaskDAO {

    public static String CollectionName = "task";

    @Autowired
    MongoTemplate mongoTemplate;

    public List<TaskDO> getList(TaskQuery taskQuery) {
        return mongoTemplate.find(buildQuery(taskQuery), TaskDO.class, CollectionName);
    }

    public long count(TaskQuery query) {
        return mongoTemplate.count(buildQueryWithoutPage(query), AnalyseOverviewDO.class, CollectionName);
    }

    public TaskDO getById(String id) {
        return mongoTemplate.findById(id, TaskDO.class, CollectionName);
    }

    public TaskDO insert(TaskDO taskDO) {
        mongoTemplate.insert(taskDO, CollectionName);
        return taskDO;
    }

    public List<TaskDO> insert(List<TaskDO> taskList) {
        mongoTemplate.insert(taskList, CollectionName);
        return taskList;
    }

    public TaskDO update(TaskDO taskDO) {
        mongoTemplate.save(taskDO, CollectionName);
        return taskDO;
    }

    public void delete(String id) {
        Query query = new Query(where("id").is(id));
        mongoTemplate.remove(query, TaskDO.class, CollectionName);
    }

    private Query buildQuery(TaskQuery targetQuery) {
        Query query = buildQueryWithoutPage(targetQuery);

        query.skip((targetQuery.getPageNo() - 1) * targetQuery.getPageSize());
        query.limit(targetQuery.getPageSize());
        //默认没有排序功能(排序会带来极大的性能开销)
        if(!StringUtils.isEmpty(targetQuery.getSortColumn())){
            query.with(new Sort(targetQuery.getOrderBy(), targetQuery.getSortColumn()));
        }
        return query;
    }

    private Query buildQueryWithoutPage(TaskQuery targetQuery) {
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
        if (targetQuery.getCreator() != null) {
            query.addCriteria(where("creator").is(targetQuery.getCreator()));
        }
        if (targetQuery.getName() != null) {
            query.addCriteria(where("name").is(targetQuery.getName()));
        }
        if (targetQuery.getOverviewId() != null) {
            query.addCriteria(where("overviewId").is(targetQuery.getOverviewId()));
        }
        if (targetQuery.getTaskTemplate() != null) {
            query.addCriteria(where("taskTemplate").is(targetQuery.getTaskTemplate()));
        }
        if (targetQuery.getStatusList() != null) {
            query.addCriteria(where("status").in(targetQuery.getStatusList()));
        }

        return query;
    }

}
