package com.westlake.air.propro.service;

import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.UserDO;

import java.util.Set;

public interface UserService {

    UserDO findByUsername(String username);

    Set<String> getRoleByUserId(String uid);

    Set<String> getPermsByUserId(String uid);

    ResultDO<UserDO> register(UserDO userDO);
}
