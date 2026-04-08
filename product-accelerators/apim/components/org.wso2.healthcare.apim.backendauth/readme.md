# Backend Authentication for WSO2 API Manager

This module provides a streamlined solution for integrating an external authentication server to securely obtain and 
manage access tokens for backend service requests. By default, WSO2 API Manager's gateway forward the end-user's access token 
from the invoking client to the backend API. 
<br>This module replaces that default behavior by fetching a new access token 
from an external OAuth 2.0-compliant authentication server and replacing the `Authorization` header with the 
retrieved token.

## Configuration Model
This feature can be configured either through the predefined deployment.toml file using TOML configuration or inline by 
specifying policy attributes directly when enabling the policy.

### 1. PREDEFINED CONFIGURATION

In PREDEFINED mode, configuration values must be specified in the deployment.toml file, and the configuration name 
should be referenced in the 'Config Value' policy attribute.

```toml
[[healthcare.backend.auth]]
name = "backend-auth"
auth_type = "pkjwt"  #"pkjwt" or "client-credentials"
token_endpoint = "https://localhost:9443/oauth2/token"
client_id = "client_id"
client_secret = "client_secret" # Only for Client Credentials flow
private_key_alias = "key_alias" # Only for PKJWT flow
```
###  Parameters

| Parameter           | Description                                                                                                                                              | Example Value                                 |
|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------|
| `name`              | Name of the authentication method. This name must be matched with the `Backend Auth Config` policy attribute in the `Replace Backend Auth Token` policy. | `epic-pkjwt`                                  |
| `auth_type`         | Authentication type. Only `pkjwt` and `client_credentials` are supported atm.                                                                            | `pkjwt`                                       |
| `token_endpoint`    | The URL of the external auth server's token endpoint for obtaining tokens.                                                                               | `https://external.auth.com:9443/oauth2/token` |
| `client_id`         | The client identifier registered with the auth server.                                                                                                   | `client_id`                                   |
| `client_secret`     | The client secret associated with the client ID (if applicable).                                                                                         | `client_secret`                               |
| `private_key_alias` | The alias of the private key used for signing JWTs (if applicable). This key must be added to the Primary Keystore                                       | `key_alias`                                   |

### 2. INLINE CONFIGURATION

In Inline mode, 'Config Value' is treated as the master configuration element, while following configuration settings 
can be overridden inline as needed. Additionally, `client_id` and `private_key_alias` configurations can be extracted from 
request headers or context properties by specifying them as Synapse expressions (e.g., `$header:<header_name>, $ctx:<property_name>`).

| Parameter           | Description                                                                                                        | Example Value                                 |
|---------------------|--------------------------------------------------------------------------------------------------------------------|-----------------------------------------------|
| `auth_type`         | Authentication type. Only `pkjwt` and `client_credentials` are supported atm.                                      | `pkjwt`                                       |
| `token_endpoint`    | The URL of the external auth server's token endpoint for obtaining tokens.                                         | `https://external.auth.com:9443/oauth2/token` |
| `client_id`         | The client identifier registered with the auth server. Synapse expressions allowed.                                | `client_id` or `$header:client_id`            |
| `client_secret`     | The client secret associated with the client ID (if applicable).                                                   | `client_secret`                               |
| `private_key_alias` | The alias of the private key used for signing JWTs (if applicable). This key must be added to the Primary Keystore | `key_alias` or `$ctx:property`                |

Also, when using both predefined and inline configurations, the inline configuration will take precedence over the predefined configuration.

## Key Features

- **External Auth Server Integration**: Connect to an external authentication server that supports OAuth 2.0 to 
retrieve tokens for backend services.
- **Grant Type Support**: Supports the following OAuth 2.0 grant types:
    - JWT Bearer Grant
    - Client Credentials Grant
- **Dynamic Header Replacement**: Automatically replaces the existing `Authorization` header in API requests with the 
access token retrieved from the configured auth server.

## Use of Private-Public key pair for PKJWT Auth flow
1. Generate an RSA key pair. Recommended signature algorithm is `RS256`.
2. Upload the private key to the WSO2 API Manager's Key Store. The alias of the uploaded key should be 
provided in the configuration(`private_key_alias`).
3. Configure the public key in the external authentication server.
4. If you're enabling SSL, provide the public key of the external authentication server in the 
WSO2 API Manager's truststore(`client-truststore.jks`).


## Important Notes
### Convert a Private Key from `.pem` to `PKCS#12` Format
```bash 
openssl pkcs12 -export \                                           
  -in <cert_for_sign> \  
  -inkey <privatekey_in_pem_format> \    
  -out <out_file_name>.p12 \
  -name <alias> \   
  -passout <password>
``` 

### Import a Private Key in `PKCS#12` format to a `JKS` Key Store
```bash
keytool -importkeystore \ 
  -srckeystore <p12_file_path> \ 
  -srcstoretype PKCS12 \ 
  -destkeystore <jks_file_path> \ 
  -deststoretype JKS \ 
  -srcstorepass <p12_file_password> \ 
  -deststorepass <jks_password> \
  -srcalias <alias_used_in_p12> \
  -destalias <alias_to_be_used_in_JKS>
```

### Limitations
- This feature is recommended for use as API-level policy. There may be unexpected behavior if used as a global policy.


