# WSO2 Healthcare Identity Server Accelerator - Distribution

This distribution package contains the WSO2 Healthcare Identity Server Accelerator components, which provide healthcare-specific identity and access management capabilities for WSO2 Identity Server and Asgardeo.

## Version

**Version**: 2.0.0

## Contents

This distribution includes the following components:

### Components (`dropins/`)

- **org.wso2.healthcare.is.smart.auth**: SMART on FHIR authentication and authorization component
  - Implements SMART launch context handling
  - Adds patient and practitioner context to token responses
  - Supports `launch/patient` and `launch/practitioner` scopes

### Configuration (`conf/`)

Configuration files and templates for healthcare-specific Identity Server settings.

### Resources (`resources/`)

Additional resources, scripts, and documentation for deploying and configuring the accelerator.

## Installation

### Prerequisites

- WSO2 Identity Server 6.0.0 or later (for on-premise deployment)
- Asgardeo account (for SaaS deployment)
- Java 11 or later

### For WSO2 Identity Server

1. **Stop the Identity Server** (if running):
   ```bash
   cd <IS_HOME>/bin
   ./wso2server.sh stop
   ```

2. **Deploy the components**:
   ```bash
   cp dropins/*.jar <IS_HOME>/repository/components/dropins/
   ```

3. **Configure user claims** (if not already configured):
   - Log in to the IS Management Console
   - Navigate to **Main > Identity > Claims > Add**
   - Add the following claims:
     - Claim URI: `http://wso2.org/claims/patient`
     - Display Name: `Patient ID`
     - Description: `FHIR Patient Resource ID`
     - Mapped Attribute: `patientId` (or your user store attribute)

     - Claim URI: `http://wso2.org/claims/practitioner`
     - Display Name: `Practitioner ID`
     - Description: `FHIR Practitioner Resource ID`
     - Mapped Attribute: `practitionerId` (or your user store attribute)

4. **Start the Identity Server**:
   ```bash
   cd <IS_HOME>/bin
   ./wso2server.sh
   ```

### For Asgardeo

1. **Package the component** as per Asgardeo extension guidelines
2. **Upload** the extension through the Asgardeo console
3. **Configure** the required claims in Asgardeo user attributes

## Configuration

### SMART on FHIR Launch Context

The accelerator automatically handles SMART launch scopes:

- **`launch/patient`**: Adds patient context to token response
- **`launch/practitioner`**: Adds practitioner context to token response

No additional configuration is required beyond setting up the user claims.

### User Claims Mapping

Ensure users have the appropriate patient or practitioner ID claims populated in their user profiles:

1. **For patients**: Set the `http://wso2.org/claims/patient` claim to the FHIR patient resource ID
2. **For practitioners**: Set the `http://wso2.org/claims/practitioner` claim to the FHIR practitioner resource ID

## Usage Example

### OAuth2 Token Request with SMART Scope

**Request**:
```bash
curl -X POST https://<IS_HOST>:<IS_PORT>/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "code=<authorization_code>" \
  -d "redirect_uri=<redirect_uri>" \
  -d "client_id=<client_id>" \
  -d "client_secret=<client_secret>"
```

**Response** (when user has patient context and `launch/patient` scope was requested):
```json
{
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGci...",
  "refresh_token": "ef3a7c5e-7c2e-4c5e-8f3a...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "patient": "patient-123"
}
```

## Verification

To verify the installation:

1. **Check component is loaded**:
   - View IS server startup logs
   - Look for: `Healthcare SMART Auth service component activated`

2. **Test token endpoint**:
   - Request a token with `launch/patient` scope
   - Verify the `patient` attribute is present in the response

3. **Check OSGi console** (if enabled):
   ```bash
   osgi> lb | grep healthcare
   ```
   Should show the `org.wso2.healthcare.is.smart.auth` bundle as ACTIVE

## Troubleshooting

### Component Not Loading

**Symptom**: Token responses don't include patient/practitioner context

**Solutions**:
1. Check IS logs for errors during component activation
2. Verify JAR file is in the correct `dropins` folder
3. Ensure all dependencies are available (they should be in standard IS distribution)
4. Restart the server with `-DosgiConsole` flag to check bundle status

### Claims Not Resolved

**Symptom**: Token response has `null` for patient/practitioner value

**Solutions**:
1. Verify claim URIs are correctly configured in IS
2. Check user profile has the claim values populated
3. Ensure claim dialect mapping is correct
4. Review IS logs for claim resolution errors

### Scope Not Working

**Symptom**: Launch scope is requested but context not added

**Solutions**:
1. Verify the exact scope string matches: `launch/patient` or `launch/practitioner`
2. Ensure the scope is included in the OAuth application configuration
3. Check that the user has the corresponding claim in their profile

## Uninstallation

To remove the accelerator:

1. **Stop the Identity Server**:
   ```bash
   cd <IS_HOME>/bin
   ./wso2server.sh stop
   ```

2. **Remove the component JARs**:
   ```bash
   rm <IS_HOME>/repository/components/dropins/org.wso2.healthcare.is.smart.auth*.jar
   ```

3. **Clean cached OSGi bundles** (optional but recommended):
   ```bash
   rm -rf <IS_HOME>/repository/components/plugins/org.wso2.healthcare.is.smart.auth*
   ```

4. **Start the Identity Server**:
   ```bash
   cd <IS_HOME>/bin
   ./wso2server.sh
   ```

## Additional Resources

- [WSO2 Healthcare Accelerator Main README](../../README.md)
- [IS Accelerator Components Documentation](../../product-accelerators/is/README.md)
- [SMART on FHIR Specification](https://hl7.org/fhir/smart-app-launch/)
- [WSO2 Identity Server Documentation](https://is.docs.wso2.com/)

## Support

For issues and questions:
- GitHub Issues: https://github.com/wso2/healthcare-accelerator/issues
- WSO2 Support: https://wso2.com/support/

## License

Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
