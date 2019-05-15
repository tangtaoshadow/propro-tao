package com.westlake.air.propro.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VMProperties {

    @Value("${dbpath}")
    private String dbpath;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    public String getDbpath() {
        if(StringUtils.isEmpty(dbpath)){
            dbpath = "localhost:27017";
        }
        return dbpath;
    }

    public void setDbpath(String dbpath) {
        this.dbpath = dbpath;
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
