/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org).
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

package org.wso2.healthcare.apim.backendauth.impl;

import org.apache.synapse.MessageContext;
import org.wso2.healthcare.apim.backendauth.Constants;
import org.wso2.healthcare.apim.core.config.BackendAuthConfig;

/**
 * Interface for the backend authentication handler implementations.
 */
public interface BackendAuthHandler {

    /**
     * Fetch a valid access token from the relevant Auth implementation.
     *
     * @param messageContext Message context.
     * @return Valid access token.
     */
    String fetchValidAccessToken(MessageContext messageContext, BackendAuthConfig backendAuthConfig);

    /**
     * Get the auth header scheme. i.e. Bearer, Basic etc.
     */
    default String getAuthHeaderScheme() {
        return Constants.HEADER_VALUE_BEARER;
    }
}
