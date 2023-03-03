package com.highestpeak.gist.example.base;

import java.nio.charset.StandardCharsets;

/**
 * @author zhangjike <zhangjike03@kuaishou.com>
 * Created on 2021-08-17
 */
public class BytesToString {
    public static void main(String[] args) {
        // byte array print to string
        byte[] bytes = new byte[] {
                -26,-75,-117,-24,-81,-107,49,10,-26,-75,-117,-24,-81,-107,50,10,-26,-75,-117,-24,-81,-107,51,10,-26,-75,-117,-24,-81,-107,52,10
        };
        System.out.println(new String(bytes, StandardCharsets.UTF_8));
    }
}
