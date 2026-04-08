/*
 * Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.healthcare.apim.core.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;

import java.time.Instant;
import java.util.UUID;

/**
 * Utility class holding general utility functions
 */
public class CommonUtil {

    private static final Log LOG = LogFactory.getLog(CommonUtil.class);

    /**
     * Generate a UUID
     * @return
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Get the server startup time.
     *
     * @return server startup time in UTC format
     */
    public static long getServerStartupTime() {
        String serverStartTime = System.getProperty(CarbonConstants.START_TIME);
        if (serverStartTime != null) {
            return Long.parseLong(serverStartTime);
        }
        return System.currentTimeMillis();
    }

    /**
     * Convert a time string in instant format to a long millisecond value.
     *
     * @param time time string
     * @return long value of the time
     */
    public static long convertTimeToLong(String time) {
        try {
            return Instant.parse(time).toEpochMilli();
        } catch (NumberFormatException e) {
            LOG.error("Error while parsing time: \"" + time + "\". The provided time should be in the Instant format " +
                            "(e.g., yyyy-MM-dd'T'HH:mm:ss.SSS'Z').", e);
            // Return the server startup time if the provided time is not in the correct format
            return getServerStartupTime();
        }
    }
}
