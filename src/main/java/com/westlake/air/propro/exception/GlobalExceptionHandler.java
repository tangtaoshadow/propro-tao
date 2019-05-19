package com.westlake.air.propro.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotLoginException.class)
    public String userNotLoginException(UserNotLoginException e, HttpServletResponse response) {
        return "redirect:/login/login";
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public String unauthorizedAccessException(UnauthorizedAccessException e, HttpServletResponse response) {
        return e.getRedirectPage();
    }
}
