# Saveable Theme Manager Settings Composition

Status: Development — GLAZE UI V1.6 source mapping integrated; application acceptance pending

`LauncherSettingsSurface` composes the validated saveable Settings destination model with `LauncherSettingsDestinationHost`. Theme persistence remains with the caller-provided `GlazeThemeRepository` through `onSelectThemeMode`; the presentation layer gains no Home/Apps navigation, workspace placement, launcher-role, wallpaper, icon-pack, account, or system-setting authority.

## V1.6 source mapping

The branch pins exact Official Stable GLAZE UI V1.6 / `1.6.0` source `a7180679ea851389e0f3004515f9a25f420e716d`.

`GlazeMetrics` preserves the inherited Stable spacing subset used by Launcher and records the inherited 44 dp-equivalent coarse target floor while keeping Launcher's stricter 48 dp normal target and 56 dp accessibility-oriented target. Product-owned 20/40 dp spacing, radii, optical aliases, pigments, and atmosphere remain explicitly non-canonical.

`GlazeV16PresentationPolicy` models V1.6 material/accessibility/performance behavior without inferring authoritative state. Reduced Transparency can force glass to solid, Essential performance can reduce costly material and motion, Reduced Motion yields minimal motion, large text may make density yield to reflow, and strong input/accessibility contexts preserve visible focus.

`GlazeTheme` propagates caller-supplied presentation context through nested previews. Neutral defaults remain neutral until a caller/platform supplies actual state.

Theme Manager is **Application** settings content. Durable explanatory/settings content remains certainty-first, while presentation policy may simplify optional material cost without changing meaning, action hierarchy, persistence, or authority.

## Current bounded behavior

- System, Light, Dark, and Deep Dark are reachable.
- Only a different appearance choice invokes caller-owned persistence.
- Stale/unknown Settings destination state fails closed to Settings root.
- The selected appearance is represented as selected state, not as an actionable duplicate.
- The preview atmosphere is decorative and carries no semantic state.
- Theme Manager can resolve raised material to solid when authoritative presentation context requests Reduced Transparency or Essential performance.

## Acceptance boundary

This source mapping does not establish V1.6 application conformance. Authoritative runtime accessibility/performance wiring, rendered accessibility, localization/RTL, representative form factors/devices, issue #80 physical HOME/drawer behavior, measured performance/power, rollback, Human Visual Excellence, platform-system acceptance, signing/distribution, Release Candidate, production, and Stable qualification remain separate gates.
