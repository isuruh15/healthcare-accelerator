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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.synapse.MessageContext;
import org.wso2.healthcare.apim.backendauth.Constants;
import org.wso2.healthcare.apim.backendauth.Utils;
import org.wso2.healthcare.apim.backendauth.tokenmgt.Token;
import org.wso2.healthcare.apim.backendauth.tokenmgt.TokenManager;
import org.wso2.healthcare.apim.core.OpenHealthcareException;
import org.wso2.healthcare.apim.core.OpenHealthcareRuntimeException;
import org.wso2.healthcare.apim.core.config.BackendAuthConfig;

import java.util.ArrayList;

/**
 * Mediator to authenticate with the backend using client credentials.
 */
public class ClientCredentialsBackendAuthenticator implements BackendAuthHandler {

    private static final Log log = LogFactory.getLog(ClientCredentialsBackendAuthenticator.class);

    @Override
    public String fetchValidAccessToken(MessageContext messageContext, BackendAuthConfig backendAuthConfig) {
        log.info("PrivateKeyJWTBackendAuthenticator mediator is started.");

        String accessToken;
        String tokenEndpoint = backendAuthConfig.getAuthEndpoint();
        String clientId = backendAuthConfig.getClientId();

        if (log.isDebugEnabled()) {
            log.debug("Configured client ID: " + clientId);
        }

        Token token = TokenManager.getToken(clientId, tokenEndpoint);
        if (token == null || !token.isActive()) {
            if (log.isDebugEnabled()) {
                log.debug("Access token not available in TokenManager.");
            }
            try {
                token = getAndAddNewToken(messageContext, backendAuthConfig);
            } catch (OpenHealthcareException e) {
                log.error("Error occurred while retrieving access token.", e);
                throw new OpenHealthcareRuntimeException(e);
            }
        }
        accessToken = token.getAccessToken();
        return accessToken;
    }

    /**
     * Get and add a new token to the token store.
     *
     * @param messageContext Message context.
     * @param config         Backend auth configuration.
     * @return Token.
     * @throws OpenHealthcareException If an error occurs while fetching the token.
     */
    private synchronized Token getAndAddNewToken(MessageContext messageContext,
                                                 BackendAuthConfig config) throws OpenHealthcareException {

        String clientId = config.getClientId();
        String tokenEndpoint = config.getAuthEndpoint();
        Token token = TokenManager.getToken(clientId, tokenEndpoint);
        if (token == null || !token.isActive()) {
            token = getAccessToken(messageContext, tokenEndpoint, config);
            TokenManager.addToken(clientId, tokenEndpoint, token);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Token exists in the token store.");
            }
        }
        return token;
    }

    /**
     * Get the access token from the token endpoint.
     *
     * @param messageContext Message context.
     * @param tokenEndpoint  Token endpoint.
     * @param config         Backend auth configuration.
     * @return Access token.
     * @throws OpenHealthcareException If an error occurs while fetching the token.
     */
    private static Token getAccessToken(
            MessageContext messageContext,
            String tokenEndpoint,
            BackendAuthConfig config)
            throws OpenHealthcareException {

        HttpPost postRequest = new HttpPost(tokenEndpoint);
        ArrayList<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair(Constants.OAUTH2_GRANT_TYPE, Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(
                "client_id", config.getClientId()));
        parameters.add(new BasicNameValuePair(
                "client_secret", String.valueOf(config.getClientSecret())));
        return Utils.initiateTokenRequest(messageContext, postRequest, parameters);
    }
}
