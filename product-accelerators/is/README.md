# WSO2 Healthcare Identity Server Accelerator

The WSO2 Healthcare Identity Server (IS) Accelerator provides healthcare-specific identity and access management capabilities for WSO2 Identity Server and Asgardeo, enabling seamless integration with healthcare standards such as SMART on FHIR.

## Overview

This accelerator extends WSO2 Identity Server with healthcare-specific authentication and authorization features, making it easier to build secure, standards-compliant healthcare applications.

## Components

### 1. SMART on FHIR Auth Component

**Module**: `org.wso2.healthcare.is.smart.auth`

**Location**: [components/org.wso2.healthcare.is.smart.auth](components/org.wso2.healthcare.is.smart.auth)

**Purpose**: Implements SMART on FHIR authentication and authorization specifications for healthcare applications.

**Key Features**:
- Launch context handling (`launch/patient`, `launch/practitioner`)
- Patient and practitioner context injection in token responses
- Healthcare-specific claim resolution
- SMART on FHIR compliant token response modifications

**Main Classes**:
- `HealthcareSmartAuthTokenResponseHandler`: Access token response handler that adds SMART on FHIR specific attributes
- `UserClaimResolver`: Utility for resolving user claims from the user store
- `HealthcareSmartServiceComponent`: OSGi service component for registering the token response handler

## Building the IS Accelerator

From the healthcare-accelerator root directory:

```bash
mvn clean install
```

To build only the IS accelerator components:

```bash
cd product-accelerators/is
mvn clean install
```

## Deployment

### For WSO2 Identity Server

1. Build the IS accelerator distribution:
   ```bash
   cd distribution/is-accelerator
   mvn clean install
   ```

2. Extract the generated ZIP file from `distribution/is-accelerator/target/`

3. Copy the JAR files from the `dropins` folder to `<IS_HOME>/repository/components/dropins/`

4. Restart the Identity Server

### For Asgardeo

The SMART on FHIR auth component can be deployed as a custom authenticator extension in Asgardeo. Follow the Asgardeo extension deployment guidelines.

## Configuration

### SMART Launch Scopes

The accelerator supports the following SMART launch scopes:

- `launch/patient` - Launch with patient context
- `launch/practitioner` - Launch with practitioner context

### User Claims

Configure the following claims in your user store:

- **Patient ID Claim**: `http://wso2.org/claims/patient`
  - Stores the FHIR patient resource ID for the user

- **Practitioner ID Claim**: `http://wso2.org/claims/practitioner`
  - Stores the FHIR practitioner resource ID for the user

These claims can be configured in the Identity Server management console under **Claims > Add**.

## Usage

### Token Response Enhancement

When a token request includes SMART launch scopes, the accelerator automatically adds the corresponding context to the token response:

**Request with `launch/patient` scope**:
```json
{
  "access_token": "...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "patient": "patient-123"
}
```

**Request with `launch/practitioner` scope**:
```json
{
  "access_token": "...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "practitioner": "practitioner-456"
}
```

## Development

### Adding New Components

To add new IS accelerator components:

1. Create a new module under `product-accelerators/is/components/`
2. Follow the naming convention: `org.wso2.healthcare.is.<component-name>`
3. Update `product-accelerators/pom.xml` to include the new module
4. Update the distribution assembly if the component needs to be packaged

### Package Structure

```
org.wso2.healthcare.is.<component>
├── src/main/java/org/wso2/healthcare/is/<component>/
│   ├── internal/           # OSGi service components and data holders
│   ├── common/            # Constants and utilities
│   └── ...                # Feature-specific packages
└── pom.xml
```

## Testing

Run unit tests:

```bash
mvn test
```

## Dependencies

The IS accelerator components depend on:

- WSO2 Carbon Identity Framework
- WSO2 Carbon OAuth2 components
- WSO2 Carbon User Core
- Apache Felix OSGi annotations

All dependencies are managed through the parent POM.

## Contributing

When contributing to the IS accelerator:

1. Follow the existing code structure and naming conventions
2. Use the Apache 2.0 license header in all source files
3. Ensure all tests pass before submitting
4. Update documentation for new features

## License

Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.

Licensed under the Apache License, Version 2.0.

## Support

For issues and questions:
- Create an issue in the [GitHub repository](https://github.com/wso2/healthcare-accelerator)
- Refer to the [WSO2 Healthcare Accelerator documentation](../../README.md)
