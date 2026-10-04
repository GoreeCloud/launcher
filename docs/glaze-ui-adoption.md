# GLAZE UI V1.6 Source Mapping — GoreeCloud Launcher

Status: **Development source mapping integrated / V1.7 migration and application acceptance pending**  
Current shared authority: **Glaze V1.7 (`1.7.0`)** at `1a5756daed2294155be2e9972b24f580f6222b7b`  
Current required target: **GLAZE UI V1.6 (`1.6.0`)**  
Canonical repository: `GoreeCloud/goreecloud-glaze-ui`  
Exact V1.6 implementation source authority: `a7180679ea851389e0f3004515f9a25f420e716d`  
Shared known-good rollback Stable: **GLAZE UI V1.5.1**  
Prior Launcher source baseline: **GLAZE UI V1.1 (`1.1.0`)** at `15cc76d2bcd4065552dc31c77145b63f34d9e7b2`  
Production eligible on the Glaze UI gate: **no**  
Automated contract: `scripts/check_glaze_ui.py`

## Authority boundary

## Current Launcher mockup authority

The owner-supplied Launcher references stored in `GoreeCloud/Mockups/GoreeCloud Launcher/` are the current product-specific visual authority for this Development pass:

- `Launcher Home — Current Mockup.png`
- `Universal Search — Current Mockup.png`

Those references control composition, hierarchy, translucency, spacing, density, and visual emphasis for Home and Universal Search. They do **not** authorize fabricated calendar events, contacts, files, cloud state, privacy metrics, or other sample data shown only to communicate the intended composition. Runtime surfaces must continue to present authoritative device/local/provider state and fail closed when data or permission is unavailable.

The mockup-alignment candidate therefore keeps Launcher search/provider authority, explicit connected-source handoff rules, workspace persistence, Android permissions, and Glaze V1.6 source authority unchanged while replacing the prior dark/opaque Home and Search presentation with wallpaper-first light glass.


Glaze V1.7 / 1.7.0 is now the current Anchor shared release at `1a5756daed2294155be2e9972b24f580f6222b7b` (qualification anchor `7c4ded83d7a8725165bb6a55dfb175667cc9589e`). V1.7.0 intentionally inherits the accepted V1.6.0 runtime, so Launcher's integrated V1.6 implementation remains relevant provenance but downstream V1.7 contract adoption and application acceptance are explicitly non-transferable. The Launcher source therefore separates **implementation mapping**, **current shared authority**, and **consumer acceptance**:

- `GlazeMetrics` and `GlazeV16PresentationPolicy` pin exact V1.6 source provenance.
- `GlazeCurrentAuthority.sourceMigrationRequired()` is true because the repository is still explicitly mapped as a V1.6 consumer while current shared authority is V1.7 / 1.7.0.
- `currentConsumerConformanceEstablished` remains false and `consumerAcceptanceRequired()` remains true.
- Platform Contract stays Development/nonconformant and records Glaze as `applicable-migration-required` rather than conformant.

No source string, successful build, emulator run, or shared Glaze qualification is allowed to manufacture Launcher-local rendered, accessibility, device, performance, production, release, or Stable acceptance.

## Implemented V1.6 source semantics

The integrated source consumes the accepted V1.6 release source and its inherited Stable authorities rather than inventing a new palette contract.

### Layout and interaction

The inherited Stable layout source contains 4/8/12/16/24/32/48/64 spacing, a 44 px-equivalent coarse target floor, and a 32 px pointer-compact floor. Launcher keeps a stricter 48 dp general touch floor plus a 56 dp accessibility-oriented target.

Launcher-owned 20 dp and 40 dp spacing conveniences, retained radius tiers, and retained optical geometry aliases are explicitly non-canonical product values.

### Material and accessibility policy

`GlazeV16PresentationPolicy` maps the V1.6 presentation-only contract:

- material roles distinguish canvas, solid, raised, functional glass, clear glass, and overlay;
- Reduced Transparency converts glass requests to solid presentation;
- Essential performance converts costly raised/glass presentation to solid where appropriate;
- Efficient performance reduces glass to raised presentation;
- Reduced Motion or Essential performance resolves to minimal motion;
- large/extra-large text may make density yield to reflow;
- keyboard-first, screen-reader-optimized, strong-focus, or increased-contrast context requires strong visible focus;
- inherited 2 dp focus ring width/offset is preserved as the source reference;
- all authoritative privacy, security, permission, capability, connectivity, recovery, and workflow truth remains outside Glaze.

Caller/platform context defaults are neutral. The migration does not pretend Android has supplied an accessibility or performance preference when it has not.

### Launcher appearance

System, Light, Dark, and Deep Dark remain Launcher-owned structural palettes presented under V1.6 semantics. Retained Deep Teal/Soft Amber atmosphere is decorative application presentation only. It cannot establish semantic or protected state.

Theme Manager consumes the V1.6 presentation context for material-cost simplification while preserving the existing fail-closed settings destination and caller-owned persistence boundary.

## Still required for V1.6 application acceptance

- authoritative Android accessibility/performance context wiring where applicable;
- complete component/state/material review across Home, Apps, Settings, Theme Manager, dialogs, workspace editing, and search entry points;
- Reduced Motion, Reduced Transparency, increased contrast, native forced-color equivalents, and degradation-order validation;
- 200% text/reflow, localization expansion, RTL, TalkBack, Switch Access, keyboard/D-pad, focus order, and announcement validation;
- responsive phone/tablet/foldable composition;
- representative physical-device Theme Manager navigation/persistence and issue #80 HOME/drawer behavior;
- measured performance/power fallback evidence;
- rollback verification;
- Human Visual Excellence review;
- applicable nine-system platform acceptance;
- protected signing/distribution, release approval, and lifecycle qualification.

## Historical boundary and rollback

The prior V1.1 source mapping remains immutable Development provenance. It is not rewritten as V1.6 acceptance.

The verified pre-migration Launcher main `5656ad908113dc3fabe362e06d825cdfd9de0cea` remains historical source rollback provenance. Any actual rollback requires fresh governed validation and does not redefine the current shared Glaze target.
