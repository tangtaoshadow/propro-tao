package com.westlake.air.propro.config;


import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;


/***
 * @Author TangTao
 * @CreateTime 2019-7-22 00:30:50
 * @UpdateTime 2019-7-22 22:01:38
 * @Achieve 配置JWT 过滤规则  鉴权登录拦截器
 * @Copyright 西湖 PROPRO http://www.proteomics.pro/
 *
 */

public class JWTFilter extends BasicHttpAuthenticationFilter {


    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 如果带有 token，则对 token 进行检查，否则直接通过
     * >2 接着执行 isAccessAllowed
     */
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) throws UnauthorizedException {

        // System.out.println(">isAccessAllowed");

        // 判断请求的请求头是否存在 Token
        if (isLoginAttempt(request, response)) {
            //如果存在，则进入 executeLogin 方法执行登入，检查 token 是否正确
            // 不正常就会抛出异常
            try {
                // 执行登录
                executeLogin(request, response);
                return true;
            } catch (Exception e) {
                //token 错误
                responseError(response, e.getMessage());
                return false;
            }
        } else {
        }

        /***
         * Statement:
         * 当请求头中检测到不存在 token 这种情况不允许存在
         * 因为允许它不需要携带 token 那就应该在 shiro 里配置好
         * 而不是留到此处进行处理
         */
        return false;
    }

    /**
     * 判断用户是否想要登入。
     * 检测 header 里面是否包含 Token 字段
     */
    @Override
    protected boolean isLoginAttempt(ServletRequest request, ServletResponse response) {

        System.out.println(">执行 isLoginAttempt");

        HttpServletRequest req = (HttpServletRequest) request;

        // System.out.println("req.getHeader(Token)"+req.getHeader("Token"));

        String token = req.getHeader("Token");

        return token != null;

    }

    /**
     * 执行登陆操作
     */
    @Override
    protected boolean executeLogin(ServletRequest request, ServletResponse response) throws Exception {

        // System.out.println(">执行 executeLogin");

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String token = httpServletRequest.getHeader("Token");

        JWTToken jwtToken = new JWTToken(token);
        // getSubject(request, response).login(jwtToken);

        try {
            // 提交给realm进行登入，如果错误他会抛出异常并被捕获
            getSubject(request, response).login(jwtToken);
        } catch (Exception e) {
            return false;
        }


        // 如果没有抛出异常则代表登入成功，返回true
        return true;
    }

    /**
     * 对跨域提供支持
     * <p>
     * >1 请求最先从这开始执行
     */
    @Override
    protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {

        System.out.println(">jwtfilter 执行 preHandle");


        /*****************  下面注释的代码不要删掉  ****************************/
        // 因为判断过程已经最先在 AuthenticationInterceptor 验证过 没有验证第二次的必要
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        // 设置 header key-value
        // httpServletResponse.setHeader("Access-control-Allow-Origin", httpServletRequest.getHeader("Origin"));
        httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS,PUT,DELETE,aaa");
        httpServletResponse.setHeader("Allow", "GET,POST,OPTIONS,PUT,DELETE");
        // System.out.println(httpServletRequest.getHeader("Origin"));

        httpServletResponse.setHeader("Access-Control-Allow-Headers", httpServletRequest.getHeader("Access-Control-Request-Headers"));
        // System.out.println( httpServletRequest.getHeader("Access-Control-Request-Headers"));

        // 跨域时会首先发送一个option请求，这里我们给option请求直接返回正常状态
        if (httpServletRequest.getMethod().equals(RequestMethod.OPTIONS.name())) {
            httpServletResponse.setStatus(HttpStatus.OK.value());
            return false;
        }

        return super.preHandle(request, response);
    }

    /**
     * 将非法请求跳转到 /unauthorized/**
     */
    private void responseError(ServletResponse response, String message) {

        // System.out.println(">执行 responseError");

        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        try {
            //设置编码，否则中文字符在重定向时会变为空字符串
            message = URLEncoder.encode(message, "UTF-8");
            httpServletResponse.setHeader("Content-type", "text/html;charset=UTF-8");

        } catch (IOException e) {

            logger.error(e.getMessage());

        }

    }


}

