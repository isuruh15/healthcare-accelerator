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

package org.wso2.healthcare.apim.core.config;

/**
 * FHIR related configuration
 */
public class FHIRServerConfig {

    private String serverName = ConfigConstants.DEFAULT_FHIR_SERVER_NAME;
    private String serverVersion = ConfigConstants.DEFAULT_FHIR_SERVER_VERSION;
    private long capabilityStatementPublishedTime;

    public FHIRServerConfig() {
    }

    public String getServerName() {

        return serverName;
    }

    public void setServerName(String serverName) {

        this.serverName = serverName;
    }

    public String getServerVersion() {

        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {

        this.serverVersion = serverVersion;
    }

    public long getCapabilityStatementPublishedTime() {

        return capabilityStatementPublishedTime;
    }

    public void setCapabilityStatementPublishedTime(long capabilityStatementPublishedTime) {

        this.capabilityStatementPublishedTime = capabilityStatementPublishedTime;
    }
}
