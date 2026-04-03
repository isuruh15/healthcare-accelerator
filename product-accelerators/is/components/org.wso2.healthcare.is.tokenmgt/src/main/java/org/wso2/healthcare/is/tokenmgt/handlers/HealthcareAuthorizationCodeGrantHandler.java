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
package org.wso2.healthcare.is.tokenmgt.handlers;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenRespDTO;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.oauth2.token.handlers.grant.AuthorizationCodeGrantHandler;
import org.wso2.healthcare.is.smart.auth.util.UserClaimResolver;

import java.util.Arrays;
import java.util.List;

/**
 * Custom grant handler for authorization code grant type to set the patient id in token call response according to the
 * SMART on FHIR specification -
 * http://www.hl7.org/fhir/smart-app-launch/scopes-and-launch-context/index.html#requesting-context-with-scopes
 * <p>
 * Need to add the following configuration in the WSO2 IS deployment.toml
 * <p>
 * [oauth.grant_type.authorization_code]
 * grant_handler = "org.wso2.healthcare.is.tokenmgt.handlers.HealthcareAuthorizationCodeGrantHandler"
 */
public class HealthcareAuthorizationCodeGrantHandler extends AuthorizationCodeGrantHandler {

    /**
     * As per the
     * http://www.hl7.org/fhir/smart-app-launch/scopes-and-launch-context/index.html#requesting-context-with-scopes
     * This scope should be added as an allowed scope in IS's deployment.toml or via the UI
     * <p>
     * Example scopes: "openid", "fhirUser", "launch/patient"
     */
    public static final String PATIENT_LAUNCH_SCOPE = "launch/patient";

    /**
     * Default claim URI for patient ID - can be customized via deployment.toml
     * Example: http://wso2.org/claims/patientid
     */
    public static final String DEFAULT_PATIENT_CLAIM_URI = "http://wso2.org/claims/patientid";

    /**
     * As per the
     * http://www.hl7.org/fhir/smart-app-launch/scopes-and-launch-context/index.html#patient-specific-scopes
     * This scope is granted if the patient launch context is requested.
     */
    public static final String PATIENT_RESOURCES_READ_SCOPE = "patient/*.read";

    private static final Log LOG = LogFactory.getLog(HealthcareAuthorizationCodeGrantHandler.class);
    private static final String PATIENT_CLAIM_URI_PROPERTY = "OAuth.GrantType.AuthorizationCode.PatientClaimUri";

    @Override
    public OAuth2AccessTokenRespDTO issue(OAuthTokenReqMessageContext tokReqMsgCtx)
            throws IdentityOAuth2Exception {

        OAuth2AccessTokenRespDTO oAuth2AccessTokenRespDTO = super.issue(tokReqMsgCtx);

        List<String> requestedScopes = Arrays.asList(tokReqMsgCtx.getScope());
        if (!requestedScopes.contains(PATIENT_LAUNCH_SCOPE)) {
            // patient launch context has not been requested, hence no change to the token response
            if (LOG.isDebugEnabled()) {
                LOG.debug("Patient launch context has not been requested, hence no change to the token response.");
            }
            return oAuth2AccessTokenRespDTO;
        }

        AuthenticatedUser authenticatedUser = tokReqMsgCtx.getAuthorizedUser();

        try {
            // Get the patient claim URI from configuration or use default
            String claimUri = getPatientClaimUri();

            // Use the UserClaimResolver from smart auth component
            UserClaimResolver claimResolver = new UserClaimResolver();
            String patientId = claimResolver.getUserClaimValue(claimUri, authenticatedUser);

            if (StringUtils.isNotBlank(patientId)) {
                // set patient parameter and patient id in the token response
                oAuth2AccessTokenRespDTO.addParameter("patient", patientId);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Successfully set the patient property in the token response for user: "
                            + authenticatedUser.getUserName());
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Patient claim value is empty for user: " + authenticatedUser.getUserName()
                            + " with claim URI: " + claimUri);
                }
            }
        } catch (Exception e) {
            LOG.warn("Unable to add patient context to the token response: Error occurred while retrieving " +
                    "patient claim for user: " + authenticatedUser.getUserName(), e);
        }

        return oAuth2AccessTokenRespDTO;
    }

    /**
     * Get the patient claim URI from configuration or return default value
     *
     * @return Patient claim URI
     */
    private String getPatientClaimUri() {
        // In IS 7.2.0, configuration can be read from deployment.toml
        // For now, return the default claim URI
        // This can be enhanced to read from IdentityUtil.getProperty() or similar
        String claimUri = System.getProperty(PATIENT_CLAIM_URI_PROPERTY);
        if (StringUtils.isBlank(claimUri)) {
            claimUri = DEFAULT_PATIENT_CLAIM_URI;
        }
        return claimUri;
    }
}
