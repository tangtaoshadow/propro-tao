package com.westlake.air.propro.service.impl;

import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.dao.UserDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.UserDO;
import com.westlake.air.propro.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.crypto.Data;
import java.util.Date;
import java.util.Set;

@Service("userService")
public class UserServiceImpl implements UserService {

    @Autowired
    UserDAO userDAO;

    @Override
    public UserDO findByUsername(String username) {
        return userDAO.getByUsername(username);
    }

    @Override
    public UserDO update(UserDO userDO) {
        return userDAO.update(userDO);
    }

    @Override
    public Set<String> getRoleByUserId(String uid) {
        UserDO user = userDAO.getById(uid);
        if(user != null){
            return user.getRoles();
        }
        return null;
    }

    @Override
    public Set<String> getPermsByUserId(String uid) {
        UserDO user = userDAO.getById(uid);
        if(user != null){
            return user.getPerms();
        }
        return null;
    }

    @Override
    public ResultDO<UserDO> register(UserDO userDO) {
        UserDO existed = findByUsername(userDO.getEmail());
        if(existed != null){
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
