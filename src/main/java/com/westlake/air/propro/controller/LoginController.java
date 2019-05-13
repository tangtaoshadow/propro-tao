package com.westlake.air.propro.controller;

import com.westlake.air.propro.domain.db.UserDO;
import com.westlake.air.propro.service.UserService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;

@Controller
@RequestMapping("login")
public class LoginController extends BaseController {

    private static Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    UserService userService;

    @RequestMapping(value = "/dologin")
    public ModelAndView login(UserDO user, RedirectAttributes redirectAttributes, boolean remember) {
        ModelAndView view = new ModelAndView();
        String username = user.getUsername();
        Subject currentUser = SecurityUtils.getSubject();
        if (!currentUser.isAuthenticated()) {
            UsernamePasswordToken token = new UsernamePasswordToken(username, user.getPassword());
            try {
                if (remember) {
                    token.setRememberMe(true);
                }
                currentUser.login(token);
                view.setViewName("redirect:/");
            } catch (UnknownAccountException uae) {
                logger.info("对用户[" + username + "]进行登录验证..验证未通过,未知账户");
                redirectAttributes.addFlashAttribute("message", "unknown Account");
            } catch (IncorrectCredentialsException ice) {
                logger.info("对用户[" + username + "]进行登录验证..验证未通过,错误的凭证");
                redirectAttributes.addFlashAttribute("message", "username or password error");
            } catch (LockedAccountException lae) {
                logger.info("对用户[" + username + "]进行登录验证..验证未通过,账户已锁定");
                redirectAttributes.addFlashAttribute("message", "account is locked");
            } catch (ExcessiveAttemptsException eae) {
                logger.info("对用户[" + username + "]进行登录验证..验证未通过,错误次数过多");
                redirectAttributes.addFlashAttribute("message", "too many errors");
            } catch (AuthenticationException ae) {
                //通过处理Shiro的运行时AuthenticationException就可以控制用户登录失败或密码错误时的情景
                logger.info("对用户[" + username + "]进行登录验证..验证未通过,堆栈轨迹如下");
                ae.printStackTrace();
                redirectAttributes.addFlashAttribute("message", "username or password error");
            }
        }

        view.setViewName("redirect:/login/login");
        return view;
    }

    @RequestMapping(value = "/login")
    public String login(HttpServletRequest request) {

        try {
            if ((null != SecurityUtils.getSubject() && SecurityUtils.getSubject().isAuthenticated()) || (SecurityUtils.getSubject() != null && SecurityUtils.getSubject().isRemembered())) {
                return "redirect:/";
            } else {
                logger.info("--进行登录验证..验证开始");
                return "login";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "login";
    }

    @RequestMapping(value = "/logout")
    public String logout(HttpServletRequest request) {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated()) {
            subject.logout();
        }

        return "login/login";
    }

    @RequiresRoles({"admin"})
    @RequestMapping("xiao")
    public ModelAndView xiao() {
        ModelAndView view = new ModelAndView("index");
        System.out.println("来到了xiao方法。。。");

        return view;
    }

}
