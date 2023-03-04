package com.highestpeak.gist.common.help;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@ToString
@Slf4j
public class JacksonException extends RuntimeException {

    private final Object[] o;

    public JacksonException(Throwable throwable, Object... o) {
        super(throwable);
        this.o = o;
    }

    @Override
    public String getMessage() {
        return toString();
    }
}
