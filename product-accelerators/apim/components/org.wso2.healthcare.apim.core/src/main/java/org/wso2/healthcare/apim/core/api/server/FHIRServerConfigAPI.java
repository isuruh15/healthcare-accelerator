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

package org.wso2.healthcare.apim.core.api.server;

import org.wso2.healthcare.apim.core.OpenHealthcareEnvironment;
import org.wso2.healthcare.apim.core.OpenHealthcareException;

/**
 * Internal Java API to retrieve FHIR server configurations of this server
 */
public class FHIRServerConfigAPI {

    /**
     * Function to retrieve server name of this Server
     *
     * @return
     * @throws OpenHealthcareException
     */
    public static String getFHIRServerName() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getFHIRServerConfig().getServerName();
    }

    /**
     * Function to retrieve server version of this Server
     * @return
     * @throws OpenHealthcareException
     */
    public static String getFHIRServerVersion() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getFHIRServerConfig().getServerVersion();
    }

    /**
     * Function to retrieve capability statement published time of this Server
     *
     * @return capability statement published time
     * @throws OpenHealthcareException
     */
    public static long getCapabilityStatementPublishedTime() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getFHIRServerConfig().getCapabilityStatementPublishedTime();
    }

}
