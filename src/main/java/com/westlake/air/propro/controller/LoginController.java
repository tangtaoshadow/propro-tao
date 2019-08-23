package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSON;
import com.westlake.air.propro.config.VMProperties;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.message.ApplyMessage;
import com.westlake.air.propro.domain.bean.message.DingtalkMessage;
import com.westlake.air.propro.domain.db.UserDO;
import com.westlake.air.propro.service.UserService;
import com.westlake.air.propro.service.impl.HttpClient;
import com.westlake.air.propro.utils.JWTUtil;
import com.westlake.air.propro.utils.PasswordUtil;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/***
 * @UpdateTime 2019-8-7 01:44:24
 * @Statement 这里的请求可以不用携带 token
 * RequestMapping       这里避免与前端的 login 共用 (路由转发混淆)，所以设置成 login_propro
 */
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


    @RequestMapping(value = "/login11")
    public String login(HttpServletRequest request,
                        HttpServletResponse response,
                        @RequestParam(value = "lang", required = false) String lang) {

        LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
        if (localeResolver != null) {
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
        System.out.println(JSON.toJSONString(message));
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

    // @RequestMapping 自动扫描形参的POJO，并创建对象，
    // 如果post传进来的参数与POJO成员变量名相同，会通过POJO的setter方法传给该对象。
    // 通过post方式传入用户名和密码
    @RequestMapping("/login")
    @ResponseBody
    public String test(UserDO user) {

        Map<String, Object> map = new HashMap<String, Object>();
        int res = checkLogin(user.getUsername(), user.getPassword(), map);
        map.put("status", res);

        System.out.println("getsubject====" + SecurityUtils.getSubject() + "--" + SecurityUtils.getSubject().getPrincipal());
        System.out.println(map);
        if (0 == res) {
            // 登录成功
            map.put("token", JWTUtil.createToken(user.getUsername()));
            // 添加用户需要的信息
        }
        // 返回json
        JSONObject json = new JSONObject(map);
        System.out.println(json.toString());
        return json.toString();
    }

    @PostMapping("test11")
    @ResponseBody
    // @RequiresRoles("admin")

    public String test1() {
        System.out.println("test1");
        return "{\n" +
                "\"employees\": [\n" +
                "{ \"firstName\":\"B11111ill\" , \"lastName\":\"Gates\" },\n" +
                "{ \"firstName\":\"George\" , \"lastName\":\"Bush\" },\n" +
                "{ \"firstName\":\"Thomas\" , \"lastName\":\"Carter\" }\n" +
                "]\n" +
                "}";
    }


    /***
     * @Author TangTao
     * @CreateTime 2019-8-3 13:52:23
     * @Statement 为什么传入 map ？ 首先这个函数包装了所有处理登录过程
     *                  返回一个 int 供后端处理 为什么不直接把 int 写入 map 这时为了后续开发 可能会有更复杂的操作 而这里只负责验证登录
     *                  登录成功就把用户信息写入map 这样只需一次查询操作
     * @Archive 实现对账号密码进行验证 成功之后同时返回了用户信息
     * @param           username
     * @param           password
     * @param           map         如果用户登录成功 将用户信息写入 map 供前端显示
     * @return 成功 0
     */
    protected int checkLogin(String username, String password, Map<String, Object> map) {
        // 判断 username password 是否为空
        if (null == username || null == password) {
            return -1;
        }


        // 1 从数据库中根据 username 获取存在的用户
        UserDO userInfo = userService.getByUsername(username);
        System.out.println(userInfo);
        if (null == userInfo) {
            // 用户不存在
            return -2;
        }

        // 2 获取 salt
        String salt = userInfo.getSalt();
        // 3 生成 password
        String hashPassword = PasswordUtil.getHashPassword(password, salt);
        // 4 验证 password
        if (hashPassword.equals(userInfo.getPassword())) {
            // 登录成功
            System.out.println("登录成功");
            // 写入 map 一起发送给前端
            map.put("username", username);
            map.put("email", userInfo.getEmail());
            map.put("nick", userInfo.getNick());
            map.put("organization", userInfo.getOrganization());
            map.put("telephone", userInfo.getTelephone());
            // 这个角色 前端拿到用来供给不同用户显示不同界面 但是不显示出来 一是没有必要 二是泄露了隐私
            map.put("roles", userInfo.getRoles());
            return 0;
        } else {
            // 密码错误
            return -3;
        }

    }


}
