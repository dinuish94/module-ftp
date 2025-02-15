/*
 * Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.ei.ftp.util;

import org.ballerinalang.jvm.BallerinaValues;
import org.ballerinalang.jvm.types.BPackage;
import org.ballerinalang.jvm.types.BType;
import org.ballerinalang.jvm.values.ErrorValue;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utils class for FTP client operations.
 */
public class FTPUtil {

    private static final String FTP_ERROR_CODE = "{wso2/ftp}FTPError";

    private FTPUtil() {
        // private constructor
    }

    public static boolean notValidProtocol(String url) {

        return !url.startsWith("ftp") && !url.startsWith("sftp") && !url.startsWith("ftps");
    }

    public static String createUrl(ObjectValue clientConnector, String filePath) throws BallerinaFTPException {

        String username = (String) clientConnector.getNativeData(FTPConstants.ENDPOINT_CONFIG_USERNAME);
        String password = (String) clientConnector.getNativeData(FTPConstants.ENDPOINT_CONFIG_PASS_KEY);
        String host = (String) clientConnector.getNativeData(FTPConstants.ENDPOINT_CONFIG_HOST);
        int port = (int) clientConnector.getNativeData(FTPConstants.ENDPOINT_CONFIG_PORT);
        String protocol = (String) clientConnector.getNativeData(FTPConstants.ENDPOINT_CONFIG_PROTOCOL);

        return createUrl(protocol, host, port, username, password, filePath);
    }

    public static String createUrl(MapValue config, Logger logger) throws BallerinaFTPException {

        final String filePath = config.getStringValue(FTPConstants.ENDPOINT_CONFIG_PATH);
        String protocol = config.getStringValue(FTPConstants.ENDPOINT_CONFIG_PROTOCOL);
        final String host = config.getStringValue(FTPConstants.ENDPOINT_CONFIG_HOST);
        int port = extractPortValue(config, FTPConstants.ENDPOINT_CONFIG_PORT, logger);

        final MapValue secureSocket = config.getMapValue(FTPConstants.ENDPOINT_CONFIG_SECURE_SOCKET);
        String username = null;
        String password = null;
        if (secureSocket != null) {
            final MapValue basicAuth = secureSocket.getMapValue(FTPConstants.ENDPOINT_CONFIG_BASIC_AUTH);
            if (basicAuth != null) {
                username = basicAuth.getStringValue(FTPConstants.ENDPOINT_CONFIG_USERNAME);
                password = basicAuth.getStringValue(FTPConstants.ENDPOINT_CONFIG_PASS_KEY);
            }
        }
        return createUrl(protocol, host, port, username, password, filePath);
    }

    private static String createUrl(String protocol, String host, int port, String username, String password,
                                    String filePath) throws BallerinaFTPException {

        String userInfo = username + ":" + password;
        URI uri = null;
        try {
            uri = new URI(protocol, userInfo, host, port, filePath, null, null);
        } catch (URISyntaxException e) {
            throw new BallerinaFTPException("Error occurred while constructing a URI from host: " + host +
                    ", port: " + port + ", username: " + username + " and basePath: " + filePath + e.getMessage(), e);
        }
        return uri.toString();
    }

    public static Map<String, String> getAuthMap(MapValue config) {
        final MapValue secureSocket = config.getMapValue(FTPConstants.ENDPOINT_CONFIG_SECURE_SOCKET);
        String username = null;
        String password = null;
        if (secureSocket != null) {
            final MapValue basicAuth = secureSocket.getMapValue(FTPConstants.ENDPOINT_CONFIG_BASIC_AUTH);
            if (basicAuth != null) {
                username = basicAuth.getStringValue(FTPConstants.ENDPOINT_CONFIG_USERNAME);
                password = basicAuth.getStringValue(FTPConstants.ENDPOINT_CONFIG_PASS_KEY);
            }
        }
        Map<String, String> authMap = new HashMap<>();
        authMap.put(FTPConstants.ENDPOINT_CONFIG_USERNAME, username);
        authMap.put(FTPConstants.ENDPOINT_CONFIG_PASS_KEY, password);

        return authMap;
    }

    /**
     * Creates an error message.
     *
     * @param errMsg the cause for the error.
     * @return an error which will be propagated to ballerina user.
     */
    public static ErrorValue createError(String errMsg) {

        return new ErrorValue(FTP_ERROR_CODE, errMsg);
    }

    /**
     * Gets an int from the {@link MapValue} config.
     *
     * @param config the config
     * @param key    the key that has an integer value
     * @param logger the logger to log errors
     * @return the relevant int value from the config
     */
    public static int extractPortValue(MapValue config, String key, Logger logger) {

        return getIntFromLong(config.getIntValue(key), key, logger);
    }

    /**
     * Gets an integer from a long value. Handles errors appropriately.
     *
     * @param longVal the long value.
     * @param name    the name of the long value: useful for logging the error.
     * @param logger  the logger to log errors
     * @return the int value from the given long value
     */
    private static int getIntFromLong(long longVal, String name, Logger logger) {

        if (longVal <= 0) {
            return -1;
        }
        try {
            return Math.toIntExact(longVal);
        } catch (ArithmeticException e) {
            logger.warn("The value set for {} needs to be less than {}. The {} value is set to {}", name,
                    Integer.MAX_VALUE, name, Integer.MAX_VALUE);
            return Integer.MAX_VALUE;
        }
    }

    public static BType getFileInfoType() {
        MapValue<String, Object> fileInfoStruct = BallerinaValues.createRecordValue(
                new BPackage(FTPConstants.FTP_ORG_NAME, FTPConstants.FTP_MODULE_NAME, FTPConstants.FTP_MODULE_VERSION),
                FTPConstants.FTP_FILE_INFO);
        return fileInfoStruct.getType();
    }
}
