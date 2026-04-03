#!/bin/bash
# ------------------------------------------------------------------------
#
# Copyright (c) 2024, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
#
# This software is the property of WSO2 Inc. and its suppliers, if any.
# Dissemination of any information or reproduction of any material contained
# herein is strictly forbidden, unless permitted by WSO2 in accordance with
# the WSO2 Commercial License available at http://wso2.com/licenses. For specific
# language governing the permissions and limitations under this license,
# please see the license as well as any agreement you've entered into with
# WSO2 governing the purchase of this software and any associated services.
#
# ------------------------------------------------------------------------

# merge.sh script copy the WSO2 OH IS accelerator artifacts on top of WSO2 IS base product
#
# merge.sh <WSO2_OH_IS_HOME>

# Initialize variables
WSO2_OH_IS_HOME=""

# resolve links - $0 may be a softlink
PRG="$0"

# Parse arguments
for arg in "$@"; do
  case $arg in
    *)
      if [ -z "$WSO2_OH_IS_HOME" ]; then
        WSO2_OH_IS_HOME="$arg"
      else
        echo -e "[ERROR] Unknown argument: $arg"
        exit 1
      fi
      ;;
  esac
done

while [ -h "$PRG" ]; do
  ls=$(ls -ld "$PRG")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  if expr "$link" : '.*/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=$(dirname "$PRG")/"$link"
  fi
done

# Get standard environment variables
PRGDIR=$(dirname "$PRG")

ACCELERATOR_HOME=$(cd "$PRGDIR/.." || exit ; pwd)

echo "[INFO] Accelerator home is: ${ACCELERATOR_HOME}"

# set product home
if [ "${WSO2_OH_IS_HOME}" == "" ];
  then
    WSO2_OH_IS_HOME=${ACCELERATOR_HOME}/..
fi

echo "[INFO] Product home is: ${WSO2_OH_IS_HOME}"

# validate product home
if [ ! -d "${WSO2_OH_IS_HOME}/repository/components" ]; then
  echo -e "[ERROR] Specified product path is not a valid carbon product path";
  exit 2;
else
  echo -e "[INFO] Valid carbon product path found";
fi

# Reading the config.toml file
config_toml_file="${ACCELERATOR_HOME}"/conf/config.toml

# Read values from the TOML file
enable_smart_on_fhir=$(grep -E '^enable_smart_on_fhir = ' "$config_toml_file" | cut -d'=' -f2 | tr -d ' ')

# create the hc-accelerator folder in product home, if not exist
WSO2_OH_ACCELERATOR_VERSION=$(cat "${ACCELERATOR_HOME}"/version.txt)
WSO2_OH_ACCELERATOR_AUDIT="${WSO2_OH_IS_HOME}"/hc-accelerator
WSO2_OH_ACCELERATOR_AUDIT_BACKUP="${WSO2_OH_ACCELERATOR_AUDIT}"/backup
if [ ! -d  "${WSO2_OH_ACCELERATOR_AUDIT}" ]; then
   mkdir -p "${WSO2_OH_ACCELERATOR_AUDIT_BACKUP}"
   mkdir -p "${WSO2_OH_ACCELERATOR_AUDIT_BACKUP}"/conf
   echo -e "[INFO] Accelerator audit folder [""${WSO2_OH_ACCELERATOR_AUDIT}""] is created";
else
   echo -e "[INFO] Accelerator audit folder is present at [""${WSO2_OH_ACCELERATOR_AUDIT}""]";
fi

# backup original product files to the audit folder
echo -e "[INFO] Backup original product files.."
if [ -z "$(find "${WSO2_OH_ACCELERATOR_AUDIT_BACKUP}/conf" -mindepth 1 -print -quit)" ]; then
  cp "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml "${WSO2_OH_ACCELERATOR_AUDIT_BACKUP}"/conf 2>/dev/null
  cp "${WSO2_OH_IS_HOME}"/repository/conf/claim-config.xml "${WSO2_OH_ACCELERATOR_AUDIT_BACKUP}"/conf 2>/dev/null
else
  echo -e "[INFO] Backup files already exist in the audit folder"
fi

# Reusable functions

configure_claims_and_scopes() {
    local claim_config="${WSO2_OH_IS_HOME}/repository/conf/claim-config.xml"
    local oidc_scope_config="${WSO2_OH_IS_HOME}/repository/conf/identity/oidc-scope-config.xml"

    echo -e "[INFO] Adding configurations to claim-config.xml file"

    # Configure local claim
    local patient_id_claim="\t<Dialect dialectURI=\"http://wso2.org/claims\">\n<Claim>\n<ClaimURI>http://wso2.org/claims/patientId</ClaimURI>\n<DisplayName>Patient ID</DisplayName>\n<AttributeID>patientId</AttributeID>\n<Description>PatientID</Description>\n<DisplayOrder>13</DisplayOrder>\n<SupportedByDefault />\n</Claim>\n"
    if grep -Fq '<ClaimURI>http://wso2.org/claims/patientId</ClaimURI>' "$claim_config"; then
        echo -e "[WARN] PatientId local claim configuration already exist"
    else
        sed -i -e "s|<Dialect dialectURI=\"http://wso2.org/claims\">|${patient_id_claim}|g" "$claim_config"
    fi

    # Configure OIDC claim
    local patient_id_oidc_claim="\t<Dialect dialectURI=\"http://wso2.org/oidc/claim\">\n<Claim>\n<ClaimURI>patientId</ClaimURI>\n<DisplayName>Patient ID</DisplayName>\n<AttributeID>patientId</AttributeID>\n<Description>PatientID</Description>\n<DisplayOrder>13</DisplayOrder>\n<MappedLocalClaim>http://wso2.org/claims/patientId</MappedLocalClaim>\n</Claim>\n"
    if grep -Fq '<MappedLocalClaim>http://wso2.org/claims/patientId</MappedLocalClaim>' "$claim_config"; then
        echo -e "[WARN] PatientId OIDC claim configuration already exist"
    else
        sed -i -e "s|<Dialect dialectURI=\"http://wso2.org/oidc/claim\">|${patient_id_oidc_claim}|g" "$claim_config"
    fi

    echo -e "[INFO] Adding configurations to repository/conf/identity/oidc-scope-config.xml file"

    # Configure OIDC scopes using separate variables instead of an array
    local fhiruser_scope="<Scopes>\n\t<Scope id=\"fhirUser\">\n\t\t<Claim>patientId</Claim>\n\t</Scope>\n"
    local launch_patient_scope="<Scopes>\n\t<Scope id=\"launch\/patient\">\n\t\t<Claim>patientId</Claim>\n\t</Scope>\n"
    local offline_access_scope="<Scopes>\n\t<Scope id=\"offline_access\">\n\t\t<Claim>patientId</Claim>\n\t</Scope>\n"

    # Configure fhirUser scope
    if grep -Fq '<Scope id="fhirUser">' "$oidc_scope_config"; then
        echo -e "[WARN] fhirUser scope configuration already exist"
    else
        sed -i -e "s|<Scopes>|${fhiruser_scope}|g" "$oidc_scope_config"
    fi

    # Configure launch/patient scope
    if grep -Fq '<Scope id="launch/patient">' "$oidc_scope_config"; then
        echo -e "[WARN] launch/patient scope configuration already exist"
    else
        sed -i -e "s|<Scopes>|${launch_patient_scope}|g" "$oidc_scope_config"
    fi

    # Configure offline_access scope
    if grep -Fq '<Scope id="offline_access">' "$oidc_scope_config"; then
        echo -e "[WARN] offline_access scope configuration already exist"
    else
        sed -i -e "s|<Scopes>|${offline_access_scope}|g" "$oidc_scope_config"
    fi
}

echo -e "[INFO] Copying Open Healthcare artifacts.."
# adding the OH artifacts to the product pack
cp -R "${ACCELERATOR_HOME}"/carbon-home/repository/components/* "${WSO2_OH_IS_HOME}"/repository/components

# adding configurations to deployment.toml file
echo -e "[INFO] Adding configurations to deployment.toml file"

if [ "${enable_smart_on_fhir}" == "true" ]; then
  if grep -Fxq "[oauth]" "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml
    then
        # code if found
        echo -e "[WARN] oauth configuration already exist"
    else
        # code if not found
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS (BSD sed)
              sed -i '' '/\[oauth\.grant_type\.token_exchange\]/i\
[oauth]\
show_display_name_in_consent_page = true\
authorize_all_scopes = true\

  ' "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml
        else
            # Linux (GNU sed)
            sed -i '/\[oauth\.grant_type\.token_exchange\]/i [oauth]\nshow_display_name_in_consent_page = true\nauthorize_all_scopes = true' "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml
        fi
        echo -e "[INFO] Added [oauth] configuration above [oauth.grant_type.token_exchange]"
    fi

  if grep -Fxq "[oauth.grant_type.authorization_code]" "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml
  then
      # code if found
      echo -e "[WARN] oauth.grant_type.authorization_code configuration already exist"
  else
      # code if not found
      echo -e "\n[oauth.grant_type.authorization_code]\ngrant_handler = \"org.wso2.healthcare.apim.tokenmgt.handlers.OpenHealthcareExtendedAuthorizationCodeGrantHandler\""  | tee -a "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml >/dev/null
  fi

  # custom code response type handler
  if grep -Fxq "[[oauth.custom_response_type]]" "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml
  then
      # code if found
      echo -e "[WARN] oauth.custom_response_type configuration already exist"
  else
      # code if not found
      echo -e "\n[[oauth.custom_response_type]]\nname =\"code\"\nclass = \"org.wso2.healthcare.is.smart.auth.handlers.OpenHealthcareExtendedCodeResponseTypeHandler\""  | tee -a "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml >/dev/null
  fi

  # scopemgt config
  if grep -Fxq "#[healthcare.identity.scopemgt]" "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml || grep -Fxq "[healthcare.identity.scopemgt]" "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml
  then
      # code if found
      echo -e "[WARN] healthcare.identity.scopemgt configuration already exist"
  else
      # code if not found
      echo -e "\n[healthcare.identity.scopemgt]\nroles = [\"patient-read\", \"patient-write\", \"user-read\", \"user-write\"]\nenable_fhir_scope_to_wso2_scope_mapping = true"  | tee -a "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml >/dev/null
  fi

  # shared scopes
  if grep -Fxq "#[[healthcare.identity.scopemgt.shared_scopes]]" "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml || grep -Fxq "[[healthcare.identity.scopemgt.shared_scopes]]" "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml
  then
      # code if found
      echo -e "[WARN] healthcare.identity.scopemgt.shared_scopes configuration already exist"
  else
      # code if not found
     echo -e "\n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"patient/*.c\"\nname = \"patient/*.c\"\nroles = \"patient-write\"\ndescription = \"This scope grants patients access to CREATE any fhir resource.\"
     \n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"patient/*.r\"\nname = \"patient/*.r\"\nroles = \"patient-write,patient-read\"\ndescription = \"This scope grants patients access to READ any fhir resource.\"
     \n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"patient/*.u\"\nname = \"patient/*.u\"\nroles = \"patient-write\"\ndescription = \"This scope grants patients access to UPDATE any fhir resource.\"
     \n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"patient/*.d\"\nname = \"patient/*.d\"\nroles = \"patient-write\"\ndescription = \"This scope grants patients access to DELETE any fhir resource.\"
     \n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"patient/*.s\"\nname = \"patient/*.s\"\nroles = \"patient-write,patient-read\"\ndescription = \"This scope grants patients access to SEARCH any fhir resource.\"
     \n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"user/*.c\"\nname = \"user/*.c\"\nroles = \"user-write\"\ndescription = \"This scope grants other users access to CREATE any fhir resource.\"
     \n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"user/*.r\"\nname = \"user/*.r\"\nroles = \"user-write,user-read\"\ndescription = \"This scope grants other users access to READ any fhir resource.\"
     \n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"user/*.u\"\nname = \"user/*.u\"\nroles = \"user-write\"\ndescription = \"This scope grants other users access to UPDATE any fhir resource.\"
     \n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"user/*.d\"\nname = \"user/*.d\"\nroles = \"user-write\"\ndescription = \"This scope grants other users access to DELETE any fhir resource.\"
     \n[[healthcare.identity.scopemgt.shared_scopes]]\nkey = \"user/*.s\"\nname = \"user/*.s\"\nroles = \"user-write,user-read\"\ndescription = \"This scope grants other users access to SEARCH any fhir resource.\""  | tee -a "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml >/dev/null
  fi

  if grep -Fxq "#[healthcare.identity.claims]" "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml || grep -Fxq "[healthcare.identity.claims]" "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml
  then
      # code if found
      echo -e "[WARN] healthcare.identity.claims configuration already exist"
  else
      # code if not found
      echo -e "\n#[healthcare.identity.claims]\n#patient_id_claim_uri = \"http://wso2.org/claims/patientId\"\n#patient_id_key = \"patientId\"\n#fhirUser_resource_url_context = \"/r4/Patient\"\n#fhirUser_resource_id_claim_uri = \"http://wso2.org/claims/patientId\""  | tee -a "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml >/dev/null
  fi
fi

if grep -Fxq "id = \"private_key_jwt_authenticator\"" "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml || grep -Fxq "#id = \"private_key_jwt_authenticator\"" "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml
then
    # code if found
    echo -e "[WARN] private_key_jwt_authenticator configuration already exists"
else
    # code if not found
    echo -e "\n[[event_listener]]\nid = \"private_key_jwt_authenticator\"\ntype = \"org.wso2.carbon.identity.core.handler.AbstractIdentityHandler\"\nname = \"org.wso2.healthcare.apim.clientauth.jwt.PrivateKeyJWTClientAuthenticator\"\norder = \"899\"\n#[event_listener.properties]\n#max_allowed_jwt_lifetime_seconds = \"300\"\n#token_endpoint_alias = \"sampleurl\"\n\n[[cache.manager]] \nname = \"IdentityApplicationManagementCacheManager\" \ntimeout = \"10\"\ncapacity = \"5000\""  | tee -a "${WSO2_OH_IS_HOME}"/repository/conf/deployment.toml >/dev/null
fi

# Configure claims and scopes
echo -e "[INFO] Applying configurations for Claims and Scopes."
configure_claims_and_scopes

echo -e "[INFO] WSO2 Open Healthcare IS Accelerator is successfully applied"

echo -e "$(date)" - "$USER" - "${WSO2_OH_ACCELERATOR_VERSION}" | tee -a "${WSO2_OH_ACCELERATOR_AUDIT}"/merge_audit.log >/dev/null

echo -e "[INFO] Done"
