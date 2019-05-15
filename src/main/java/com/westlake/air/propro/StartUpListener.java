package com.westlake.air.propro;

import com.westlake.air.propro.domain.db.UserDO;
import com.westlake.air.propro.service.UserService;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.HashSet;
import java.util.Set;

public class StartUpListener implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        UserService userService = contextRefreshedEvent.getApplicationContext().getBean(UserService.class);
        UserDO userDO = userService.getByUsername("Admin");
        if(userDO == null){
            userDO = new UserDO();
            userDO.setUsername("Admin");
            String randomSalt = new SecureRandomNumberGenerator().nextBytes().toHex();
            String result = new Md5Hash("propro", randomSalt, 3).toString();
            userDO.setSalt(randomSalt);
            userDO.setPassword(result);
            Set<String> roles = new HashSet<>();
            roles.add("admin");
            userDO.setRoles(roles);
            userService.register(userDO);
        }
    }
}
