package com.highestpeak.gist.common;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-04
 */
public class HashUtil {

    /**
     * 生成字符串的哈希值
     */
    public static long hashStrToLong(String str) {
        // WARNING: https://stackoverflow.com/questions/2624192/good-hash-function-for-strings
        return str.hashCode();
    }

}
