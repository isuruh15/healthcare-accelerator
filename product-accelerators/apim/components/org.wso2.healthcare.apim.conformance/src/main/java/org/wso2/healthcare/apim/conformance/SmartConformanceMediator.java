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

package org.wso2.healthcare.apim.conformance;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.xml.stream.XMLStreamException;
import java.util.List;
import java.util.Map;

/**
 * This class will be referred by the synapse API which deployed to handle the 'well-know resource URI'
 */
public class SmartConformanceMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(ConformanceMediator.class);

    @Override
    public boolean mediate(MessageContext mc) {
        //endpoint https://localhost:8243/.well-known/smart-configuration
        if (log.isDebugEnabled()) {
            log.debug("SmartConformanceMediator Started");
        }
        String tenant = mc.getProperty(Constants.TENANT_INFO_DOMAIN).toString();
        JSONObject resource = createWellKnownURIPayload(tenant);
        if (mc instanceof Axis2MessageContext) {
            org.apache.axis2.context.MessageContext axisMsgCtx =
                    ((Axis2MessageContext) mc).getAxis2MessageContext();
            Object headers = axisMsgCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            String targetContentType = Constants.JSON_CONTENT_TYPE;
            if (headers instanceof Map) {
                Map headersMap = (Map) headers;
                if (headersMap.get(Constants.HTTP_HEADER_ACCEPT) != null) {
                    targetContentType = (String) headersMap.get(Constants.HTTP_HEADER_ACCEPT);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("No HTTP Accept header found");
                }
            }
            if (Constants.XML_CONTENT_TYPE.equalsIgnoreCase(targetContentType) ||
                    Constants.TEXT_XML_CONTENT_TYPE.equalsIgnoreCase(targetContentType)) {
                OperationOutcome errorResource = new OperationOutcome();
                try {
                    JsonUtil.removeJsonPayload(axisMsgCtx);
                    String payload;
                    payload = Util.serializeToXML(errorResource);
                    OMElement omXML = AXIOMUtil.stringToOM(payload);
                    axisMsgCtx.getEnvelope().getBody().addChild(omXML);
                    axisMsgCtx.setProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE, Constants.XML_CONTENT_TYPE);
                    axisMsgCtx
                            .setProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE, Constants.XML_CONTENT_TYPE);
                } catch (XMLStreamException e) {
                    throw new ConformanceMediatorException(
                            "Error occurred while populating XML payload in the message context", e);
                }
            } else {
                String payload;
                payload = resource.toString();
                try {
                    JsonUtil.getNewJsonPayload(axisMsgCtx, payload, true, true);
                    axisMsgCtx.setProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE, Constants.JSON_CONTENT_TYPE);
                    axisMsgCtx.setProperty(
                            org.apache.axis2.Constants.Configuration.CONTENT_TYPE, Constants.JSON_CONTENT_TYPE);
                } catch (AxisFault axisFault) {
                    throw new ConformanceMediatorException(
                            "Error occurred while populating JSON payload in message context",
                            axisFault);
                }
            }
            axisMsgCtx.removeProperty("NO_ENTITY_BODY");
        }
        return true;
    }

    public JSONObject createWellKnownURIPayload(String tenant) {

        log.info("Creating SMART well-known URI payload for tenant: " + tenant);
        JSONObject wellKnownURIDocument = new JSONObject();
        JSONArray authMethods = new JSONArray(Constants.SMART_AUTH_METHODS);
        JSONArray scopesSupported = new JSONArray(Constants.SMART_SCOPES_SUPPORTED);
        JSONArray responseTypes = new JSONArray(Constants.SMART_RESPONSE_TYPES);
        JSONArray capabilities = new JSONArray(Constants.SMART_CAPABILITIES);
        JSONArray codeChallengeMethods = new JSONArray().put(Util.getSignatureAlgorithm().contains(
                "SHA256") ? "S256" : "");
        List<String> supportedGrantTypes = Util.getSupportedGrantTypes(tenant);
        JSONArray grantTypes = new JSONArray();
        if (supportedGrantTypes.contains(Constants.GRANT_TYPE_AUTH_CODE)) {
            grantTypes.put(Constants.GRANT_TYPE_AUTH_CODE);
        }
        if (supportedGrantTypes.contains(Constants.GRANT_TYPE_CLIENT_CREDENTIALS)) {
            //this condition should also check SMART backend authorization feature as well. It will be added for all the
            //distributions by default.
            grantTypes.put(Constants.GRANT_TYPE_CLIENT_CREDENTIALS);
        }
        try {
            wellKnownURIDocument.put("authorization_endpoint", Util.getKeyManagerProperty(tenant, 0,
                    "authorize_endpoint"));
            wellKnownURIDocument.put("token_endpoint", Util.getKeyManagerProperty(tenant, 0, "token_endpoint"));
            wellKnownURIDocument.put("introspection_endpoint", Util.getKeyManagerProperty(tenant, 0, "introspection_endpoint"));
            wellKnownURIDocument.put("token_endpoint_auth_methods_supported", authMethods);
            wellKnownURIDocument.put("scopes_supported", scopesSupported);
            wellKnownURIDocument.put("response_types_supported", responseTypes);
            wellKnownURIDocument.put("revocation_endpoint", Util.getKeyManagerProperty(tenant, 0,
                    "revoke_endpoint"));
            wellKnownURIDocument.put("capabilities", capabilities);
            wellKnownURIDocument.put(Constants.SMART_GRANT_TYPES, grantTypes);
            wellKnownURIDocument.put(Constants.SMART_CODE_CHALLENGE_METHODS, codeChallengeMethods);
            //the following attributes are required if the server supports sso-openid-connect capability.
            wellKnownURIDocument.put("jwks_uri", Util.getKeyManagerProperty(tenant, 0,
                    "certificate_value"));
            wellKnownURIDocument.put("issuer", Util.getKeyManagerProperty(tenant, 0, "issuer"));
        } catch (JSONException e) {
            log.error("Error occurred while creating the SMART config payload: " + e.getMessage());
            throw new ConformanceMediatorException("Error occurred while creating the SMART config payload", e);
        }
        return wellKnownURIDocument;
    }
}
