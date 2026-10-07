# GoreeCloud Launcher — User-owned Direct API Answers

Status: **Development candidate; not integrated or release-qualified.**  
Source: `apps/launcher/` in `GoreeCloud/android-app-defaults`  
Owner feedback: Universal Search must answer through the user's chosen Claude, OpenAI/ChatGPT, Gemini, Perplexity, or compatible custom API without opening a dedicated app.

## User-facing contract

**Universal Search → Sources → Direct API answers** exposes individually configurable connections:

- **ChatGPT (OpenAI API)** — user API key and model; OpenAI Chat Completions.
- **Claude API** — user API key and model; Anthropic Messages.
- **Gemini API** — user API key and model; Google Gemini generateContent.
- **Perplexity API** — user API key and model; Perplexity Sonar Chat Completions.
- **Custom chat API** — user-named HTTPS OpenAI Chat Completions-compatible endpoint, model, authorization header, optional credential (including no-key private HTTPS servers).
- **Custom search API** — user-named HTTPS GET JSON endpoint, query parameter, auth header, optional credential, and JSON response path. Lists of string or {title, snippet, url} objects can appear as bounded text answers.

Each connection is independently enabled/disabled, edited, and (for custom entries) deleted. A separately confirmed **Delete all API connections** action erases the local ciphertext and its dedicated Keystore key to recover from unreadable key material; externally issued keys must be revoked separately. Official provider destinations are pinned to their fixed HTTPS origins; user-defined destinations are explicit and visibly configured, never inferred from search text. API credentials are entered through a concealed field, cannot be read back into the settings form, and are never displayed in results, diagnostics, or documentation.

**Universal Search → type a question → Ask** sends only the current question to exactly the selected enabled API and renders the returned answer inside Launcher. A new question, navigation away, Cancel, or disposal stops the in-progress interaction and does not retain an answer history. No query-as-you-type API dispatch or multi-provider fan-out occurs. Legacy provider app handoffs and local results remain separately governed.

## Security and privacy

- Reusable API credentials and full connection definitions are encrypted using an app-scoped, non-exportable Android Keystore AES-256 GCM key and atomically stored in Launcher-private `noBackupFilesDir`. They do not enter DataStore, Room, portable preferences, Android backup, telemetry, or repository state.
- Requests require HTTPS and disallow URL-embedded credentials, query strings, fragments, and untrusted redirects. Official provider endpoints are immutable; custom endpoints are user-authored. Custom API destinations additionally reject localhost, IP literals, local/reserved DNS suffixes, nondefault HTTPS ports, and hosts resolving to private, shared, benchmark, multicast, link-local, or loopback addresses before placing API credentials in request headers. The DNS check is a bounded client preflight, **not** DNS pinning or a fully hardened outbound proxy; rebinding/re-resolution and representative-device negative tests remain an acceptance gate.
- Credentials are sent only in the provider-request header, never in a URL. API errors do not display remote response bodies, credential values, or request payloads.
- The transport bounds query size (2,000 characters), response body size (96 KB), displayed answer size (12,000 characters), response tokens (512 where supported), connect timeout (10 seconds), and read timeout (20 seconds). Every network call requires a deliberate Ask tap.
- API billing, availability, safety limitations, and provider-side retention remain under the user's API agreement. Users can disable/delete sources without affecting Launcher local search.
- No new Android permissions are requested; Launcher already has `INTERNET`.

## Compatibility and limits

The two custom adapters cover common **OpenAI-compatible JSON chat** and **GET JSON search** interfaces, not literally every proprietary protocol. OAuth-only APIs, non-JSON responses, streaming, multipart, tool calls, arbitrary request-body templates, private-key authentication, SSE, and providers without compatible user-held credentials require separate reviewed adapters. No server-managed user key is introduced, and a ChatGPT subscription is not an API credential.

External service API methods and model availability may change; the model field is user-editable. Existing local, account-authorized Google Drive, provider app handoffs, notification permissions, profile boundaries, App Lock, HOME role, Room workspace, and Glaze behavior are not expanded by this work.


The Development candidate includes focused JVM policy coverage for private destination rejection and an Android runtime fixture verifying encrypted `noBackupFilesDir` storage, fake-key readback, and key-redacted diagnostics. They remain **unverified as passing** until exact-head protected CI completes. Source badges use the installed official provider app artwork where present and otherwise an explicitly generic API mark, not a fabricated official logo.

## Acceptance

Required before source integration or broader maturity claims:

1. Exact new-head Android build, JVM, lint, Room schema, source/manifest guards, and full Android 16 runtime validation.
2. Representative-device successful requests with scoped **test keys** for all four official API adapters and custom chat/search, plus 401/403/429/5xx, TLS failure, no network, malformed JSON, timeout, cancellation, and max-size behavior.
3. Credential encryption/noBackup integrity, Android Keystore loss/recovery, update-in-place and signing continuity, screenshot/accessibility secret exclusion, large-text/keyboard/reflow and profile behavior.
4. Owner acceptance of direct answer UX, provider charges disclosure, deletion/revocation, and custom endpoint trust boundaries.

**No success, source integration, production readiness, or release status is claimed solely from candidate source or CI startup.**
