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

import net.consensys.cava.toml.Toml;
import net.consensys.cava.toml.TomlArray;
import net.consensys.cava.toml.TomlParseResult;
import net.consensys.cava.toml.TomlTable;
import org.apache.axiom.om.OMElement;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.model.Scope;
import org.wso2.carbon.apimgt.api.model.policy.ApplicationPolicy;
import org.wso2.carbon.apimgt.api.model.policy.PolicyConstants;
import org.wso2.carbon.apimgt.api.model.policy.QuotaPolicy;
import org.wso2.carbon.apimgt.api.model.policy.RequestCountLimit;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.healthcare.apim.core.OpenHealthcareException;
import org.wso2.healthcare.apim.core.ReferenceHolder;
import org.wso2.healthcare.apim.core.utils.CommonUtil;
import org.wso2.healthcare.apim.core.utils.ScopeMgtUtil;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;
import org.wso2.securevault.commons.MiscellaneousUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class holds configuration details related to Open-Healthcare
 */
public class OpenHealthcareConfig {

    private static final Log LOG = LogFactory.getLog(OpenHealthcareConfig.class);

    private final TomlParseResult config;
    private AccountConfig accountConfig;
    private SignUpConfig signUpConfig;
    private AlertConfig alertConfig;
    private SecretResolver secretResolver;
    private CaptchaConfig captchaConfig;
    private FHIRServerConfig fhirServerConfig;
    private ThrottlingConfig throttlingConfig;
    private APIHubConfig apiHubConfig;
    private ClaimMgtConfig claimMgtConfig;
    private WebApplicationConfig webApplicationConfig;
    private Map<String, MailNotificationConfig> notificationMailConfig;
    private OrganizationConfig organizationConfig;
    private ScopeMgtConfig scopeMgtConfig;
    private Map<String, BackendAuthConfig> backendAuthConfig;
    private SmartConfig smartConfig;

    private OpenHealthcareConfig(TomlParseResult config) throws OpenHealthcareException {
        this.config = config;
        parse(config);
    }

    /**
     * Build open-healthcare configuration model
     *
     * @return
     * @throws OpenHealthcareException
     */
    public static OpenHealthcareConfig build() throws OpenHealthcareException {
        Path deploymentTOML = Paths.get(CarbonUtils.getCarbonConfigDirPath() + File.separator + "deployment.toml");
        if (LOG.isDebugEnabled()) {
            LOG.debug("Loading Open-Healthcare configuration from : " + deploymentTOML.toString());
        }
        try {
            TomlParseResult tomlParseResult = Toml.parse(deploymentTOML);
            if (tomlParseResult.hasErrors()) {
                StringBuilder strBuilder =
                        new StringBuilder("Error occurred while parsing the configuration file. Errors: \n");
                tomlParseResult.errors().forEach(tomlParseError -> {
                    strBuilder.append(tomlParseError.toString());
                    strBuilder.append("\n");
                });
                throw new OpenHealthcareException(strBuilder.toString());
            }
            return new OpenHealthcareConfig(tomlParseResult);
        } catch (IOException e) {
            throw new OpenHealthcareException("Error occurred while parsing the deployment.toml file", e);
        }
    }

    public AccountConfig getAccountConfig() {
        return accountConfig;
    }

    public SignUpConfig getSignUpConfig() {
        return signUpConfig;
    }

    public AlertConfig getAlertConfig() {
        return alertConfig;
    }

    public CaptchaConfig getCaptchaConfig() {
        return captchaConfig;
    }

    public FHIRServerConfig getFHIRServerConfig() {
        return fhirServerConfig;
    }

    public ThrottlingConfig getThrottlingConfig() {
        return throttlingConfig;
    }

    public APIHubConfig getApiHubConfig() {
        return apiHubConfig;
    }

    public WebApplicationConfig getWebApplicationConfig() {
        return webApplicationConfig;
    }

    public ClaimMgtConfig getClaimMgtConfig() {
        return claimMgtConfig;
    }

    public Map<String, MailNotificationConfig> getNotificationMailConfig() {
        return notificationMailConfig;
    }

    public OrganizationConfig getOrganizationConfig() {
        return organizationConfig;
    }

    public ScopeMgtConfig getScopeMgtConfig() {
        return scopeMgtConfig;
    }

    public Map<String, BackendAuthConfig> getBackendAuthConfig() {
        return backendAuthConfig;
    }

    public SmartConfig getSmartConfig() {
        return smartConfig;
    }

    private void parse(TomlParseResult config) throws OpenHealthcareException {

        secretResolver = SecretResolverFactory.create((OMElement) null, false);
        if (!secretResolver.isInitialized()) {
            secretResolver.init(ReferenceHolder.getInstance().getSecretCallbackHandlerService().getSecretCallbackHandler());
        }
        // Parse account configuration
        accountConfig = buildAccountConfig();
        // Parse sign-up configurations
        signUpConfig = buildSignUpConfig();
        // Parse alert config
        alertConfig = buildAlertConfig();
        // Parse captcha config
        captchaConfig = buildCaptchaConfig();
        // Parse FHIR server config
        fhirServerConfig = buildFHIRServerConfig();
        // Parse throttling config
        throttlingConfig = buildThrottlingConfig();
        // Parse APIHub config
        apiHubConfig = buildAPIHubConfig();
        // Parse claim management config
        claimMgtConfig = buildClaimMgtConfig();
        // Parse webapp config
        webApplicationConfig = buildWebApplicationConfig();
        // Parse email notifications config
        notificationMailConfig = buildNotificationMailConfig();
        //Parse organization config
        organizationConfig = buildOrganizationconfig();
        //Parse scope mgt config
        scopeMgtConfig = buildScopeMgtConfig();
        //Parse backend auth config
        backendAuthConfig = buildBackendAuthConfig();
        //Parse SMART config
        smartConfig = buildSmartConfig();
    }

    private AccountConfig buildAccountConfig() throws OpenHealthcareException {
        AccountConfig accountConfig = new AccountConfig();

        Object accountConfigRootObj = config.get("healthcare.saas.account");
        if (accountConfigRootObj instanceof TomlTable) {
            TomlTable accountConfigTable = (TomlTable) accountConfigRootObj;
            accountConfig.setEnable(accountConfigTable.getBoolean("enable", () -> false));
            accountConfig.setAccountDataSourceName(
                    accountConfigTable.getString("account_datasource", () -> ConfigConstants.DEFAULT_ACCOUNT_DS_NAME));
            accountConfig.setRegistrationDataSourceName(
                    accountConfigTable.getString("registration_datasource", () -> ConfigConstants.DEFAULT_REGISTRATION_DS_NAME));
            accountConfig.setPiiStoreConfig(buildPiiStoreConfig());
        }
        return accountConfig;
    }

    private PIIStoreConfig buildPiiStoreConfig() throws OpenHealthcareException {
        PIIStoreConfig piiStoreConfig = new PIIStoreConfig();
        Object piiStoreRootObj = config.get("healthcare.saas.account.pii_store");
        if (piiStoreRootObj instanceof  TomlTable) {
            TomlTable piiStoreConfigTable = (TomlTable) piiStoreRootObj;
            piiStoreConfig.setStoreImplClass(piiStoreConfigTable.
                    getString("store", () -> ConfigConstants.DEFAULT_PII_STORE_IMPL));

            if (piiStoreConfigTable.isTable("parameters")) {
                TomlTable parameterTable = piiStoreConfigTable.getTable("parameters");
                if (parameterTable != null) {
                    parameterTable.dottedKeySet().
                            forEach((key) -> piiStoreConfig.addParameter(key,
                                    new String(resolveSecret(parameterTable.getString(key)))));
                }
            }
        }
        return piiStoreConfig;
    }

    private SignUpConfig buildSignUpConfig() throws OpenHealthcareException {
        SignUpConfig signUpConfig = new SignUpConfig();

        Object signUpRootObj = config.get("healthcare.saas.signup");
        if (signUpRootObj instanceof  TomlTable) {
            TomlTable signUpTable = (TomlTable) signUpRootObj;
            signUpConfig.setUniqueAccountPerEmail(signUpTable.getBoolean("unique_email_account", () -> false));
        }
        signUpConfig.setAccountCreationWFConfig(buildWorkflowConfig("healthcare.saas.signup.workflow"));

        return signUpConfig;
    }

    private CaptchaConfig buildCaptchaConfig() throws OpenHealthcareException {
        Object captchaConfObj = config.get("healthcare.captcha");
        CaptchaConfig captchaConfig = new CaptchaConfig();
        if (captchaConfObj instanceof  TomlTable) {
            TomlTable captchaTable = (TomlTable) captchaConfObj;

            captchaConfig.setEnable(captchaTable.getBoolean("enable", () -> false));
            if (captchaConfig.isEnable()) {
                captchaConfig.setApiUrl(captchaTable.getString("api_url", () -> null));
                captchaConfig.setVerifyUrl(captchaTable.getString("verify_url", () -> null));
                captchaConfig.setSiteKey(captchaTable.getString("site_key", () -> null));

                String secretKey = captchaTable.getString("secret_key", () -> null);
                if (secretKey != null) {
                    captchaConfig.setSecretKey(resolveSecret(secretKey));
                }
                // Check whether all the captcha configs are full filled
                if (StringUtils.isEmpty(captchaConfig.getApiUrl()) || StringUtils.isEmpty(captchaConfig.getVerifyUrl()) ||
                        StringUtils.isEmpty(captchaConfig.getSiteKey()) || captchaConfig.getSecretKey() == null) {
                    throw new OpenHealthcareException("\"api_url\", \"verify_url\", \"site_key\", \"secret_key\" are " +
                            "mandatory configurations under \"[healthcare.saas.signup.captcha]\", when sign-up captcha is " +
                            "enabled");
                }
            }
        }
        return captchaConfig;
    }

    private AlertConfig buildAlertConfig() throws OpenHealthcareException {
        Object alertConfObj = config.get("healthcare.alert");
        AlertConfig alertConfig = new AlertConfig();
        if (alertConfObj instanceof TomlTable) {
            TomlTable alertTable = (TomlTable) alertConfObj;
            alertConfig.setAlertType(AlertConfig.AlertType.fromValue(alertTable.getString("type", () -> "log")));
            alertConfig.setLogFormat(
                    AlertConfig.LogFormat.fromValue(alertTable.getString("log_format", () -> "structured")));
            alertConfig.setEscalationEnable(alertTable.getBoolean("escalation.enable", () -> false));

            if (alertConfig.isEscalationEnable()) {
                alertConfig.setEscalationWorkflowConfig(buildWorkflowConfig("healthcare.alert.escalation.workflow"));
            }
        }
        return alertConfig;
    }

    private FHIRServerConfig buildFHIRServerConfig() {
        Object fhirConfObj = config.get("healthcare.fhir");
        FHIRServerConfig fhirServerConfig = new FHIRServerConfig();
        if (fhirConfObj instanceof TomlTable) {
            TomlTable fhirConfigTable = (TomlTable) fhirConfObj;
            fhirServerConfig.setServerName(
                    fhirConfigTable.getString("server_name", () -> ConfigConstants.DEFAULT_FHIR_SERVER_NAME));
            fhirServerConfig.setServerVersion(
                    fhirConfigTable.getString("server_version", () -> ConfigConstants.DEFAULT_FHIR_SERVER_VERSION));
            String serverMetadataPublishedTime = fhirConfigTable.getString("server_metadata_published_time");
            fhirServerConfig.setCapabilityStatementPublishedTime(
                    serverMetadataPublishedTime != null ?
                            CommonUtil.convertTimeToLong(serverMetadataPublishedTime) : CommonUtil.getServerStartupTime()
            );
        }
        return fhirServerConfig;
    }


    private ThrottlingConfig buildThrottlingConfig() {
        ThrottlingConfig throttlingConfig = new ThrottlingConfig();

        Object apimThrottleConfigObj = config.get("healthcare.apim.throttling");
        if (apimThrottleConfigObj instanceof TomlTable) {
            TomlTable apimThrottleConfigTable = (TomlTable) apimThrottleConfigObj;
            throttlingConfig.setDisableStandardProductThrottlePolicies(
                    apimThrottleConfigTable.getBoolean("disable_standard_product_throttle_policies", () -> false));
        }

        Object appThrottleConfigObj = config.get("healthcare.apim.throttling.tier.application");
        if (appThrottleConfigObj instanceof TomlArray) {
            TomlArray appThrottleConfig = (TomlArray) appThrottleConfigObj;
            List<Object> entryList = appThrottleConfig.toList();
            for (Object entry : entryList) {
                if (entry instanceof TomlTable) {
                    TomlTable appThrottlePolicy = (TomlTable) entry;
                    String name = appThrottlePolicy.getString("name");
                    if (name == null) continue;
                    ApplicationPolicy applicationPolicy = new ApplicationPolicy(name);
                    applicationPolicy.setDisplayName(appThrottlePolicy.getString("display_name"));
                    applicationPolicy.setDescription(appThrottlePolicy.getString("description"));

                    QuotaPolicy defaultQuotaPolicy = new QuotaPolicy();
                    String type = appThrottlePolicy.getString("policy.type", () -> PolicyConstants.REQUEST_COUNT_TYPE);
                    if (PolicyConstants.REQUEST_COUNT_TYPE.equals(type)) {
                        RequestCountLimit requestCountLimit = new RequestCountLimit();
                        requestCountLimit.setRequestCount(
                                Long.parseLong(appThrottlePolicy.getString("policy.limit.request_count", () -> "10")));
                        requestCountLimit.setUnitTime(
                                Integer.parseInt(appThrottlePolicy.getString("policy.limit.unit_time", () -> "1")));
                        requestCountLimit.setTimeUnit(
                                appThrottlePolicy.getString("policy.limit.time_unit", () -> APIConstants.TIME_UNIT_MINUTE));
                        defaultQuotaPolicy.setType(PolicyConstants.REQUEST_COUNT_TYPE);
                        defaultQuotaPolicy.setLimit(requestCountLimit);
                    } else {
                        LOG.warn("Application Policy type : " + type + " of application policy : " + name + " is not supported");
                        continue;
                    }
                    applicationPolicy.setDefaultQuotaPolicy(defaultQuotaPolicy);
                    throttlingConfig.addApplicationPolicy(applicationPolicy);
                }
            }
        }
        return throttlingConfig;
    }

    private APIHubConfig buildAPIHubConfig() {
        APIHubConfig apiHubConfig = new APIHubConfig();
        apiHubConfig.setCacheConfig(buildCacheConfig(APIHubConfig.HEALTHCARE_APIHUB_CACHE, "healthcare.apihub.cache"));
        return apiHubConfig;
    }

    private WorkflowConfig buildWorkflowConfig(String configTable) throws OpenHealthcareException {
        WorkflowConfig workflowConfig = null;
        Object wfConfigObj = config.get(configTable);
        if (wfConfigObj instanceof TomlTable) {
            TomlTable wfConfigTable = (TomlTable) wfConfigObj;
            String type = wfConfigTable.getString("type", () -> ConfigConstants.WF_TYPE_DEFAULT);
            if (type != null) {
                if (!(StringUtils.equalsIgnoreCase(ConfigConstants.WF_TYPE_DEFAULT, type) ||
                        StringUtils.equalsIgnoreCase(ConfigConstants.WF_TYPE_BPMN, type))) {
                    throw new OpenHealthcareException(
                            "Unknown workflow type \"" + type + "\" for workflow config : " + configTable);
                }
                if (StringUtils.equalsIgnoreCase(type, ConfigConstants.WF_TYPE_BPMN)) {
                    String processDefinitionKey = wfConfigTable.getString("process_definition_key");
                    String serviceUrl = wfConfigTable.getString("service_url");
                    String username = wfConfigTable.getString("username");
                    String password = wfConfigTable.getString("password");
                    if (StringUtils.isEmpty(processDefinitionKey) || StringUtils.isEmpty(serviceUrl) ||
                            StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
                        throw new OpenHealthcareException(
                                "Mandatory parameter (when type = \"BPMN\") in " + configTable + " config is missing." +
                                        " Mandatory parameters : process_definition_key, service_url, username, password");
                    }
                    workflowConfig =
                            new WorkflowConfig(type, processDefinitionKey, serviceUrl, username, resolveSecret(password));
                }
            }
        }
        if (workflowConfig == null) {
            // workflow for sign-up is not configured
            LOG.info("Workflow configuration not found, hence creating default workflow configuration for : " + configTable);
            workflowConfig = new WorkflowConfig(ConfigConstants.WF_TYPE_DEFAULT, null, null, null, null);
        }
        return workflowConfig;
    }

    private CacheConfig buildCacheConfig(String name, String configTable) {
        CacheConfig cacheConfig = new CacheConfig(name);
        Object cacheConfigObj = config.get(configTable);
        if (cacheConfigObj instanceof TomlTable) {
            TomlTable cacheConfigTable = (TomlTable) cacheConfigObj;
            cacheConfig.setEnable(cacheConfigTable.getBoolean("enable", () -> false));
            cacheConfig.setExpiry(cacheConfigTable.getLong("expiry", () -> 300));
            long capacity = cacheConfigTable.getLong("capacity", () -> 10);
            cacheConfig.setCapacity((int) capacity);
        }
        return cacheConfig;
    }

    private ClaimMgtConfig buildClaimMgtConfig() {

        Object claimConfObj = config.get("healthcare.identity.claims");
        ClaimMgtConfig claimMgtConfig = new ClaimMgtConfig();
        if (claimConfObj instanceof TomlTable) {
            TomlTable claimConfigTable = (TomlTable) claimConfObj;
            claimMgtConfig.setClaimUri(claimConfigTable
                    .getString("patient_id_claim_uri", () -> ConfigConstants.DEFAULT_PATIENT_ID_CLAIM_URI));
            claimMgtConfig.setPatientIdKey(
                    claimConfigTable.getString("patient_id_key", () -> ConfigConstants.DEFAULT_PATIENT_ID_KEY));
            claimMgtConfig.setFhirUserClaimContext(claimConfigTable
                    .getString("fhirUser_resource_url_context", () -> ConfigConstants.DEFAULT_FHIRUSER_CLAIM_CONTEXT));
            claimMgtConfig.setFhirUserMappedLocalClaim(claimConfigTable.getString("fhirUser_resource_id_claim_uri",
                    () -> ConfigConstants.DEFAULT_FHIRUSER_MAPPED_LOCAL_CLAIM));
        }
        Object claimMgtTable = config.get("healthcare.identity.claim.mgt");
        if (claimMgtTable instanceof TomlTable) {
            TomlTable claimMgtConfigTable = (TomlTable) claimMgtTable;
            claimMgtConfig.setEnable(claimMgtConfigTable.getBoolean("enable", () -> false));
        }
        return claimMgtConfig;
    }

    private AuthConfig buildAuthConfig() throws OpenHealthcareException {

        AuthConfig authConfig = new AuthConfig();
        Object authConfigObj = config.get("healthcare.deployment.webapps.authenticationendpoint");
        if (authConfigObj instanceof TomlTable) {
            TomlTable authConfigTable = (TomlTable) authConfigObj;
            authConfig.setSelSignupEnable(authConfigTable.getBoolean("enable_selfsignup",  () -> true));
            authConfig.setSelfSignupURL(
                authConfigTable.getString("selfsignup_url", () -> ConfigConstants.DEFAULT_AUTH_SELFSIGNUP_URL));
            authConfig.setSignInDisclaimerPortalName(
                    authConfigTable.getString("signin_disclaimer_portal_name", () -> ConfigConstants.DEFAULT_WEBAPP_TITLE));
        }
        return authConfig;
    }

    private RecoveryConfig buildRecoveryConfig() throws OpenHealthcareException {
        RecoveryConfig recoveryConfig = new RecoveryConfig();
        Object recoveryConfigObj = config.get("healthcare.deployment.webapps.accountrecovery");
        if (recoveryConfigObj instanceof TomlTable) {
            TomlTable recoveryConfigTable = (TomlTable) recoveryConfigObj;
            recoveryConfig.setUsernameInfoEnable(recoveryConfigTable.getBoolean("signup_flow.username_info_enable", () -> false));
            recoveryConfig.setSignUpSuccessMessage(
                    recoveryConfigTable.getString("signup_flow.success_message", () -> ConfigConstants.DEFAULT_WEBAPP_SIGN_UP_SUCCESS_MSG));
        }
        return recoveryConfig;
    }

    private WebApplicationConfig buildWebApplicationConfig() throws OpenHealthcareException {
        WebApplicationConfig webApplicationConfig = new WebApplicationConfig();
        Object webAppConfigObj = config.get("healthcare.deployment.webapps");
        if (webAppConfigObj instanceof TomlTable) {
            TomlTable webAppConfigTable = (TomlTable) webAppConfigObj;
            webApplicationConfig.setDeploymentName(
                webAppConfigTable.getString("name", () -> ConfigConstants.DEFAULT_DEPLOYMENT_NAME));
            webApplicationConfig.setDeploymentNameContainerCSS(
                    webAppConfigTable.getString("name_container_css", () -> ConfigConstants.DEFAULT_DEPLOYMENT_NAME_CONTAINER_CSS));
            webApplicationConfig.setTermsOfUse(
                webAppConfigTable.getString("terms_of_use", () -> ConfigConstants.DEFAULT_WEBAPP_TERMS_OF_USE));
            webApplicationConfig.setPrivacyPolicy(
                webAppConfigTable.getString("privacy_policy", () -> ConfigConstants.DEFAULT_WEBAPP_PRIVACY_POLICY));
            webApplicationConfig.setCookiePolicy(
                webAppConfigTable.getString("cookie_policy", () -> ConfigConstants.DEFAULT_WEBAPP_COOKIE_POLICY));
            webApplicationConfig.setLogo(
                webAppConfigTable.getString("logo", () -> ConfigConstants.DEFAULT_WEBAPP_LOGO));
            webApplicationConfig.setLogoHeight(
                    webAppConfigTable.getString("logoHeight", () -> ConfigConstants.DEFAULT_WEBAPP_LOGO_HEIGHT));
            webApplicationConfig.setLogoWidth(
                    webAppConfigTable.getString("logoWidth", () -> ConfigConstants.DEFAULT_WEBAPP_LOGO_WIDTH));
            webApplicationConfig.setLogoContainerCSS(
                    webAppConfigTable.getString("logo_container_css", () -> ConfigConstants.DEFAULT_WEBAPP_LOGO_CONTAINER_CSS));
            webApplicationConfig.setFavicon(
                    webAppConfigTable.getString("favicon", () -> ConfigConstants.DEFAULT_WEBAPP_FAVICON));
            webApplicationConfig.setTitle(
                    webAppConfigTable.getString("title", () -> ConfigConstants.DEFAULT_WEBAPP_TITLE));
            webApplicationConfig.setFooter(
                    webAppConfigTable.getString("footer", () -> ConfigConstants.DEFAULT_WEBAPP_FOOTER));
            webApplicationConfig.setFooterSecondaryHTML(
                    webAppConfigTable.getString("footer_secondary_html", () -> ConfigConstants.DEFAULT_WEBAPP_FOOTER_SECONDARY_HTML));
            webApplicationConfig.setMainColor(
                webAppConfigTable.getString("main_color", () -> ConfigConstants.DEFAULT_WEBAPP_MAIN_COLOR));
        }
        webApplicationConfig.setAuthConfig(buildAuthConfig());
        webApplicationConfig.setRecoveryConfig(buildRecoveryConfig());

        return webApplicationConfig;
    }

    private Map<String, MailNotificationConfig> buildNotificationMailConfig() throws OpenHealthcareException {
        Map<String, MailNotificationConfig> notificationMailConfig = new HashMap<>();
        Object mailConfigObject = config.get("healthcare.notification.mail");
        if (mailConfigObject instanceof TomlArray) {
            TomlArray mailConfig = (TomlArray) mailConfigObject;
            List<Object> notifications = mailConfig.toList();
            for (Object notification : notifications) {
                if (notification instanceof TomlTable) {
                    TomlTable notificationConfig = (TomlTable) notification;
                    MailNotificationConfig config = new MailNotificationConfig();
                    if (StringUtils.isEmpty(notificationConfig.getString("name")) ||
                            StringUtils.isEmpty(notificationConfig.getBoolean("enable").toString()) ||
                            StringUtils.isEmpty(notificationConfig.getString("email_subject")) ||
                            StringUtils.isEmpty(notificationConfig.getString("email_body"))) {
                        throw new OpenHealthcareException("One or more mandatory parameter/s in the notification " +
                                "config missing. [Mandatory params - name, enable, email_subject, email_body]");
                    }
                    config.setName(notificationConfig.getString("name"));
                    config.setEnable(notificationConfig.getBoolean("enable"));
                    config.setRecipients(notificationConfig.getString("recipients"));
                    config.setRecipientRoles(notificationConfig.getString("recipient_roles"));
                    config.setEmailSubject(notificationConfig.getString("email_subject"));
                    config.setEmailBody(notificationConfig.getString("email_body"));
                    notificationMailConfig.put(config.getName(), config);
                }
            }
        }
        return notificationMailConfig;
    }

    private OrganizationConfig buildOrganizationconfig() {
        OrganizationConfig organizationConfig = new OrganizationConfig();
        Object orgObj = config.get("healthcare.organization");
        if (orgObj instanceof TomlTable) {
            TomlTable orgTable = (TomlTable) orgObj;
            organizationConfig.setOrgName(orgTable.getString("org_name"));
            organizationConfig.setContactEmail(orgTable.getString("contact_email"));
            String zoneStr = orgTable.getString("timezone", () -> "GMT");
            if (!ZoneId.getAvailableZoneIds().contains(zoneStr)) {
                LOG.warn("Unsupported timezone - " + zoneStr + ". Defaulting to GMT");
                zoneStr = "GMT";
            }
            organizationConfig.setZoneId(ZoneId.of(zoneStr));
        }
        return organizationConfig;
    }

    private ScopeMgtConfig buildScopeMgtConfig() {

        ScopeMgtConfig scopeMgtConfig = new ScopeMgtConfig();
        Object scopeAndRoleObject = config.get("healthcare.identity.scopemgt");
        if (scopeAndRoleObject instanceof TomlTable) {
            TomlTable fhirConfigTable = (TomlTable) scopeAndRoleObject;
            TomlArray rolesArr = fhirConfigTable.getArray("roles");
            //convert .toml roles arr to a java list
            if (rolesArr != null) {
                List<String> rolesList = new ArrayList<>();
                for (int i = 0; i < rolesArr.size(); i++) {
                    rolesList.add(rolesArr.getString(i));
                }
                scopeMgtConfig.setRoles(rolesList);
            }
            Boolean fhirScopeTOWso2ScopeMapping = fhirConfigTable.getBoolean("enable_fhir_scope_to_wso2_scope_mapping");
            scopeMgtConfig.setEnableFHIRScopeToWSO2ScopeMapping(
                    fhirScopeTOWso2ScopeMapping != null ? fhirScopeTOWso2ScopeMapping : false
            );
        }
        Object scopeObject = config.get("healthcare.identity.scopemgt.shared_scopes");
        if (scopeObject instanceof TomlArray) {
            TomlArray scopeArray = (TomlArray) scopeObject;
            List<Object> scopeList = scopeArray.toList();
            for (Object entry : scopeList) {
                if (entry instanceof TomlTable) {
                    TomlTable scopeDetails = (TomlTable) entry;
                    Scope scope = new Scope();
                    scope.setKey(scopeDetails.getString("key"));
                    scope.setName(scopeDetails.getString("name"));
                    scope.setRoles(scopeDetails.getString("roles"));
                    scope.setDescription(scopeDetails.getString("description"));
                    scopeMgtConfig.addScope(ScopeMgtUtil.formatScope(scope, scopeMgtConfig.isFHIRScopeToWSO2ScopeMappingEnabled()));
                }
            }
        }
        return scopeMgtConfig;

    }

    private Map<String, BackendAuthConfig> buildBackendAuthConfig() throws OpenHealthcareException {

        Map<String, BackendAuthConfig> backendAuthConfigs = new HashMap<>();
        Object backendAuthConfigObject = config.get("healthcare.backend.auth");
        if (backendAuthConfigObject instanceof TomlArray) {
            TomlArray authConfig = (TomlArray) backendAuthConfigObject;
            List<Object> authConfigList = authConfig.toList();
            for (Object notification : authConfigList) {
                if (notification instanceof TomlTable) {
                    TomlTable beAuthTable = (TomlTable) notification;
                    BackendAuthConfig backendAuthConfig = new BackendAuthConfig();
                    if (StringUtils.isEmpty(beAuthTable.getString("name")) ||
                            StringUtils.isEmpty(beAuthTable.getString("token_endpoint"))) {
                        throw new OpenHealthcareException("One or more mandatory parameter/s in the notification " +
                                "config missing. [Mandatory params - name, token_endpoint]");
                    }
                    backendAuthConfig.setName(beAuthTable.getString("name"));
                    backendAuthConfig.setAuthEndpoint(beAuthTable.getString("token_endpoint"));
                    backendAuthConfig.setClientId(beAuthTable.getString("client_id"));
                    String clientId = beAuthTable.getString("client_id", () -> null);
                    if (clientId != null) {
                        backendAuthConfig.setClientId(clientId);
                    }
                    String keyAlias = beAuthTable.getString("private_key_alias", () -> null);
                    if (keyAlias != null) {
                        backendAuthConfig.setPrivateKeyAlias(keyAlias);
                    }
                    String clientSecret = beAuthTable.getString("client_secret", () -> null);
                    if (clientSecret != null) {
                        backendAuthConfig.setClientSecret(resolveSecret(clientSecret));
                    }
                    backendAuthConfig.setAuthType(beAuthTable.getString("auth_type"));
                    backendAuthConfigs.put(backendAuthConfig.getName(), backendAuthConfig);
                }
            }
        }
        return backendAuthConfigs;
    }

    private SmartConfig buildSmartConfig() {
        LOG.debug("Building SMART configuration");
        SmartConfig smartConfig = new SmartConfig();
        Object smartConfObj = config.get("healthcare.smartconfig");
        if (smartConfObj instanceof TomlTable) {
            TomlTable smartConfigTable = (TomlTable) smartConfObj;

            // Auth methods
            TomlArray authMethodsArr = smartConfigTable.getArray("auth_methods");
            if (authMethodsArr != null) {
                List<String> authMethodsList = new ArrayList<>();
                for (int i = 0; i < authMethodsArr.size(); i++) {
                    authMethodsList.add(authMethodsArr.getString(i));
                }
                smartConfig.setAuthMethods(authMethodsList);
            }

            // Grant types supported
            TomlArray grantTypesSupportedArr = smartConfigTable.getArray("grant_types_supported");
            if (grantTypesSupportedArr != null) {
                List<String> grantTypesSupportedList = new ArrayList<>();
                for (int i = 0; i < grantTypesSupportedArr.size(); i++) {
                    grantTypesSupportedList.add(grantTypesSupportedArr.getString(i));
                }
                smartConfig.setGrantTypesSupported(grantTypesSupportedList);
            }

            // Scopes supported
            TomlArray scopesSupportedArr = smartConfigTable.getArray("scopes_supported");
            if (scopesSupportedArr != null) {
                List<String> scopesSupportedList = new ArrayList<>();
                for (int i = 0; i < scopesSupportedArr.size(); i++) {
                    scopesSupportedList.add(scopesSupportedArr.getString(i));
                }
                smartConfig.setScopesSupported(scopesSupportedList);
            }

            // Response types
            TomlArray responseTypesArr = smartConfigTable.getArray("response_types");
            if (responseTypesArr != null) {
                List<String> responseTypesList = new ArrayList<>();
                for (int i = 0; i < responseTypesArr.size(); i++) {
                    responseTypesList.add(responseTypesArr.getString(i));
                }
                smartConfig.setResponseTypes(responseTypesList);
            }

            // Capabilities
            TomlArray capabilitiesArr = smartConfigTable.getArray("capabilities");
            if (capabilitiesArr != null) {
                List<String> capabilitiesList = new ArrayList<>();
                for (int i = 0; i < capabilitiesArr.size(); i++) {
                    capabilitiesList.add(capabilitiesArr.getString(i));
                }
                smartConfig.setCapabilities(capabilitiesList);
            }
        }
        LOG.info("SMART configuration loaded successfully");
        return smartConfig;
    }

    private char[] resolveSecret(String secret) {
        String protectedToken = MiscellaneousUtil.getProtectedToken(secret);
        if (protectedToken != null) {
            secretResolver.addProtectedToken(protectedToken);
        }
        return MiscellaneousUtil.resolve(secret, secretResolver).toCharArray();
    }
}
