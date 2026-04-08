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

import java.util.Calendar;

/**
 * Configuration related constants
 */
public class ConfigConstants {

    public static final String WF_TYPE_DEFAULT = "default";
    public static final String WF_TYPE_BPMN = "BPMN";

    public static final String DEFAULT_ACCOUNT_DS_NAME = "jdbc/OHAccountDB";
    public static final String DEFAULT_REGISTRATION_DS_NAME = "jdbc/OHRegistrationDB";

    public static final String DEFAULT_FHIR_SERVER_NAME = "WSO2 Open-healthcare FHIR Server";
    public static final String DEFAULT_FHIR_SERVER_VERSION = "1.0.0";

    public static final String DEFAULT_PII_STORE_IMPL = "org.wso2.healthcare.apim.core.account.saas.InternalPIIStore";

    public static final String DEFAULT_PATIENT_ID_CLAIM_URI = "http://wso2.org/claims/patientId";
    public static final String DEFAULT_PATIENT_ID_KEY = "patientId";
    public static final String DEFAULT_FHIRUSER_CLAIM_CONTEXT = "/r4/Patient";
    public static final String DEFAULT_FHIRUSER_MAPPED_LOCAL_CLAIM = "http://wso2.org/claims/patientId";

    public static final String DEFAULT_DEPLOYMENT_NAME = "Open Healthcare";
    public static final String DEFAULT_DEPLOYMENT_NAME_CONTAINER_CSS = "";
    public static final String DEFAULT_WEBAPP_TERMS_OF_USE = "https://wso2.com/terms-of-use";
    public static final String DEFAULT_WEBAPP_PRIVACY_POLICY = "https://wso2.com/privacy-policy";
    public static final String DEFAULT_WEBAPP_COOKIE_POLICY = "https://wso2.com/cookie-policy";
    public static final String DEFAULT_WEBAPP_LOGO = "images/wso2-logo.svg";
    public static final String DEFAULT_WEBAPP_LOGO_HEIGHT = "";
    public static final String DEFAULT_WEBAPP_LOGO_WIDTH = "";
    public static final String DEFAULT_WEBAPP_LOGO_CONTAINER_CSS = "";
    public static final String DEFAULT_WEBAPP_FAVICON = "libs/theme/assets/images/favicon.ico";
    public static final String DEFAULT_WEBAPP_TITLE = "WSO2 Healthcare";
    public static final String DEFAULT_WEBAPP_FOOTER =
            "WSO2 Healthcare";
    public static final String DEFAULT_WEBAPP_FOOTER_SECONDARY_HTML = "";
    public static final String DEFAULT_WEBAPP_MAIN_COLOR = "#342382";
    public static final String DEFAULT_AUTH_SELFSIGNUP_URL = "";
    public static final String DEFAULT_WEBAPP_SIGN_UP_SUCCESS_MSG = "User registration completed successfully";
}
