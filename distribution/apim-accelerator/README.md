# WSO2 Healthcare API Manager Accelerator - v${project.version}

The WSO2 Healthcare API Manager Accelerator provides features tailored for healthcare API management, particularly focusing on FHIR (Fast Healthcare Interoperability Resources), SMART on FHIR, and OAuth 2.0 support. 

## Features Overview

### FHIR Capability Statement Auto-Generation
  The FHIR metadata endpoint auto-generates a **CapabilityStatement** that describes the FHIR server's functionality. This endpoint, `https://<API_GW_HOST>:<PORT>/r4/metadata`, provides an essential overview of supported FHIR operations, resource types, and interactions, helping developers and integrators understand the server’s FHIR capabilities.

### OAuth 2.0 Discovery via Well-Known Endpoint
  The well-known endpoint (`https://<API_GW_HOST>:<PORT>/r4/.well-known/smart-configuration`) allows OAuth 2.0 clients to easily discover authentication and token endpoints as well as supported scopes and claims. This simplifies the setup for applications integrating with OAuth 2.0 and OpenID Connect for healthcare data security.

### SMART on FHIR Integration
  SMART on FHIR support enables the integration of applications that use the SMART on FHIR framework. This includes capabilities like SMART scopes registration, SMART App Launch flow features etc.

## Prerequisites:
1. Download WSO2 API Manager product from [WSO2 API Manager](https://apim.docs.wso2.com/en/latest/). 
2. Download the WSO2 Healthcare API Manager Accelerator from [Releases](https://github.com/wso2/healthcare-accelerator/releases) section of the accelerator repository.


## Installation Steps:
1. Extract WSO2 APIM product and update the product. Let's call the installed location `<WSO2_APIM_HOME>`.
2. Extract WSO2 Healthcare APIM Accelerator to `<WSO2_APIM_HOME>`. Let's call it `<WSO2_HC_APIM_ACC_HOME>`.
3. [Optional] Check the accelerator configurations in `<WSO2_HC_APIM_ACC_HOME>/conf/config.toml` file to enable or disable features.
4. Navigate to `<WSO2_HC_APIM_ACC_HOME>` directory and execute `./bin/merge.sh` command. This will copy the artifacts to the WSO2 APIM and add the required configurations.

*Note: If you are using a [Distributed Deployment of WSO2 APIM](https://apim.docs.wso2.com/en/4.3.0/install-and-setup/setup/distributed-deployment/deploying-wso2-api-m-in-a-distributed-setup/), 
you need to run the `merge.sh` script in all the nodes with the respective product profile. For more info, refer [Installing the Accelerator in a Distributed Deployment of WSO2 APIM](#installing-the-accelerator-in-a-distributed-deployment-of-wso2-apim).*

5. Navigate to `<WSO2_APIM_HOME>` directory and execute `./bin/api-manager.sh` to start the APIM server with WSO2 Healthcare Accelerator.

## Audit Logs:
Running `merge.sh` script creates a audit log folder in the product home. Structure of it looks like below;

```sh
hc-accelerator
├── backup
│   ├── conf
│   ├── jaggeryapps
│   └── webapps
└── merge_audit.log
```
- `merge_audit.log` will have an audit line per execution of the `merge.sh` script of the accelerator. Each line contains execution date and time, user account and the version of the accelerator. Example log line is below;
```sh
Mon May 31 22:01:55 +0530 2021 - john - WSO2 Healthcare API Manager Accelerator - v${project.version}
```
- `backup` folder contains the files that were originally there in the APIM product before running the accelerator. Please note that only the last state will be there. 

## U2 Update Tool
By default, U2 update related files are added in the build.
```sh
hc-accelerator
├── LICENSE.txt
│   ├── bin
│   ├── updates
```

## Installing the Accelerator in a Distributed Deployment of WSO2 APIM.
WSO2 Healthcare API Manager Accelerator can be installed in a distributed deployment of WSO2 API Manager. 
The same profile parameters used in the distributed deployment must be applied when running the `merge.sh` script.

Note: Make sure to run the `merge.sh` script **after** performing the profile optimization steps mentioned in the [Distributed Deployment documentation](https://apim.docs.wso2.com/en/4.3.0/install-and-setup/setup/distributed-deployment/product-profiles/#running-the-api-m-profiles).

Example: If you have a distributed deployment with Gateway, Control Plane profiles, you need to run the `merge.sh` script in both nodes with the respective profile parameters as below.

**Gateway Node**
```sh
./<WSO2_HC_APIM_ACC_HOME>/bin/merge.sh -Dprofile=gateway-worker
```

**Control Plane Node**
```sh
./<WSO2_HC_APIM_ACC_HOME>/bin/merge.sh -Dprofile=control-plane
```

## Configuration Catalog

All Open-Healthcare related configurations are located in the `<WSO2_APIM_HOME>/repository/conf/deployment.toml` file under the `[healthcare]` namespace.

### Core Healthcare Configurations

| Configuration Section | Parameter | Type | Description | Default Value |
|----------------------|-----------|------|-------------|---------------|
| **healthcare.fhir** | `server_name` | String | Name of the FHIR server | `WSO2 Open Healthcare` |
| | `server_version` | String | Version of the FHIR server | `1.2.0` |
| | `server_metadata_published_time` | String | CapabilityStatement publication time (format: `yyyy-MM-dd'T'HH:mm:ss.SSS'Z`) | Server start time |
| **healthcare.apihub.cache** | `enable` | Boolean | Enable/disable API Hub cache | `false` |
| | `expiry` | Integer | Cache expiry time in seconds | `300` |
| | `capacity` | Integer | Maximum cache capacity | `10` |
| **healthcare.organization** | `org_name` | String | Organization name for notifications | - |
| | `contact_email` | String | Organization contact email | - |
| | `timezone` | String | Organization timezone | `GMT` |

### Backend Authentication

| Configuration Section | Parameter | Type | Description | Example Value |
|----------------------|-----------|------|-------------|---------------|
| **healthcare.backend.auth** | `name` | String | Authentication configuration name | `epic_pkjwt` |
| | `auth_type` | String | Authentication type (`pkjwt` or `client_credentials`) | `pkjwt` |
| | `token_endpoint` | String | External OAuth token endpoint URL | `https://localhost:9443/oauth2/token` |
| | `client_id` | String | OAuth client ID | - |
| | `private_key_alias` | String | Private key alias (for PKJWT) | - |
| | `client_secret` | String | Client secret (for client_credentials) | - |

### SMART on FHIR Configuration

| Configuration Section | Parameter | Type | Description | Example Value |
|----------------------|-----------|------|-------------|---------------|
| **healthcare.smartconfig** | `auth_methods` | Array[String] | Supported authentication methods | `["client_secret_basic", "client_secret_post", "private_key_jwt"]` |
| | `grant_types_supported` | Array[String] | OAuth grant types supported | `["authorization_code", "refresh_token", "client_credentials"]` |
| | `scopes_supported` | Array[String] | SMART/FHIR scopes supported | `["openid", "fhirUser", "launch", "patient/*.read"]` |
| | `response_types` | Array[String] | OAuth response types supported | `["code", "token", "id_token"]` |
| | `capabilities` | Array[String] | SMART on FHIR capabilities | `["launch-ehr", "client-public", "sso-openid-connect"]` |
| **healthcare.identity.claims** | `patient_id_claim_uri` | String | Patient ID claim URI | `http://wso2.org/claims/patientId` |
| | `patient_id_key` | String | Patient ID key in JWT | `patientId` |
| | `fhirUser_resource_url_context` | String | FHIR resource context for fhirUser claim | `/r4/Patient` |
| | `fhirUser_resource_id_claim_uri` | String | Claim URI for FHIR resource ID | `http://wso2.org/claims/patientId` |
| **healthcare.identity.scopemgt** | `roles` | Array[String] | Roles for FHIR scope mapping | `["patient-read", "patient-write", "user-read", "user-write"]` |
| | `enable_fhir_scope_to_wso2_scope_mapping` | Boolean | Enable FHIR to WSO2 scope mapping | `true` |
| **healthcare.identity.scopemgt.shared_scopes** | `key` | String | Scope key (e.g., `patient/*.r`) | - |
| | `name` | String | Scope display name | - |
| | `roles` | String | Comma-separated roles | - |
| | `description` | String | Scope description | - |
| **healthcare.identity.claim.mgt** | `enable` | Boolean | Enable custom claim management | `false` |

### Web Application Customization

| Configuration Section | Parameter | Type | Description | Default Value |
|----------------------|-----------|------|-------------|---------------|
| **healthcare.deployment.webapps** | `name` | String | Application name | `Open Healthcare` |
| | `terms_of_use` | String | Terms of use URL | - |
| | `privacy_policy` | String | Privacy policy URL | - |
| | `cookie_policy` | String | Cookie policy URL | - |
| | `logo` | String | Logo image path | `images/wso2-logo.svg` |
| | `title` | String | Page title | `WSO2 Open Healthcare` |
| | `footer` | String | Footer text | `WSO2 Open Healthcare` |
| | `main_color` | String | Primary theme color | `#342382` |
| **healthcare.deployment.webapps.authenticationendpoint** | `enable_selfsignup` | Boolean | Enable self-registration | `true` |
| | `selfsignup_url` | String | Custom self-registration URL | - |
| | `signin_disclaimer_portal_name` | String | Portal name for sign-in disclaimer | `WSO2 Open Healthcare` |
| **healthcare.deployment.webapps.accountrecovery** | `signup_flow.username_info_enable` | Boolean | Show username info in signup flow | `false` |
| | `signup_flow.success_message` | String | Success message after signup | `User registration completed successfully` |


**Note:** These datasources are only required when `healthcare.saas.account.enable = true`.

### API Manager Configurations (Added by merge.sh)

The following configurations are automatically added to `deployment.toml` when running the `merge.sh` script:

| Configuration Section | Parameter | Type | Description | Default Value |
|----------------------|-----------|------|-------------|---------------|
| **apim.jwt** | `enable` | Boolean | Enable JWT generation | `true` |
| | `encoding` | String | JWT encoding type | `base64` |
| | `claim_dialect` | String | Claim dialect URI | `http://wso2.org/claims` |
| | `convert_dialect` | Boolean | Convert claim dialect | `false` |
| | `header` | String | JWT header name | `X-JWT-Assertion` |
| | `signing_algorithm` | String | JWT signing algorithm | `SHA256withRSA` |
| | `enable_user_claims` | Boolean | Include user claims in JWT | `true` |
| | `claims_extractor_impl` | String | Claims extractor implementation | `org.wso2.carbon.apimgt.impl.token.ExtendedDefaultClaimsRetriever` |
| **apim.jwt.gateway_generator** | `impl` | String | Gateway JWT generator implementation | `org.wso2.healthcare.apim.gateway.security.jwt.generator.HealthcareGatewayJWTGenerator` |
| **apim.oauth_config** | `allowed_scopes` | Array[String] | Allowed OAuth scopes (regex patterns) | `["^device_.*", "openid", "fhirUser", "launch/patient", "offline_access"]` |
| **oauth** | `show_display_name_in_consent_page` | Boolean | Show display name in OAuth consent page | `true` |
| **oauth.grant_type.authorization_code** | `grant_handler` | String | Authorization code grant handler | `org.wso2.healthcare.apim.tokenmgt.handlers.OpenHealthcareExtendedAuthorizationCodeGrantHandler` |
| **oauth.custom_response_type** | `name` | String | Response type name | `code` |
| | `class` | String | Response type handler class | `org.wso2.healthcare.apim.scopemgt.handlers.OpenHealthcareExtendedCodeResponseTypeHandler` |
| **apim.key_manager** | `key_validation_handler_impl` | String | Key validation handler implementation | `org.wso2.healthcare.apim.scopemgt.handlers.OpenHealthcareExtendedKeyValidationHandler` |
| **apim.sync_runtime_artifacts.gateway.skip_list** | `apis` | Array[String] | APIs to skip during artifact sync | `["_MetadataAPI_.xml", "_WellKnownResourceAPI_.xml"]` |
| **event_listener** (Private Key JWT) | `id` | String | Event listener ID | `private_key_jwt_authenticator` |
| | `type` | String | Handler type | `org.wso2.carbon.identity.core.handler.AbstractIdentityHandler` |
| | `name` | String | Handler implementation | `org.wso2.healthcare.apim.clientauth.jwt.PrivateKeyJWTClientAuthenticator` |
| | `order` | Integer | Execution order | `899` |
| | `properties.max_allowed_jwt_lifetime_seconds` | Integer | Maximum JWT lifetime in seconds | `300` |
| | `properties.token_endpoint_alias` | String | Token endpoint alias | - |
| **cache.manager** (Identity Cache) | `name` | String | Cache manager name | `IdentityApplicationManagementCacheManager` |
| | `timeout` | Integer | Cache timeout in minutes | `10` |
| | `capacity` | Integer | Cache capacity | `5000` |
| **custom_message_formatters** | `class` | String | Message formatter class | `org.apache.axis2.transport.http.ApplicationXMLFormatter` or `org.apache.synapse.commons.json.JsonStreamFormatter` |
| | `content_type` | String | Content type | `application/fhir+xml` or `application/fhir+json` |
| **custom_message_builders** | `class` | String | Message builder class | `org.apache.axis2.builder.ApplicationXMLBuilder` or `org.apache.synapse.commons.json.JsonStreamBuilder` |
| | `content_type` | String | Content type | `application/fhir+xml` or `application/fhir+json` |

### Notification Configuration

| Configuration Section | Parameter | Type | Description | Example Value |
|----------------------|-----------|------|-------------|---------------|
| **healthcare.notification.mail** | `name` | String | Notification template name | `new_user_signup_requested_internal` |
| | `enable` | Boolean | Enable this notification | `true` |
| | `recipient_roles` | String | Comma-separated recipient roles | `approver,approver2` |
| | `recipients` | String | Comma-separated recipient emails | `user1@test.com,user2@test.com` |
| | `email_subject` | String | Email subject template (supports variables) | `[Developer Portal] New User Signup - ${first_name} ${last_name}` |
| | `email_body` | String | Email body template (HTML supported, supports variables) | HTML content with variables like `${time}`, `${date}`, `${status}` |

**Available Notification Templates:**
- `new_user_signup_requested_internal` - Notifies approvers when a new user signs up
- `new_user_signup_completed_internal` - Notifies approvers when signup is approved/rejected
- `new_user_signup_completed_external` - Notifies user about signup status
- `new_app_creation_requested_internal` - Notifies approvers when a new app is created
- `new_app_creation_completed_internal` - Notifies approvers when app creation is approved/rejected
- `new_app_creation_completed_external` - Notifies user about app creation status

**Supported Variables in Email Templates:**
- `${first_name}`, `${last_name}`, `${user_name}` - User information
- `${time}`, `${date}`, `${timezone}` - Timestamp information
- `${status}` - Approval status (approved/rejected)
- `${approver}` - Approver username
- `${app_name}` - Application name
- `${org_name}` - Organization name (from `healthcare.organization.org_name`)
- `${contact_email}` - Contact email (from `healthcare.organization.contact_email`)
- `${server_url}` - Server URL

### OIDC Scopes (Added to oidc-scope-config.xml)

The following OIDC scopes are automatically configured when running the `merge.sh` script:

| Scope ID | Mapped Claim | Description |
|----------|--------------|-------------|
| `fhirUser` | `patientId` | FHIR user context scope |
| `launch/patient` | `patientId` | Patient launch context scope |
| `offline_access` | `patientId` | Offline access scope for refresh tokens |

### Custom Claims (Added to claim-config.xml)

| Claim URI | Display Name | Attribute ID | Description |
|-----------|--------------|--------------|-------------|
| `http://wso2.org/claims/patientId` | Patient ID | `patientId` | Patient identifier claim |

**Note:** These claims and scopes are essential for SMART on FHIR functionality and are automatically configured during installation.
