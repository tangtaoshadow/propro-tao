package com.westlake.air.propro.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VMProperties {

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${dingtalk.robot}")
    private String dingtalkRobot;

    public String getDingtalkRobot() {
        if(StringUtils.isEmpty(dingtalkRobot)){
            dingtalkRobot = "https://oapi.dingtalk.com/robot/send?access_token=f2fb029431f174e678106b30c2db5fb0e40e921999386a61031bf864f18beb77";
        }
        return dingtalkRobot;
    }

    public void setDingtalkRobot(String dingtalkRobot) {
        this.dingtalkRobot = dingtalkRobot;
    }

    public String getAdminUsername() {
        if(StringUtils.isEmpty(adminUsername)){
            adminUsername = "Admin";
        }
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        if(StringUtils.isEmpty(adminPassword)){
            adminPassword = "propro";
        }
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

}
