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

package org.wso2.carbon.usage.data.collector.identity.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This class facilitates internal call.
 */
public class AppCredentialsUtil {

    private static final Log LOG = LogFactory.getLog(AppCredentialsUtil.class);
    private static final AppCredentialsUtil instance = new AppCredentialsUtil();

    private String appName;
    private char[] appPassword;

    // Configuration constants
    private static final String SERVICE_CONFIG_RELATIVE_PATH =
            "./repository/conf/identity/RecoveryEndpointConfig.properties";
    private static final String SERVICE_CONFIG_FILE_NAME = "RecoveryEndpointConfig.properties";
    private static final String APP_NAME = "app.name";
    private static final String APP_PASSWORD = "app.password";
    private static final String DEFAULT_CALLBACK_HANDLER =
            "org.wso2.carbon.securevault.DefaultSecretCallbackHandler";
    private static final String SECRET_PROVIDER = "secretProvider";

    private AppCredentialsUtil() {

        init();
    }

    /**
     * Returns the singleton instance of AppCredentialsUtil
     *
     * @return an instance of AppCredentialsUtil
     */
    public static AppCredentialsUtil getInstance() {

        return instance;
    }

    /**
     * Initializes and loads app credentials from configuration file
     */
    public void init() {

        InputStream inputStream = null;

        try {
            Properties properties = new Properties();
            File currentDirectory = new File(new File(".").getAbsolutePath());
            String configFilePath = currentDirectory.getCanonicalPath() + File.separator +
                    SERVICE_CONFIG_RELATIVE_PATH;
            File configFile = new File(configFilePath);

            if (configFile.exists()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(SERVICE_CONFIG_FILE_NAME + " file loaded from " +
                            SERVICE_CONFIG_RELATIVE_PATH);
                }
                inputStream = new FileInputStream(configFile);
                properties.load(inputStream);
                resolveSecrets(properties);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Could not find " + SERVICE_CONFIG_FILE_NAME + " in filesystem");
                }
                return;
            }

            // Extract app credentials
            appName = properties.getProperty(APP_NAME);
            String appPasswordStr = properties.getProperty(APP_PASSWORD);

            if (appPasswordStr != null) {
                appPassword = appPasswordStr.toCharArray();
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("App credentials loaded - App Name: " + appName +
                        ", Password configured: " + (appPassword != null && appPassword.length > 0));
            }

        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to load app credentials from configuration file", e);
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Failed to close the FileInputStream for file: " +
                                SERVICE_CONFIG_FILE_NAME, e);
                    }
                }
            }
        }
    }

    /**
     * Resolves secrets from secure vault if configured
     *
     * @param properties Properties to resolve
     */
    private void resolveSecrets(Properties properties) {

        String secretProvider = (String) properties.get(SECRET_PROVIDER);
        if (StringUtils.isBlank(secretProvider)) {
            properties.put(SECRET_PROVIDER, DEFAULT_CALLBACK_HANDLER);
        }

        try {
            SecretResolver secretResolver = SecretResolverFactory.create(properties);
            if (secretResolver != null && secretResolver.isInitialized()) {
                for (String key : properties.stringPropertyNames()) {
                    if (secretResolver.isTokenProtected(key)) {
                        String resolvedValue = secretResolver.resolve(key);
                        if (resolvedValue != null) {
                            properties.setProperty(key, resolvedValue);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error resolving secrets from secure vault: " + e.getMessage());
            }
        }
    }

    /**
     * Returns the app name
     *
     * @return app name
     */
    public String getAppName() {

        return appName;
    }

    /**
     * Returns the app password
     *
     * @return app password
     */
    public char[] getAppPassword() {

        return appPassword;
    }

    /**
     * Checks if credentials are configured
     *
     * @return true if both app name and password are configured
     */
    public boolean hasCredentials() {

        return appName != null && !appName.isEmpty() &&
                appPassword != null && appPassword.length > 0;
    }
}
