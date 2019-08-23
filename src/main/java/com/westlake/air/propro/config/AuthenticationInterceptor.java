package com.westlake.air.propro.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// 鉴权
public class AuthenticationInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // System.out.println(">AuthenticationInterceptor 执行 preHandle");
        //
        // HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        // HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        // // 设置 header key-value
        // // httpServletResponse.setHeader("Access-control-Allow-Origin", httpServletRequest.getHeader("Origin"));
        // httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS,PUT,DELETE,aaa");
        // httpServletResponse.setHeader("Allow", "GET,POST,OPTIONS,PUT,DELETE");
        // // System.out.println(httpServletRequest.getHeader("Origin"));
        //
        // httpServletResponse.setHeader("Access-Control-Allow-Headers", httpServletRequest.getHeader("Access-Control-Request-Headers"));
        // // System.out.println( httpServletRequest.getHeader("Access-Control-Request-Headers"));
        //
        // // 跨域时会首先发送一个option请求，这里我们给option请求直接返回正常状态
        // if (httpServletRequest.getMethod().equals(RequestMethod.OPTIONS.name())) {
        //     httpServletResponse.setStatus(HttpStatus.OK.value());
        //     return false;
        // }

        return true;
    }
}
