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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.healthcare.apim.backendauth.Constants;
import org.wso2.healthcare.apim.backendauth.Utils;
import org.wso2.healthcare.apim.backendauth.tokenmgt.Token;
import org.wso2.healthcare.apim.backendauth.tokenmgt.TokenManager;
import org.wso2.healthcare.apim.core.OpenHealthcareException;
import org.wso2.healthcare.apim.core.OpenHealthcareRuntimeException;
import org.wso2.healthcare.apim.core.config.BackendAuthConfig;

import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Mediator to authenticate with the backend using private key JWT.
 */
public class PrivateKeyJWTBackendAuthenticator implements BackendAuthHandler {

    private static final Log log = LogFactory.getLog(PrivateKeyJWTBackendAuthenticator.class);

    @Override
    public String fetchValidAccessToken(MessageContext messageContext, BackendAuthConfig backendAuthConfig) {

        log.info("PrivateKeyJWTBackendAuthenticator mediator is started.");

        String accessToken;
        String tokenEndpoint = backendAuthConfig.getAuthEndpoint();
        String keyAlias = backendAuthConfig.getPrivateKeyAlias();
        char[] keyStorePass = CarbonUtils.getServerConfiguration().getFirstProperty(
                Constants.CONFIG_KEYSTORE_PASSWORD).toCharArray();
        String clientId = backendAuthConfig.getClientId();
        int tenantId = Integer.parseInt(messageContext.getProperty(Constants.TENANT_INFO_ID).toString());
        KeyStoreManager keyStoreManager = KeyStoreManager.getInstance(tenantId);
        Key privateKey;
        Certificate publicCert;

        if (log.isDebugEnabled()) {
            log.debug("Configured client ID: " + clientId);
        }

        try {
            KeyStore primarykeyStore = keyStoreManager.getPrimaryKeyStore();
            privateKey = primarykeyStore.getKey(keyAlias, keyStorePass);
            publicCert = keyStoreManager.getDefaultPrimaryCertificate();
        } catch (Exception e) {
            log.error("Error occurred while retrieving private key from keystore.", e);
            throw new OpenHealthcareRuntimeException(e);
        }

        Token token = TokenManager.getToken(clientId, tokenEndpoint);
        if (token == null || !token.isActive()) {
            if (log.isDebugEnabled()) {
                log.debug("Access token not available in TokenManager.");
            }
            try {
                token = getAndAddNewToken(messageContext, privateKey, publicCert, backendAuthConfig);
            } catch (OpenHealthcareException e) {
                log.error("Error occurred while retrieving access token.", e);
                throw new OpenHealthcareRuntimeException(e);
            }
        }
        accessToken = token.getAccessToken();
        return accessToken;
    }

    /**
     * Retrieve token from the token store. If token is not available or inactive, generate new token.
     *
     * @param messageContext - to set error response
     * @param privateKey     - to sign the JWT
     * @return Token
     * @throws OpenHealthcareException - when error occurred while retrieving token
     */
    private synchronized Token getAndAddNewToken(MessageContext messageContext, Key privateKey, Certificate publicCert,
                                                 BackendAuthConfig config)
            throws OpenHealthcareException {

        String clientId = config.getClientId();
        String tokenEndpoint = config.getAuthEndpoint();
        Token token = TokenManager.getToken(clientId, tokenEndpoint);
        if (token == null || !token.isActive()) {
            String jwt = generateJWT(clientId, tokenEndpoint, privateKey, publicCert);
            token = getAccessToken(messageContext, tokenEndpoint, jwt);
            TokenManager.addToken(clientId, tokenEndpoint, token);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Token exists in the token store.");
            }
        }
        return token;
    }

    /**
     * Generate JWT token with signature.
     *
     * @param clientId      - to add as subject
     * @param tokenEndpoint - audience
     * @param privateKey    - to sign the JWT
     * @return JWT as string
     * @throws OpenHealthcareException JOSEException
     */
    private String generateJWT(String clientId, String tokenEndpoint, Key privateKey, Certificate publicCert) throws
            OpenHealthcareException {

        try {
            RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) privateKey;
            JWSSigner signer = new RSASSASigner(rsaPrivateKey);
            long curTimeInMillis = System.currentTimeMillis();

            JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();
            claimsSetBuilder.issuer(clientId);
            claimsSetBuilder.subject(clientId);
            claimsSetBuilder.audience(tokenEndpoint);
            claimsSetBuilder.jwtID(UUID.randomUUID().toString());
            claimsSetBuilder.issueTime((new Date(curTimeInMillis)));
            claimsSetBuilder.notBeforeTime((new Date(curTimeInMillis)));
            claimsSetBuilder.expirationTime(new Date(curTimeInMillis + 300000)); // Maximum expiration time is 5 min.
            JWTClaimsSet claimsSet = claimsSetBuilder.build();

            JWSAlgorithm signatureAlgorithm = new JWSAlgorithm(JWSAlgorithm.RS256.getName());
            JWSHeader.Builder headerBuilder = new JWSHeader.Builder(signatureAlgorithm);
            headerBuilder.type(JOSEObjectType.JWT);
            headerBuilder.keyID(Utils.getKID((X509Certificate) publicCert));
            JWSHeader jwsHeader = headerBuilder.build();

            SignedJWT signedJWT = new SignedJWT(jwsHeader, claimsSet);
            signedJWT.sign(signer);
            return signedJWT.serialize();

        } catch (JOSEException e) {
            String message = "Error occurred while signing the JWT.";
            log.error(message, e);
            throw new OpenHealthcareException(message, e);
        }
    }

    /**
     * Retrieve access token from the token endpoint. This makes external HTTP call to the token endpoint.
     *
     * @param messageContext - to set error response
     * @param tokenEndpoint  - to retrieve access token
     * @param jwt            - signed JWT including claims
     * @return Token
     * @throws OpenHealthcareException - when error occurred while retrieving access token
     */
    private static Token getAccessToken(
            MessageContext messageContext,
            String tokenEndpoint,
            String jwt)
            throws OpenHealthcareException {

        HttpPost postRequest = new HttpPost(tokenEndpoint);
        ArrayList<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair(Constants.OAUTH2_GRANT_TYPE, Constants.CLIENT_CREDENTIALS));
        parameters.add(new BasicNameValuePair(
                Constants.OAUTH2_CLIENT_ASSERTION_TYPE, Constants.OAUTH2_CLIENT_ASSERTION_TYPE_JWT_BEARER));
        parameters.add(new BasicNameValuePair(Constants.OAUTH2_CLIENT_ASSERTION, jwt));

        return Utils.initiateTokenRequest(messageContext, postRequest, parameters);
    }

    public String getType() {
        return null;
    }

    public void setTraceState(int traceState) {
        traceState = 0;
    }

    public int getTraceState() {
        return 0;
    }
}
