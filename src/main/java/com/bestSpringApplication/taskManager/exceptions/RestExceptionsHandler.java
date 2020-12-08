package com.bestSpringApplication.taskManager.exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionsHandler{

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static ErrorMessage notFoundHandle(ContentNotFoundException ex){
        return new ErrorMessage(HttpStatus.NOT_FOUND.value(),ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static ErrorMessage illegalFileHandle(IllegalFileFormatException ex){
        return new ErrorMessage(HttpStatus.BAD_REQUEST.value(),ex.getMessage());
    }
}
