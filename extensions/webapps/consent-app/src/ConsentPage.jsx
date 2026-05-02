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

import { useState, useCallback } from "react";
import "./ConsentPage.css";

const { backendUrl = "" } = window.__CONSENT_APP_CONFIG__ ?? {};
const storeScopesUrl = `${backendUrl}/store-scopes`;

// ─── WSO2 Logo ────────────────────────────────────────────────────────────────
function Wso2Logo() {
  return (
    <img
      src="https://wso2.cachefly.net/wso2/sites/all/image_resources/logos/WSO2-Logo-Black.webp"
      alt="WSO2"
      className="wso2-logo"
    />
  );
}

// ─── ConsentPage component ─────────────────────────────────────────────────────
export default function ConsentPage({
  sessionDataKeyConsent,
  spId,
  user,
  scopes = [],  
  mandatoryClaims = "",
  consentAuthorizeRedirectUrl,
  additionalContext = [],
  onApprove,
  onDeny,
}) {
  const additionalContextValues = Array.isArray(additionalContext) ? additionalContext : [];
  // Parse mandatoryClaims string (e.g. "0_Telephone,2_Phone Verified,3_Email")
  // into [{id: "0", name: "Telephone"}, {id: "2", name: "Phone Verified"}, ...]
  const claims = mandatoryClaims
    ? mandatoryClaims.split(",").map((c) => {
        const idx = c.indexOf("_");
        return idx >= 0
          ? { id: c.substring(0, idx), name: c.substring(idx + 1) }
          : { id: c, name: c };
      })
    : [];
  // Validate SMART scopes: if it starts with patient/user/system/ it must match the full regex
  const scopeRegex = /^(patient|user|system)\/(\*|[A-Za-z]+)\.(cruds|(?=[cruds]+$)c?r?u?d?s?)$/;
  const isValidScope = (s) => {
    if (/^(patient|user|system)\//.test(s)) return scopeRegex.test(s);
    return true;
  };

  // Split scopes: hidden (OH_launch/) vs selectable; drop invalid SMART scopes
  const hiddenScopes = scopes.filter((s) => s.startsWith("OH_launch/"));
  const selectableScopes = scopes
    .filter((s) => !s.startsWith("OH_launch/"))
    .filter(isValidScope);

  const [checked, setChecked] = useState(() =>
    Object.fromEntries(selectableScopes.map((s) => [s, true]))
  );

  const toggleScope = useCallback((scope) => {
    setChecked((prev) => ({ ...prev, [scope]: !prev[scope] }));
  }, []);

  const toggleAll = useCallback((val) => {
    setChecked(Object.fromEntries(selectableScopes.map((s) => [s, val])));
  }, [selectableScopes]);

  const selectedScopes = selectableScopes.filter((s) => checked[s]);

  const storeScopes = async (scopesToStore) => {
    try {
      await fetch(storeScopesUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          sessionDataKeyConsent,
          scopes: scopesToStore,
        }),
      });
    } catch (err) {
      console.warn("Failed to store scopes:", err);
    }
  };

  const submitAuthorizeForm = (consent, scopesToSubmit) => {
    const targetUrl = consentAuthorizeRedirectUrl;
    const form = document.createElement("form");
    form.method = "post";
    form.action = targetUrl;
    const fields = {
      sessionDataKeyConsent: sessionDataKeyConsent,
      consent: consent,
      hasApprovedAlways: "false",
      user_claims_consent: "true",
      user: user,
      spId: spId,
    };
    // Append consent_<id>: "approved" for each mandatory claim
    claims.forEach((c) => {
      fields[`consent_${c.id}`] = "approved";
    });
    Object.entries(fields).forEach(([k, v]) => {
      const el = document.createElement("input");
      el.type = "hidden"; el.name = k; el.value = v;
      form.appendChild(el);
    });
    if (scopesToSubmit && scopesToSubmit.length > 0) {
      const el = document.createElement("input");
      el.type = "hidden";
      el.name = "scope";
      el.value = scopesToSubmit.join(" ");
      form.appendChild(el);
    }
    document.body.appendChild(form);
    form.submit();
  };

  const handleApprove = async () => {
    if (onApprove) {
      onApprove({ sessionDataKeyConsent, spId, user, scopes: [...selectedScopes, ...hiddenScopes] });
      return;
    }
    const allScopes = [...selectedScopes, ...hiddenScopes];
    await storeScopes(allScopes);
    submitAuthorizeForm("approve", allScopes);
  };

  const handleDeny = () => {
    if (onDeny) { onDeny(); return; }
    submitAuthorizeForm("deny", []);
  };

  return (
    <div className="consent-page">
      {/* Header */}
      <header className="consent-header">
        <Wso2Logo />
        <span className="header-subtitle">
          OPEN HEALTHCARE
        </span>
      </header>

      {/* Main content */}
      <main className="consent-main">
        <div className="consent-card">
          <h1 className="card-title">Authorize Access</h1>
          <p className="card-subtitle">
            Review and approve the permissions requested by this application.
          </p>

          {/* Meta badges */}
          <div className="meta-row">
            <span className="meta-badge">👤 {user}</span>
          </div>

          {/* Mandatory claims */}
          {claims.length > 0 && (
            <>
              <div className="section-label">Required User Attributes</div>
              <div className="scope-list">
                {claims.map((c, i) => (
                  <div
                    key={c.id}
                    className={`scope-item checked ${i === claims.length - 1 ? "last" : ""}`.trim()}
                  >
                    <span className="scope-label checked">{c.name}</span>
                  </div>
                ))}
              </div>
            </>
          )}

          {/* Selectable scopes */}
          {selectableScopes.length > 0 ? (
            <>
              <div className="section-label">Requested Permissions</div>
              <div className="scope-list">
                {selectableScopes.map((scope, i) => {
                  const isChecked = !!checked[scope];
                  const isLast = i === selectableScopes.length - 1;
                  return (
                    <div
                      key={scope}
                      className={`scope-item ${isChecked ? "checked" : ""} ${isLast ? "last" : ""}`.trim()}
                      onClick={() => toggleScope(scope)}
                    >
                      <input
                        type="checkbox"
                        className="scope-checkbox"
                        checked={isChecked}
                        onChange={() => toggleScope(scope)}
                        onClick={(e) => e.stopPropagation()}
                      />
                      <span className={`scope-label ${isChecked ? "checked" : ""}`.trim()}>{scope}</span>
                    </div>
                  );
                })}
              </div>

              {/* Bulk controls */}
              <div className="bulk-row">
                <button type="button" className="ghost-btn" onClick={() => toggleAll(true)}>
                  ✓ Select all
                </button>
                <button type="button" className="ghost-btn" onClick={() => toggleAll(false)}>
                  ✕ Clear all
                </button>
                <span className="selection-count">
                  {selectedScopes.length}/{selectableScopes.length} selected
                </span>
              </div>
            </>
          ) : (
            <div className="scope-list">
              <div className="empty-state">No selectable permissions found.</div>
            </div>
          )}

          <hr className="divider" />

          {/* Actions */}
          <div className="action-row">
            <button
              type="button"
              className="approve-btn"
              onClick={handleApprove}
            >
              Approve
            </button>
            <button
              type="button"
              className="deny-btn"
              onClick={handleDeny}
            >
              Deny
            </button>
          </div>

        </div>
      </main>

      {/* Footer */}
      <footer className="consent-footer">WSO2 Healthcare | © {new Date().getFullYear()}</footer>
    </div>
  );
}
