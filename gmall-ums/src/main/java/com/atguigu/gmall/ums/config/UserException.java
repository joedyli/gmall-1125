package com.atguigu.gmall.ums.config;

public class UserException extends RuntimeException {

    public UserException(String message) {
        super(message);
    }

    public UserException() {
        super();
    }
}
