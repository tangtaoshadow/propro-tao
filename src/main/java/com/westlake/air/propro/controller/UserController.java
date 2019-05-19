package com.westlake.air.propro.controller;

import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.constants.SuccessMsg;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.UserDO;
import com.westlake.air.propro.domain.query.ExperimentQuery;
import com.westlake.air.propro.domain.query.ProjectQuery;
import com.westlake.air.propro.domain.query.UserQuery;
import com.westlake.air.propro.service.ExperimentService;
import com.westlake.air.propro.service.ProjectService;
import com.westlake.air.propro.service.UserService;
import com.westlake.air.propro.utils.PasswordUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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
            UserDO userDO = userService.getByUsername(((UserDO) object).getUsername());

            model.addAttribute("user", userDO);

            if (isAdmin()) {
                model.addAttribute("projectCount", projectService.count(new ProjectQuery()));
                model.addAttribute("expCount", experimentService.count(new ExperimentQuery()));
            } else {
                model.addAttribute("projectCount", projectService.count(new ProjectQuery(userDO.getUsername())));
                model.addAttribute("expCount", experimentService.count(new ExperimentQuery(userDO.getUsername())));
            }
        }
        return "user/profile";
    }

    @RequestMapping(value = "/update",method = RequestMethod.POST)
    String update(Model model,
                  @RequestParam(value = "nick", required = false) String nick,
                  @RequestParam(value = "email", required = false) String email,
                  @RequestParam(value = "telephone", required = false) String telephone,
                  @RequestParam(value = "organization", required = false) String organization,
                  RedirectAttributes redirectAttributes) {
        String username = getCurrentUsername();
        if(username == null){
            SecurityUtils.getSubject().logout();
            return "redirect:/login/login";
        }

        UserDO user = userService.getByUsername(username);
        if(user == null){
            SecurityUtils.getSubject().logout();
            return "redirect:/login/login";
        }

        user.setNick(nick);
        user.setEmail(email);
        user.setTelephone(telephone);
        user.setOrganization(organization);
        userService.update(user);
        return "redirect:/user/profile?tab=userprofile";
    }

    @RequestMapping(value = "/changepwd",method = RequestMethod.POST)
    String changePwd(Model model,
                  @RequestParam(value = "oldPwd", required = false) String oldPwd,
                  @RequestParam(value = "newPwd", required = false) String newPwd,
                  @RequestParam(value = "repeatPwd", required = false) String repeatPwd,
                  RedirectAttributes redirectAttributes) {
        String username = getCurrentUsername();
        if(username == null){
            SecurityUtils.getSubject().logout();
            return "redirect:/login/login";
        }

        UserDO user = userService.getByUsername(username);
        if(user == null){
            SecurityUtils.getSubject().logout();
            return "redirect:/login/login";
        }

        if(!newPwd.equals(repeatPwd)){
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.NEW_PASSWORD_NOT_EQUALS_WITH_REPEAT_PASSWORD.getMessage());
            redirectAttributes.addFlashAttribute("tab","changepwd");
            return "redirect:/user/profile";
        }

        String oldMD5Pwd = PasswordUtil.getHashPassword(oldPwd, user.getSalt());
        if(!user.getPassword().equals(oldMD5Pwd)){
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.OLD_PASSWORD_ERROR.getMessage());
            redirectAttributes.addFlashAttribute("tab","changepwd");
            return "redirect:/user/profile";
        }

        String randomSalt = PasswordUtil.getRandomSalt();
        String result = PasswordUtil.getHashPassword(newPwd, randomSalt);
        user.setSalt(randomSalt);
        user.setPassword(result);
        userService.update(user);

        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
        redirectAttributes.addFlashAttribute("tab","changepwd");
        return "redirect:/user/profile";
    }
}
