package com.highestpeak.gist.common.help;

import com.google.common.base.Preconditions;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-05
 */
public class BytesHelper {

    private static final char[] HEX_CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String toHex(byte[] b) {
        return toHex(b, 0, b.length);
    }

    public static String toHex(byte[] b, int offset, int length) {
        Preconditions.checkArgument(length <= 1073741823);
        int numChars = length * 2;
        char[] ch = new char[numChars];

        for (int i = 0; i < numChars; i += 2) {
            byte d = b[offset + i / 2];
            ch[i] = HEX_CHARS[d >> 4 & 15];
            ch[i + 1] = HEX_CHARS[d & 15];
        }

        return new String(ch);
    }

    public static byte[] fromHex(String hex) {
        Preconditions.checkArgument(hex.length() % 2 == 0, "length must be a multiple of 2");
        int len = hex.length();
        byte[] b = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            b[i / 2] = hexCharsToByte(hex.charAt(i), hex.charAt(i + 1));
        }

        return b;
    }

    private static int hexCharToNibble(char ch) {
        if (ch <= '9' && ch >= '0') {
            return ch - 48;
        } else if (ch >= 'a' && ch <= 'f') {
            return ch - 97 + 10;
        } else if (ch >= 'A' && ch <= 'F') {
            return ch - 65 + 10;
        } else {
            throw new IllegalArgumentException("Invalid hex char: " + ch);
        }
    }

    private static byte hexCharsToByte(char c1, char c2) {
        return (byte) (hexCharToNibble(c1) << 4 | hexCharToNibble(c2));
    }

}