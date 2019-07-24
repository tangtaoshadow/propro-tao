package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSON;
import com.westlake.air.propro.config.VMProperties;
import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.message.ApplyMessage;
import com.westlake.air.propro.domain.bean.message.DingtalkMessage;
import com.westlake.air.propro.domain.db.UserDO;
import com.westlake.air.propro.service.UserService;
import com.westlake.air.propro.service.impl.HttpClient;
import com.westlake.air.propro.utils.JWTUtil;
import com.westlake.air.propro.utils.PasswordUtil;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("login")
public class LoginController extends BaseController {

    private static Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    UserService userService;

    @Autowired
    VMProperties vmProperties;

    @Autowired
    HttpClient httpClient;

    // @RequestMapping 自动扫描形参的POJO，并创建对象，
    // 如果post传进来的参数与POJO成员变量名相同，会通过POJO的setter方法传给该对象。
    // 通过post方式传入用户名和密码
    // @RequestMapping(value = "/dologin" ,method = RequestMethod.POST)
    @RequestMapping(value = "/dologin" )
    @ResponseBody
    public ResultDO login(UserDO user, boolean remember) {
        /**
         * post 格式
         * username: 1111111111
         * password: 11111111111111
         * remember: on
         * 当调用subject.login(token)的时候，
         * 首先这次身份认证会委托给Security Manager，
         * 而Security Manager又会委托给Authenticator，
         * 接着Authenticator会把传过来的token再交给我们自己注入的
         * Realm进行数据匹配从而完成整个认证
         */

        System.out.println("***** 登录 *****");
        System.out.println(">user "+user);

        // 创建一个标记登录结果的 class
        ResultDO result = new ResultDO();

        String username = user.getUsername();

        // 获取 subject  根据token 判断要不要 新建 Subject
        Subject currentUser = SecurityUtils.getSubject();

        if (!currentUser.isAuthenticated()) {

            UsernamePasswordToken token = new UsernamePasswordToken(username, user.getPassword());
            try {
                // remember=on 转换为 true
                if (remember) {
                    token.setRememberMe(true);
                }
                // 没有异常 ==> 登录成功
                // 把登录过程交给 ShiroRealm class
                currentUser.login(token);
                // 成功
                result.setSuccess(true);
            } catch (UnknownAccountException uae) {
                // 捕获 UnknownAccount
                result.setErrorResult(ResultCode.USER_NOT_EXISTED);
            } catch (IncorrectCredentialsException ice) {
                // 捕获 IncorrectCredentials 密码错误
                result.setErrorResult(ResultCode.USERNAME_OR_PASSWORD_ERROR);
            } catch (LockedAccountException lae) {
                // 捕获 LockedAccount 账号锁定
                result.setErrorResult(ResultCode.ACCOUNT_IS_LOCKED);
            } catch (ExcessiveAttemptsException eae) {
                // 捕获 ExcessiveAttempts 登录失败多次
                result.setErrorResult(ResultCode.TRY_TOO_MANY_TIMES);
            } catch (AuthenticationException ae) {
                // 捕获 Authentication 其他错误
                //通过处理Shiro的运行时AuthenticationException就可以控制用户登录失败或密码错误时的情景
                logger.info("对用户[" + username + "]进行登录验证..验证未通过,堆栈轨迹如下");
                ae.printStackTrace();
                result.setErrorResult(ResultCode.USERNAME_OR_PASSWORD_ERROR);
            }
        }else{
            result.setSuccess(true);
        }

        return result;
    }



    @RequestMapping(value = "/login")
    public String login(HttpServletRequest request,
                        HttpServletResponse response,
            @RequestParam(value = "lang", required = false) String lang) {

        LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
        if(localeResolver != null){
            if ("zh".equals(lang)) {
                localeResolver.setLocale(request, response, new Locale("zh", "CN"));
            } else if ("en".equals(lang)) {
                localeResolver.setLocale(request, response, new Locale("en", "US"));
            }
        }

        try {
            if ((null != SecurityUtils.getSubject() && SecurityUtils.getSubject().isAuthenticated()) || (SecurityUtils.getSubject() != null && SecurityUtils.getSubject().isRemembered())) {
                return "redirect:/";
            } else {
                return "login";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "login";

    }

    @RequestMapping("apply")
    @ResponseBody
    public ResultDO apply(Model model,
                          @RequestParam(value = "username", required = true) String username,
                          @RequestParam(value = "email", required = true) String email,
                          @RequestParam(value = "telephone", required = true) String telephone,
                          @RequestParam(value = "dingtalkId", required = false) String dingtalkId,
                          @RequestParam(value = "organization", required = false) String organization) {
        ResultDO resultDO = new ResultDO(true);
        String dingtalkRobot = vmProperties.getDingtalkRobot();
        ApplyMessage am = new ApplyMessage();
        am.setUsername(username);
        am.setEmail(email);
        am.setTelephone(telephone);
        am.setOrganization(organization);
        am.setDingtalkId(dingtalkId);
        DingtalkMessage message = new DingtalkMessage("账号申请", am.markdown());
        String response = httpClient.client(dingtalkRobot, JSON.toJSONString(message));
        logger.info(response);
        return resultDO;
    }


    // 登出
    @RequestMapping(value = "/logout")
    public String logout(HttpServletRequest request) {
        Subject subject = SecurityUtils.getSubject();

        if (subject.isAuthenticated()) {
            subject.logout();
        }

        return "redirect:/login/login";
    }

    @RequestMapping("/test")
    @ResponseBody
    public String test(UserDO user){

        int res=CheckLogin(user.getUsername(),user.getPassword());

        Map<String,Object> map = new HashMap<String,Object>();
        map.put("status",res);
        if(0==res){
            // 登录成功
            map.put("token",JWTUtil.createToken(user.getUsername()));
        }
        // 返回json
        JSONObject json = new JSONObject(map);
        System.out.println(json.toString());

        return json.toString();
    }

    @PostMapping("test1")
    @ResponseBody
    // @RequiresRoles("admin")
    public String test1(){
        return "test111111111111";
    }



    protected int CheckLogin(String username,String password){
        // 判断 username password 是否为空
        if(null==username||null==password){
            return -1;
        }

        // 1 从数据库中根据 username 获取存在的用户
        UserDO userInfo = userService.getByUsername(username);
        System.out.println(userInfo);
        if(null==userInfo){
            // 用户不存在
            return -2;
        }

        // 2 获取 salt
        String salt=userInfo.getSalt();
        // 3 生成 password
        String hashPassword = PasswordUtil.getHashPassword(password, salt);
        // 4 验证 password
        if(hashPassword.equals(userInfo.getPassword())){
            // 登录成功
            System.out.println("登录成功");
            return 0;
        }else{
            // 密码错误
            return -3;
        }

    }


}
