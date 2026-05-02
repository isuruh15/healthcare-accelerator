/**
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import { useState, useEffect } from 'react';
import ConsentPage from './ConsentPage';
import PatientPickerPage from './PatientPickerPage';

const { backendUrl = "", consentAuthorizeRedirectUrl = "https://localhost:9453/oauth2/authorize" } = window.__CONSENT_APP_CONFIG__ ?? {};
const apiUrl = `${backendUrl}/api`;

function readStoredPatient() {
  const stored = sessionStorage.getItem("selectedPatient");
  if (!stored) return null;
  sessionStorage.removeItem("selectedPatient");
  try { return JSON.parse(stored); } catch { return null; }
}

function isPractitioner(scimUser) {
  const fhirUser = scimUser?.["urn:scim:schemas:extension:custom:User"]?.fhirUser ?? "";
  return typeof fhirUser === "string" && fhirUser.includes("Practitioner");
}

export default function App() {
  const [consentProps, setConsentProps] = useState(null);
  const [consentError, setConsentError] = useState(null);
  const [selectedPatient, setSelectedPatient] = useState(readStoredPatient);
  const [practitioner, setPractitioner] = useState(null); // null = loading, true/false = resolved

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const sessionDataKeyConsent = params.get("sessionDataKeyConsent");
    const spId = params.get("spId");

    if (!sessionDataKeyConsent || !spId) {
      setConsentError("Missing required query parameters: sessionDataKeyConsent and spId.");
      return;
    }

    fetch(`${apiUrl}/consent-context?sessionDataKeyConsent=${encodeURIComponent(sessionDataKeyConsent)}&spId=${encodeURIComponent(spId)}`)
      .then((r) => r.ok ? r.json() : r.text().then((msg) => Promise.reject(msg || r.statusText)))
      .then((data) => setConsentProps(data))
      .catch((err) => setConsentError(String(err)));
  }, []);

  const userId = consentProps?.user ?? "";

  useEffect(() => {
    if (consentProps === null) return; // wait for consent context
    if (!userId) {
      setPractitioner(false);
      return;
    }
    fetch(`${apiUrl}/me?userId=${encodeURIComponent(userId)}`)
      .then((r) => r.ok ? r.json() : Promise.reject())
      .then((data) => setPractitioner(isPractitioner(data)))
      .catch(() => setPractitioner(false));
  }, [userId, consentProps]);

  if (consentError) {
    return (
      <div style={{ fontFamily: 'monospace', padding: '2rem', color: 'var(--oxygen-palette-error-main, #d32f2f)' }}>
        <strong>Failed to load consent context.</strong> {consentError}
      </div>
    );
  }

  if (consentProps === null || practitioner === null) {
    return null; // loading
  }

  if (practitioner && !selectedPatient) {
    return (
      <PatientPickerPage
        {...consentProps}
        onProceed={(patient) => setSelectedPatient(patient)}
      />
    );
  }

  const additionalContext = selectedPatient ? [JSON.stringify(selectedPatient)] : [];
  return <ConsentPage {...consentProps} additionalContext={additionalContext} consentAuthorizeRedirectUrl={consentAuthorizeRedirectUrl} />;
}
