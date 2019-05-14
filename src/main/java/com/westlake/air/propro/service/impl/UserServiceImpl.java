package com.westlake.air.propro.service.impl;

import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.dao.UserDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.UserDO;
import com.westlake.air.propro.domain.query.UserQuery;
import com.westlake.air.propro.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Service("userService")
public class UserServiceImpl implements UserService {

    public final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    UserDAO userDAO;

    @Override
    public UserDO getByUsername(String username) {
        return userDAO.getByUsername(username);
    }

    @Override
    public ResultDO<List<UserDO>> getList(UserQuery query) {
        List<UserDO> users = userDAO.getList(query);
        long totalCount = userDAO.count(query);
        ResultDO<List<UserDO>> resultDO = new ResultDO<>(true);
        resultDO.setModel(users);
        resultDO.setTotalNum(totalCount);
        resultDO.setPageSize(query.getPageSize());
        return resultDO;
    }

    @Override
    public ResultDO<UserDO> getById(String userId) {
        if (userId == null || userId.isEmpty()) {
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }

        try {
            UserDO user = userDAO.getById(userId);
            if (user == null) {
                return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
            } else {
                return ResultDO.build(user);
            }
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.QUERY_ERROR);
        }
    }

    @Override
    public ResultDO delete(String userId) {
        if (userId == null || userId.isEmpty()) {
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        try {
            userDAO.delete(userId);
            return new ResultDO(true);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.DELETE_ERROR);
        }
    }

    @Override
    public UserDO update(UserDO userDO) {
        return userDAO.update(userDO);
    }

    @Override
    public Set<String> getRoleByUserId(String uid) {
        UserDO user = userDAO.getById(uid);
        if (user != null) {
            return user.getRoles();
        }
        return null;
    }

    @Override
    public Set<String> getPermsByUserId(String uid) {
        UserDO user = userDAO.getById(uid);
        if (user != null) {
            return user.getPerms();
        }
        return null;
    }

    @Override
    public ResultDO<UserDO> register(UserDO userDO) {
        UserDO existed = getByUsername(userDO.getEmail());
        if (existed != null) {
            return ResultDO.buildError(ResultCode.EMAIL_ALREADY_EXISTED);
        }
        userDO.setCreated(new Date());
        userDO.setUpdated(new Date());
        UserDO user = userDAO.insert(userDO);
        ResultDO<UserDO> result = new ResultDO<>();
        result.setModel(user);
        result.setSuccess(true);
        return result;
    }
}
