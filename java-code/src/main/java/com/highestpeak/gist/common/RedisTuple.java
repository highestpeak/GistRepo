package com.highestpeak.gist.common;

import java.nio.charset.Charset;
import java.util.Arrays;

import lombok.Data;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-03
 */
@Data
public class RedisTuple implements Comparable<RedisTuple> {

    private byte[] element;
    private Double score;

    public String getElement() {
        return new String(element, Charset.defaultCharset());
    }

    @Override
    public int compareTo(RedisTuple other) {
        if (this.score == other.getScore() || Arrays.equals(this.element, other.element)) return 0;
        else return this.score < other.getScore() ? -1 : 1;
    }

}
