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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.healthcare.apim.backendauth.tokenmgt.Token;
import org.wso2.healthcare.apim.core.OpenHealthcareException;
import org.wso2.healthcare.apim.core.OpenHealthcareRuntimeException;
import org.wso2.healthcare.apim.core.config.BackendAuthConfig;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

/**
 * Utility class for the backend authentication.
 */
public class Utils {
    private static final Log log = LogFactory.getLog(Utils.class);
    public static final String ALLOW_ALL = "AllowAll";
    public static final String STRICT = "Strict";
    public static final String HOST_NAME_VERIFIER = "httpclient.hostnameVerifier";
    public static final String HTTP_PROTOCOL = "http";
    public static final String HTTPS_PROTOCOL = "https";
    private static final String[] SUPPORTED_HTTP_PROTOCOLS = {"TLSv1.2"};

    private static final JsonParser parser = new JsonParser();

    public static void setErrorResponse(MessageContext messageContext, Throwable e, int errorCode, String msg) {

        messageContext.setProperty(SynapseConstants.ERROR_CODE, errorCode);
        messageContext.setProperty(SynapseConstants.ERROR_MESSAGE, msg);
        messageContext.setProperty(SynapseConstants.ERROR_DETAIL, e.getMessage());
        messageContext.setProperty(SynapseConstants.ERROR_EXCEPTION, e);
        ((Axis2MessageContext) messageContext).getAxis2MessageContext().setProperty(Constants.HTTP_SC, errorCode);
    }

    /**
     * Get closeable https client.
     *
     * @return Closeable https client
     * @throws OpenHealthcareException exception
     */
    public static CloseableHttpClient getHttpsClient(String trustStorePath, char[] trustStorePass) throws OpenHealthcareException {

        SSLConnectionSocketFactory sslsf = createSSLConnectionSocketFactory(trustStorePath, trustStorePass);

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register(HTTP_PROTOCOL, new PlainConnectionSocketFactory())
                .register(HTTPS_PROTOCOL, sslsf)
                .build();

        final PoolingHttpClientConnectionManager connectionManager = getPoolingHttpClientConnectionManager(socketFactoryRegistry);

        return HttpClients.custom().setConnectionManager(connectionManager).build();
    }

    /**
     * Get the pooling http client connection manager.
     *
     * @param socketFactoryRegistry - Socket factory registry
     * @return PoolingHttpClientConnectionManager
     */
    private static PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager(Registry<ConnectionSocketFactory> socketFactoryRegistry) {
        final PoolingHttpClientConnectionManager connectionManager = (socketFactoryRegistry != null) ?
                new PoolingHttpClientConnectionManager(socketFactoryRegistry) :
                new PoolingHttpClientConnectionManager();

        // configuring default maximum connections
        // todo: make these values configurable:
        connectionManager.setMaxTotal(Constants.DEFAULT_MAX_TOTAL_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(Constants.DEFAULT_MAX_CONNECTIONS_PER_ROUTE);
        return connectionManager;
    }

    /**
     * create an SSL Connection Socket Factory.
     *
     * @return SSLConnectionSocketFactory
     * @throws OpenHealthcareException when failed to create the SSL Connection Socket Factory
     */
    private static SSLConnectionSocketFactory createSSLConnectionSocketFactory(String trustStorePath, char[] trustStorePassword)
            throws OpenHealthcareException {

        KeyStore trustStore = null;

        trustStore = loadKeyStore(
                trustStorePath,
                trustStorePassword);

        // Trust own CA and all self-signed certs
        SSLContext sslcontext;
        try {
            sslcontext = SSLContexts.custom().loadTrustMaterial(trustStore, (TrustStrategy) new TrustSelfSignedStrategy()).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new OpenHealthcareException("Unable to create the ssl context", e);
        }

        // Allow TLSv1 protocol only
        return new SSLConnectionSocketFactory(sslcontext, SUPPORTED_HTTP_PROTOCOLS,
                null, getX509HostnameVerifier());

    }


    /**
     * Load the keystore when the location and password is provided.
     *
     * @param keyStoreLocation Location of the keystore
     * @param keyStorePassword Keystore password
     * @return Keystore as an object
     * @throws OpenHealthcareException when failed to load Keystore from given details
     */
    public static KeyStore loadKeyStore(String keyStoreLocation, char[] keyStorePassword)
            throws OpenHealthcareException {

        KeyStore keyStore;

        try (FileInputStream inputStream = new FileInputStream(keyStoreLocation)) {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(inputStream, keyStorePassword);
            return keyStore;
        } catch (KeyStoreException e) {
            throw new OpenHealthcareException("Error while retrieving aliases from keystore", e);
        } catch (IOException | CertificateException | NoSuchAlgorithmException e) {
            throw new OpenHealthcareException("Error while loading keystore", e);
        }
    }

    /**
     * Get the Hostname Verifier property in set in system properties.
     *
     * @return X509HostnameVerifier
     */
    public static X509HostnameVerifier getX509HostnameVerifier() {

        String hostnameVerifierOption = System.getProperty(HOST_NAME_VERIFIER);
        X509HostnameVerifier hostnameVerifier;

        if (ALLOW_ALL.equals(hostnameVerifierOption)) {
            hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
        } else if (STRICT.equals(hostnameVerifierOption)) {
            hostnameVerifier = SSLSocketFactory.STRICT_HOSTNAME_VERIFIER;
        } else {
            hostnameVerifier = SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Proceeding with %s : %s", HOST_NAME_VERIFIER,
                    hostnameVerifierOption));
        }
        return hostnameVerifier;

    }

    /**
     * Helper method to add kid claim into to JWT_HEADER.
     *
     * @param cert X509 certificate
     * @return KID
     */
    public static String getKID(X509Certificate cert) {
        String serialNumber = cert.getSerialNumber().toString();
        String issuerName = cert.getIssuerDN().getName();
        String kid = issuerName + "#" + serialNumber;
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(kid.getBytes(StandardCharsets.UTF_8));
    }

    public static Token initiateTokenRequest(MessageContext messageContext, HttpPost postRequest,
                                             List<NameValuePair> parameters)
            throws OpenHealthcareException {
        long curTimeInMillis = System.currentTimeMillis();
        String trustStorePath = System.getProperty(Constants.NET_SSL_TRUST_STORE);
        char[] trustStorePass = CarbonUtils.getServerConfiguration().getFirstProperty(
                Constants.CONFIG_TRUSTSTORE_PASSWORD).toCharArray();

        try {
            postRequest.setEntity(new UrlEncodedFormEntity(parameters));
        } catch (UnsupportedEncodingException e) {
            String message = "Error occurred while preparing access token request payload.";
            log.error(message);
            throw new OpenHealthcareException(message, e);
        }
        CloseableHttpClient httpsClient = Utils.getHttpsClient(trustStorePath, trustStorePass);
        CloseableHttpResponse response;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Token request URI: " + postRequest.getURI());
            }
            response = httpsClient.execute(postRequest);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity == null) {
                String message = "Failed to retrieve access token : No entity received.";
                log.error(message);
                throw new OpenHealthcareException(message);
            }
            int responseStatus = response.getStatusLine().getStatusCode();
            String respMessage;
            respMessage = EntityUtils.toString(responseEntity);

            if (responseStatus == HttpURLConnection.HTTP_OK) {
                return extractToken(respMessage, curTimeInMillis);
            } else {
                handleTokenRequestError(messageContext, responseStatus, respMessage);
            }
        } catch (IOException e) {
            throw new OpenHealthcareRuntimeException(e);
        }
        throw new OpenHealthcareException("Error occurred while retrieving access token.");
    }

    /**
     * Extract token from the response message.
     *
     * @param respMessage     - response message
     * @param curTimeInMillis - current time in milliseconds
     * @return - Token
     */
    private static Token extractToken(String respMessage, long curTimeInMillis) {
        JsonElement jsonElement = parser.parse(respMessage);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String accessToken = jsonObject.get("access_token").getAsString();
        long expireIn = jsonObject.get("expires_in").getAsLong();

        Token token = new Token(accessToken, curTimeInMillis, expireIn * 1000);
        if (log.isDebugEnabled()) {
            log.debug(token);
        }
        return token;
    }

    /**
     * Handle error response from the token endpoint.
     *
     * @param messageContext - to set error response
     * @param responseStatus - HTTP status code
     * @param respMessage    - response message
     * @throws OpenHealthcareException - to propagate the error
     */
    private static void handleTokenRequestError(MessageContext messageContext, int responseStatus, String respMessage) throws OpenHealthcareException {
        String message = "Error occurred while retrieving access token. Response: [Status : " + responseStatus + " Message: " + respMessage + "]";
        log.error(message);
        OpenHealthcareException exp = new OpenHealthcareException(message);
        Utils.setErrorResponse(messageContext, exp, responseStatus, message);
        throw exp;
    }

    /**
     * Evaluate synapse configValue and return the value.
     * Supported expressions: $header:<header_name>, $ctx:<property_name>
     *
     * @param config         - config object to evaluate
     * @param messageContext - message context
     */
    public static void resolveConfigValues(BackendAuthConfig config, MessageContext messageContext) throws OpenHealthcareRuntimeException {

        String headerName;
        try {

            if (!config.getClientId().contains("$") && !config.getPrivateKeyAlias().contains("$")) {
                if (log.isDebugEnabled()) {
                    log.debug("Not configured runtime config resolution. Continuing with static configs.");
                }
                return;
            }
            // Validate format for clientId
            if (config.getClientId().contains("$") && !config.getClientId().contains(":")) {
                throw new OpenHealthcareRuntimeException("Invalid configuration format for clientId: " +
                        config.getClientId());
            }

            // Validate format for privateKeyAlias
            if (config.getPrivateKeyAlias().contains("$") && !config.getPrivateKeyAlias().contains(":")) {
                throw new OpenHealthcareRuntimeException("Invalid configuration format for privateKeyAlias: " +
                        config.getPrivateKeyAlias());
            }
            if (config.getClientId().contains("$ctx")) {
                String configVal = config.getClientId();
                Object clientIdProperty = messageContext.getProperty(configVal.substring(
                        configVal.indexOf(":") + 1));
                if (clientIdProperty == null) {
                    throw new OpenHealthcareRuntimeException("Context property not found: " + configVal);
                }
                String clientId = clientIdProperty.toString();
                if (log.isDebugEnabled()) {
                    log.debug("Client ID context property: " + clientId);
                }
                config.setClientId(clientId);
            }
            if (config.getPrivateKeyAlias().contains("$ctx")) {
                String configVal = config.getPrivateKeyAlias();

                Object keyAliasProperty = messageContext.getProperty(configVal.substring(
                        configVal.indexOf(":") + 1));
                if (keyAliasProperty == null) {
                    throw new OpenHealthcareRuntimeException("Context property not found: " + configVal);
                }
                String keyAlias = keyAliasProperty.toString();
                if (log.isDebugEnabled()) {
                    log.debug("Key alias context property: " + keyAlias);
                }
                config.setPrivateKeyAlias(keyAlias);
            }
            if (config.getClientId().contains("$header")) {
                headerName = config.getClientId().substring(config.getClientId().indexOf(":") + 1);
                if (log.isDebugEnabled()) {
                    log.debug("Client ID Header name: " + headerName);
                }
                config.setClientId(resolveFromHeaders(messageContext, headerName));
            }
            if (config.getPrivateKeyAlias().contains("$header")) {
                headerName = config.getPrivateKeyAlias().substring(config.getPrivateKeyAlias().indexOf(":") + 1);
                if (log.isDebugEnabled()) {
                    log.debug("Key Alias Header name: " + headerName);
                }
                config.setPrivateKeyAlias(resolveFromHeaders(messageContext, headerName));
            }
        } catch (OpenHealthcareRuntimeException e) {
            log.error("Error occurred while resolving runtime configuration values.", e);
            throw e;
        }
    }

    private static String resolveFromHeaders(MessageContext messageContext, String headerName) throws OpenHealthcareRuntimeException {
        if (messageContext instanceof Axis2MessageContext) {
            org.apache.axis2.context.MessageContext axisMsgCtx =
                    ((Axis2MessageContext) messageContext).getAxis2MessageContext();
            Object headers = axisMsgCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            if (headers instanceof Map) {
                Map headersMap = (Map) headers;
                if (headersMap.containsKey(headerName)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Header value: " + headersMap.get(headerName));
                    }
                    return (String) headersMap.get(headerName);
                } else {
                    log.warn("Configured header:" + headerName + " is not available in the request headers map");
                    throw new OpenHealthcareRuntimeException("Configured header is not available in the request " +
                            "headers map");
                }
            } else {
                log.warn("Transport headers are not available in the message context.");
                throw new OpenHealthcareRuntimeException("Transport headers are not available in the message context.");
            }
        } else {
            log.error("Message context is not an instance of Axis2MessageContext.");
            throw new OpenHealthcareRuntimeException("Message context is not an instance of Axis2MessageContext.");
        }
    }

    /**
     * Validate the backend auth config.
     *
     * @param config - BackendAuthConfig object
     * @return true if valid
     */
    public static boolean validateConfig(BackendAuthConfig config) {

        if (StringUtils.isEmpty(config.getAuthType()) || StringUtils.isEmpty(config.getAuthEndpoint()) ||
                StringUtils.isEmpty(config.getClientId())) {
            log.error("One or more required configs are missing in the policy attributes.");
            return false;
        } else if (Constants.POLICY_ATTR_AUTH_TYPE_PKJWT.equals(config.getAuthType()) &&
                StringUtils.isEmpty(config.getPrivateKeyAlias())) {
            log.error("Key alias is missing in the policy attributes.");
            return false;
        } else if (Constants.POLICY_ATTR_AUTH_TYPE_CLIENT_CRED.equals(config.getAuthType()) &&
                config.getClientSecret().length == 0) {
            log.error("Client secret is missing in the policy attributes.");
            return false;
        }
        if (log.isDebugEnabled()) {
            log.debug("Config validation Successful.");
        }
        return true;
    }
}
