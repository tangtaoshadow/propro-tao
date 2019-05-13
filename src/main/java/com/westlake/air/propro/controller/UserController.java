package com.westlake.air.propro.controller;

import com.westlake.air.propro.domain.db.UserDO;
import com.westlake.air.propro.domain.query.ExperimentQuery;
import com.westlake.air.propro.domain.query.ProjectQuery;
import com.westlake.air.propro.service.ExperimentService;
import com.westlake.air.propro.service.ProjectService;
import com.westlake.air.propro.service.UserService;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("user")
public class UserController extends BaseController {

    @Autowired
    UserService userService;
    @Autowired
    ProjectService projectService;
    @Autowired
    ExperimentService experimentService;

    @RequestMapping(value = "/profile")
    String profile(Model model, RedirectAttributes redirectAttributes) {
        Object object = SecurityUtils.getSubject().getPrincipal();
        if (object != null) {
            UserDO userDO = userService.findByUsername(((UserDO) object).getUsername());

            model.addAttribute("user", userDO);

            model.addAttribute("projectCount", projectService.count(new ProjectQuery(userDO.getUsername())));
            model.addAttribute("expCount", experimentService.count(new ExperimentQuery(userDO.getUsername())));
        }
        return "user/profile";
    }
}
