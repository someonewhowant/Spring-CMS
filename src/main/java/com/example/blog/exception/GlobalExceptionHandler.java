package com.example.blog.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.error(
                "Resource not found exception occurred: {} at URI {}",
                ex.getMessage(),
                request.getRequestURI());

        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("status", 404);
        modelAndView.addObject("errorTitle", "Resource Not Found");
        modelAndView.addObject("message", ex.getMessage());
        modelAndView.addObject("path", request.getRequestURI());
        return modelAndView;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.error(
                "Illegal argument exception occurred: {} at URI {}",
                ex.getMessage(),
                request.getRequestURI());

        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("status", 400);
        modelAndView.addObject("errorTitle", "Bad Request");
        modelAndView.addObject("message", ex.getMessage());
        modelAndView.addObject("path", request.getRequestURI());
        return modelAndView;
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex,
            HttpServletRequest request) {
        log.error("Access denied exception occurred at URI {}", request.getRequestURI());

        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("status", 403);
        modelAndView.addObject("errorTitle", "Access Denied");
        modelAndView.addObject("message", "You do not have permission to access this resource.");
        modelAndView.addObject("path", request.getRequestURI());
        return modelAndView;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleAllExceptions(Exception ex, HttpServletRequest request) {
        log.error("An unexpected error occurred at URI {}", request.getRequestURI(), ex);

        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("status", 500);
        modelAndView.addObject("errorTitle", "Internal Server Error");
        modelAndView.addObject(
                "message", "An unexpected error occurred on our servers. Please try again later.");
        modelAndView.addObject("path", request.getRequestURI());
        modelAndView.addObject("trace", ex.getClass().getName() + ": " + ex.getMessage());
        return modelAndView;
    }
}
