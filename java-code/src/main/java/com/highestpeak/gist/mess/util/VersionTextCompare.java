package com.highestpeak.gist.mess.util;

import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2022-09-21
 */
public class VersionTextCompare {
    /**
     * https://stackoverflow.com/questions/198431/how-do-you-compare-two-version-strings-in-java
     */
    public static boolean versionLessThan(String versionText, String thresholdVersion) {
        if (StringUtils.isBlank(versionText) || StringUtils.isBlank(thresholdVersion)) {
            return false;
        }
        StringTokenizer versionTokens = new StringTokenizer(versionText, ".");
        StringTokenizer thresholdTokens = new StringTokenizer(thresholdVersion, ".");

        int length = Math.max(versionTokens.countTokens(), thresholdTokens.countTokens());
        for (int i = 0; i < length; i++) {
            int versionPart = versionTokens.hasMoreTokens() ? Integer.parseInt(versionTokens.nextToken()) : 0;
            int thresholdPart = thresholdTokens.hasMoreTokens() ? Integer.parseInt(thresholdTokens.nextToken()) : 0;
            if (versionPart < thresholdPart) {
                return true;
            }
            if (versionPart > thresholdPart) {
                return false;
            }
        }
        return false;
    }
}
