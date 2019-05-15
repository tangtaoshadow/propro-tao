package com.westlake.air.propro.dao;

import com.westlake.air.propro.domain.db.UserDO;
import com.westlake.air.propro.domain.query.UserQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class UserDAO {

    public static String CollectionName = "user";

    @Autowired
    MongoTemplate mongoTemplate;

    public List<UserDO> getList(UserQuery query) {
        return mongoTemplate.find(buildQuery(query), UserDO.class, CollectionName);
    }

    public long count(UserQuery query) {
        return mongoTemplate.count(buildQueryWithoutPage(query), UserDO.class, CollectionName);
    }

    public UserDO getById(String id) {
        return mongoTemplate.findById(id, UserDO.class, CollectionName);
    }

    public UserDO getByUsername(String username) {
        UserQuery query = new UserQuery();
        query.setUsername(username);
        return mongoTemplate.findOne(buildQuery(query), UserDO.class, CollectionName);
    }

    public UserDO insert(UserDO userDO) {
        mongoTemplate.insert(userDO, CollectionName);
        return userDO;
    }

    public List<UserDO> insert(List<UserDO> users) {
        mongoTemplate.insert(users, CollectionName);
        return users;
    }

    public UserDO update(UserDO userDO) {
        mongoTemplate.save(userDO, CollectionName);
        return userDO;
    }

    public void delete(String id) {
        Query query = new Query(where("id").is(id));
        mongoTemplate.remove(query, UserDO.class, CollectionName);
    }

    private Query buildQuery(UserQuery targetQuery) {
        Query query = buildQueryWithoutPage(targetQuery);

        query.skip((targetQuery.getPageNo() - 1) * targetQuery.getPageSize());
        query.limit(targetQuery.getPageSize());
        //默认没有排序功能(排序会带来极大的性能开销)
        if(!StringUtils.isEmpty(targetQuery.getSortColumn())){
            query.with(new Sort(targetQuery.getOrderBy(), targetQuery.getSortColumn()));
        }
        return query;
    }

    private Query buildQueryWithoutPage(UserQuery targetQuery) {
        Query query = new Query();
        if (targetQuery.getId() != null) {
            query.addCriteria(where("id").is(targetQuery.getId()));
        }
        if (targetQuery.getNick() != null) {
            query.addCriteria(where("nick").is(targetQuery.getNick()));
        }
        if (targetQuery.getUsername() != null) {
            query.addCriteria(where("username").is(targetQuery.getUsername()));
        }
        if (targetQuery.getEmail() != null) {
            query.addCriteria(where("email").is(targetQuery.getEmail()));
        }
        if (targetQuery.getTelephone() != null) {
            query.addCriteria(where("telephone").is(targetQuery.getTelephone()));
        }
        if (targetQuery.getUniversity() != null) {
            query.addCriteria(where("university").is(targetQuery.getUniversity()));
        }

        return query;
    }
}
