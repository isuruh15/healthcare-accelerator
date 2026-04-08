/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org).
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

package org.wso2.healthcare.apim.core.config;

import java.util.Arrays;
import java.util.List;

/**
 * SMART on FHIR related configuration
 */
public class SmartConfig {

    // Auth methods
    private List<String> authMethods = Arrays.asList("client_secret_basic", "client_secret_post", "private_key_jwt");

    // Grant types
    private List<String> grantTypesSupported = Arrays.asList("authorization_code", "refresh_token", "client_credentials");
    // Scopes supported
    private List<String> scopesSupported = Arrays.asList("openid", "launch/patient", "patient/*.cruds", "user/*.cruds");

    // Response types
    private List<String> responseTypes = Arrays.asList("code", "id_token", "token", "device", "id_token token");

    // Capabilities
    private List<String> capabilities = Arrays.asList(
            "launch-ehr",
            "launch-standalone",
            "client-public",
            "client-confidential-symmetric",
            "sso-openid-connect",
            "context-ehr-patient",
            "context-ehr-encounter",
            "context-standalone-patient",
            "context-standalone-encounter",
            "permission-patient",
            "permission-user",
            "permission-offline"
    );

    public SmartConfig() {
    }

    public List<String> getAuthMethods() {
        return authMethods;
    }

    public void setAuthMethods(List<String> authMethods) {
        this.authMethods = authMethods;
    }

    public List<String> getGrantTypesSupported() {
        return grantTypesSupported;
    }

    public void setGrantTypesSupported(List<String> grantTypesSupported) {
        this.grantTypesSupported = grantTypesSupported;
    }

    public List<String> getScopesSupported() {
        return scopesSupported;
    }

    public void setScopesSupported(List<String> scopesSupported) {
        this.scopesSupported = scopesSupported;
    }

    public List<String> getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(List<String> responseTypes) {
        this.responseTypes = responseTypes;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }
}
