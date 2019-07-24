package com.westlake.air.propro.controller;


import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaoController {

    @PostMapping("/test01")
    @RequiresRoles("admin")
    public String test(){
        return "test...";
    }
}
