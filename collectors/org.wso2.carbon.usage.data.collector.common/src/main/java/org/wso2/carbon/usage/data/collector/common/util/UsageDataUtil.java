/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.usage.data.collector.common.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

/**
 * Utility class for usage data collection operations.
 * Provides methods for node IP retrieval and hashing.
 */
public class UsageDataUtil {

    private static final Log log = LogFactory.getLog(UsageDataUtil.class);
    private static String cachedNodeIp = null;

    /**
     * Gets the IP address of the current node.
     * The IP address is cached after first retrieval for performance.
     *
     * @return The IP address of the node
     */
    public static synchronized String getNodeIpAddress() {
        if (cachedNodeIp != null) {
            return cachedNodeIp;
        }

        cachedNodeIp = retrieveNodeIpAddress();

        return cachedNodeIp;
    }

    /**
     * Retrieves the IP address of the current node.
     * Attempts to get a non-loopback, site-local (private) address first,
     * then falls back to any non-loopback address, and finally to localhost.
     *
     * @return The IP address as a string
     */
    private static String retrieveNodeIpAddress() {
        try {
            InetAddress candidateAddress = null;

            // Iterate through all network interfaces
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // Skip loopback and inactive interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();

                    // Skip loopback addresses
                    if (inetAddress.isLoopbackAddress()) {
                        continue;
                    }

                    // Prefer site-local (private) IPv4 addresses
                    if (inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }

                    // Keep track of non-loopback address as candidate
                    if (candidateAddress == null) {
                        candidateAddress = inetAddress;
                    }
                }
            }

            // Return candidate address if found
            if (candidateAddress != null) {
                return candidateAddress.getHostAddress();
            }

            // Fallback to InetAddress.getLocalHost()
            return InetAddress.getLocalHost().getHostAddress();

        } catch (Exception e) {
            if(log.isDebugEnabled()) {
                log.error("Failed to determine node IP address, using localhost", e);
            }
            return "127.0.0.1";
        }
    }

    /**
     * Generates SHA-256 hash of the given input string.
     *
     * @param input The input string to hash
     * @return The hash value in hexadecimal format
     */
    public static String generateSHA256Hash(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert bytes to hexadecimal format
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            if(log.isDebugEnabled()) {
                log.error("SHA-256 algorithm not available", e);
            }
            // Fallback to simple hashCode if SHA-256 is not available
            return String.valueOf(input.hashCode());
        }
    }

    /**
     * Clears the cached node IP (mainly for testing).
     */
    public static synchronized void clearCache() {
        cachedNodeIp = null;
    }
}

