package com.highestpeak.gist.common.help;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-05
 */
public class JedisDataException extends RuntimeException {
    private static final long serialVersionUID = 3878126572474819403L;

    public JedisDataException(String message) {
        super(message);
    }

    public JedisDataException(Throwable cause) {
        super(cause);
    }

    public JedisDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
