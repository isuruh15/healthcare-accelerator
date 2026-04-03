# WSO2 Healthcare IS - Token Management Component

This component provides custom OAuth2 grant handlers for WSO2 Identity Server to support SMART on FHIR authorization flows.

## Overview

This module has been migrated from `org.wso2.healthcare.apim.tokenmgt` to be compatible with **WSO2 IS 7.2.0**.

### Key Changes from APIM Version

1. **Removed Dependencies**: No longer depends on APIM-specific components like:
   - `org.wso2.healthcare.apim.core`
   - `org.wso2.healthcare.apim.claim.mgt`

2. **IS-Compatible Implementation**: Uses IS 7.2.0 compatible APIs and patterns:
   - Uses `UserClaimResolver` from `org.wso2.healthcare.is.smart.auth` component
   - Utilizes `AuthenticatedUser` instead of username/tenant domain pairs
   - Follows IS 7.2.0 best practices for token response handling

3. **Configuration**: Simplified configuration approach using system properties or deployment.toml

## Components

### HealthcareAuthorizationCodeGrantHandler

Custom authorization code grant handler that adds patient context to OAuth2 token responses according to SMART on FHIR specifications.

**Features:**
- Detects `launch/patient` scope in token requests
- Retrieves patient ID from user claims
- Adds patient parameter to token response
- Compatible with WSO2 IS 7.2.0

## Configuration

### 1. Register the Grant Handler

Add the following to your WSO2 IS `deployment.toml`:

```toml
[oauth.grant_type.authorization_code]
grant_handler = "org.wso2.healthcare.is.tokenmgt.handlers.HealthcareAuthorizationCodeGrantHandler"
```

### 2. Configure Patient Claim URI (Optional)

By default, the handler uses `http://wso2.org/claims/patientid` as the claim URI for patient ID.

To customize, set the system property:

```toml
[oauth.grant_type.authorization_code]
patient_claim_uri = "http://wso2.org/claims/custom-patient-id"
```

Alternatively, set it as a system property in `startup.sh` or `startup.bat`:

```bash
-DOAuth.GrantType.AuthorizationCode.PatientClaimUri=http://wso2.org/claims/custom-patient-id
```

### 3. Configure User Claims

Ensure the patient claim is mapped to users in the user store:

1. Navigate to **Main > Identity > Claims > List**
2. Select `http://wso2.org/claims`
3. Add or edit the `patientid` claim
4. Map it to the appropriate user store attribute

### 4. Configure Allowed Scopes

Ensure SMART on FHIR scopes are allowed:

```toml
[oauth]
allowed_scopes = ["openid", "fhirUser", "launch/patient", "patient/*.read"]
```

## SMART on FHIR Specification

This implementation follows the SMART App Launch specification:
- [Requesting Context with Scopes](http://www.hl7.org/fhir/smart-app-launch/scopes-and-launch-context/index.html#requesting-context-with-scopes)
- [Patient-specific Scopes](http://www.hl7.org/fhir/smart-app-launch/scopes-and-launch-context/index.html#patient-specific-scopes)

### Supported Scopes

- `launch/patient` - Request patient launch context
- `patient/*.read` - Read access to patient-specific resources

### Token Response Format

When `launch/patient` scope is requested, the token response includes:

```json
{
  "access_token": "...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "patient": "Patient/123",
  "scope": "launch/patient patient/*.read"
}
```

## Dependencies

This component depends on:
- `org.wso2.healthcare.is.smart.auth` - For user claim resolution
- WSO2 IS 7.2.0 OAuth2 components
- Carbon user core and identity framework

## Building

```bash
mvn clean install
```

## Deployment

1. Build the component
2. Copy the JAR to `<IS_HOME>/repository/components/dropins/`
3. Configure as described above
4. Restart WSO2 IS

## Migration Notes

If migrating from the APIM version:

1. **Configuration Changes**: Update deployment.toml with IS-specific configuration
2. **Claim Management**: Set up user claims in IS instead of APIM
3. **Testing**: Verify OAuth2 flows work correctly with the new handler
4. **Scope Configuration**: Ensure all SMART on FHIR scopes are properly configured

## License

Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.

Licensed under the Apache License, Version 2.0.
