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

package org.wso2.healthcare.apim.backendauth;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.wso2.healthcare.apim.backendauth.impl.BackendAuthHandler;
import org.wso2.healthcare.apim.backendauth.impl.ClientCredentialsBackendAuthenticator;
import org.wso2.healthcare.apim.backendauth.impl.PrivateKeyJWTBackendAuthenticator;
import org.wso2.healthcare.apim.core.OpenHealthcareEnvironment;
import org.wso2.healthcare.apim.core.OpenHealthcareException;
import org.wso2.healthcare.apim.core.config.BackendAuthConfig;

import java.util.Map;

/**
 * Backend authenticator mediator.
 */
public class BackendAuthenticator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(BackendAuthenticator.class);
    private String authType;
    private String tokenEndpoint;
    private String clientId;
    private char[] clientSecret;
    private String keyAlias;
    private String configValue;
    private final Map<String, BackendAuthConfig> backendAuthConfig;
    private BackendAuthConfig masterConfig;
    private final PrivateKeyJWTBackendAuthenticator privateKeyJWTBackendAuthenticator;
    private final ClientCredentialsBackendAuthenticator clientCredentialsBackendAuthenticator;

    public BackendAuthenticator() throws OpenHealthcareException {
        if (log.isDebugEnabled()) {
            log.debug("Backend authenticator mediator is initialized.");
        }
        backendAuthConfig = OpenHealthcareEnvironment.getInstance().getConfig().getBackendAuthConfig();
        privateKeyJWTBackendAuthenticator = new PrivateKeyJWTBackendAuthenticator();
        clientCredentialsBackendAuthenticator = new ClientCredentialsBackendAuthenticator();
    }

    @Override
    public boolean mediate(MessageContext messageContext) {
        if (log.isDebugEnabled()) {
            log.debug("Backend authenticator mediator is invoked.");
        }
        try {
            masterConfig.setClientId(clientId);
            masterConfig.setPrivateKeyAlias(keyAlias);
            Utils.resolveConfigValues(masterConfig, messageContext);

            if (!Utils.validateConfig(masterConfig)) {
                log.error("Config validation failed.");
                return false;
            }

            String accessToken;
            BackendAuthHandler currentAuthenticator;

            switch (masterConfig.getAuthType()) {
                case Constants.POLICY_ATTR_AUTH_TYPE_PKJWT:
                    if (log.isDebugEnabled()) {
                        log.debug("Auth type is PKJWT.");
                    }
                    currentAuthenticator = privateKeyJWTBackendAuthenticator;
                    accessToken = privateKeyJWTBackendAuthenticator.fetchValidAccessToken(messageContext, masterConfig);
                    break;
                case Constants.POLICY_ATTR_AUTH_TYPE_CLIENT_CRED:
                    if (log.isDebugEnabled()) {
                        log.debug("Auth type is CLIENT CREDENTIALS.");
                    }
                    currentAuthenticator = clientCredentialsBackendAuthenticator;
                    accessToken = clientCredentialsBackendAuthenticator.fetchValidAccessToken(messageContext, masterConfig);
                    break;
                default:
                    log.error("Auth type is not supported.");
                    return false;
            }

            if (messageContext instanceof Axis2MessageContext) {
                org.apache.axis2.context.MessageContext axisMsgCtx =
                        ((Axis2MessageContext) messageContext).getAxis2MessageContext();
                Object headers = axisMsgCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                if (headers instanceof Map) {
                    Map headersMap = (Map) headers;
                    if (headersMap.containsKey(Constants.HEADER_NAME_AUTHORIZATION)) {
                        headersMap.remove(Constants.HEADER_NAME_AUTHORIZATION);
                    }
                    headersMap.put(Constants.HEADER_NAME_AUTHORIZATION, currentAuthenticator.getAuthHeaderScheme() + accessToken);
                    axisMsgCtx.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);
                    if (log.isDebugEnabled()) {
                        log.debug("Transport headers updated with Authorization header. Headers count = "+ headersMap.size());
                    }
                } else {
                    log.warn("Transport headers are not available in the message context.");
                }
            } else {
                log.error("Message context is not an instance of Axis2MessageContext.");
            }
            return true;
        } catch (Exception e) {
            log.error("Exception occurred in the mediator", e);
        }
        return false;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
        if (!StringUtils.isEmpty(this.authType)) {
            masterConfig.setAuthType(authType);
        }
    }

    public void setConfigValue(String configValue) {
        masterConfig = new BackendAuthConfig();
        this.configValue = configValue;
        if (backendAuthConfig.containsKey(configValue)) {
            BackendAuthConfig currentConfig = backendAuthConfig.get(configValue);
            masterConfig.setAuthType(currentConfig.getAuthType());
            masterConfig.setAuthEndpoint(currentConfig.getAuthEndpoint());
            masterConfig.setClientId(currentConfig.getClientId());
            masterConfig.setClientSecret(currentConfig.getClientSecret());
            masterConfig.setPrivateKeyAlias(currentConfig.getPrivateKeyAlias());
        }
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
        if (!StringUtils.isEmpty(tokenEndpoint)) {
            masterConfig.setAuthEndpoint(tokenEndpoint);
        }
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
        if (!StringUtils.isEmpty(clientId)) {
            masterConfig.setClientId(clientId);
        }
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret.toCharArray();
        if (this.clientSecret.length != 0) {
            masterConfig.setClientSecret(clientSecret.toCharArray());
        }
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
        if (!StringUtils.isEmpty(keyAlias)) {
            masterConfig.setPrivateKeyAlias(keyAlias);
        }
    }
}
