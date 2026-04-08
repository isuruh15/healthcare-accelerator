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

/**
 * Constants class for the backend auth component.
 */
public class Constants {
    public static final String TOKEN_KEY_SEPARATOR = "_";
    public static final String HTTP_SC = "HTTP_SC";
    public static final String TENANT_INFO_ID = "tenant.info.id";
    public static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 2;
    public static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 20;
    public static final String HEADER_NAME_AUTHORIZATION = "Authorization";
    public static final String HEADER_VALUE_BEARER = "Bearer ";

    public static final String POLICY_ATTR_AUTH_TYPE_CLIENT_CRED = "client-credentials";
    public static final String POLICY_ATTR_AUTH_TYPE_PKJWT = "pkjwt";

    public static final String CONFIG_KEYSTORE_PASSWORD = "Security.KeyStore.Password";
    public static final String CONFIG_TRUSTSTORE_PASSWORD = "Security.TrustStore.Password";
    public static final String CLIENT_CREDENTIALS = "client_credentials";
    public static final String NET_SSL_TRUST_STORE = "javax.net.ssl.trustStore";
    public static final String OAUTH2_GRANT_TYPE = "grant_type";
    public static final String OAUTH2_CLIENT_ASSERTION_TYPE = "client_assertion_type";
    public static final String OAUTH2_CLIENT_ASSERTION_TYPE_JWT_BEARER = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    public static final String OAUTH2_CLIENT_ASSERTION = "client_assertion";
}
