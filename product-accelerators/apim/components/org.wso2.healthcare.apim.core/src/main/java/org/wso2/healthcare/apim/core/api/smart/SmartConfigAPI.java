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

package org.wso2.healthcare.apim.core.api.smart;

import org.wso2.healthcare.apim.core.OpenHealthcareEnvironment;
import org.wso2.healthcare.apim.core.OpenHealthcareException;

import java.util.List;

/**
 * Internal Java API to retrieve SMART on FHIR configurations
 */
public class SmartConfigAPI {

    /**
     * Get authentication methods configuration
     *
     * @return list of auth methods
     * @throws OpenHealthcareException
     */
    public static List<String> getAuthMethods() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getSmartConfig().getAuthMethods();
    }

    /**
     * Get supported grant types configuration
     *
     * @return list of supported grant types
     * @throws OpenHealthcareException
     */
    public static List<String> getGrantTypesSupported() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getSmartConfig().getGrantTypesSupported();
    }

    /**
     * Get supported scopes configuration
     *
     * @return list of supported scopes
     * @throws OpenHealthcareException
     */
    public static List<String> getScopesSupported() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getSmartConfig().getScopesSupported();
    }

    /**
     * Get supported response types configuration
     *
     * @return list of response types
     * @throws OpenHealthcareException
     */
    public static List<String> getResponseTypes() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getSmartConfig().getResponseTypes();
    }

    /**
     * Get SMART capabilities configuration
     *
     * @return list of capabilities
     * @throws OpenHealthcareException
     */
    public static List<String> getCapabilities() throws OpenHealthcareException {
        return OpenHealthcareEnvironment.getInstance().getConfig().getSmartConfig().getCapabilities();
    }
}
