package com.highestpeak.gist.common.help;

import static java.net.InetAddress.getByName;
import static java.net.InetAddress.getLocalHost;

import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author highestpeak <highestpeak@163.com>
 * Created on 2023-03-04
 */
public class HostInfo {

    private static final String IP;
    private static final String HOST_NAME;

    static {
        try {
            InetAddress host = getLocalHost();
            HOST_NAME = host != null ? host.getHostName() : null;
            IP = getByName(HOST_NAME).getHostAddress();
        } catch (UnknownHostException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    /**
     * @return the host ip
     */
    public static String getHostIp() {
        return IP;
    }

    /**
     * @return the hostname
     */
    public static String getHostName() {
        return HOST_NAME;
    }

}
