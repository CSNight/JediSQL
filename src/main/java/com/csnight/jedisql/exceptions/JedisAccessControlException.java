package com.csnight.jedisql.exceptions;

public class JedisAccessControlException extends JedisDataException {

    public JedisAccessControlException(String message) {
        super(message);
    }

    public JedisAccessControlException(Throwable cause) {
        super(cause);
    }

    public JedisAccessControlException(String message, Throwable cause) {
        super(message, cause);
    }
}