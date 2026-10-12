# Glaze 1.7.0 — Android launcher adaptation

GoreeCloud/glaze is authoritative for the current **Glaze V1.7 / 1.7.0** Stable-compatibility design baseline. This document describes **Development mapping**, not acceptance of the launcher as a Glaze consumer.

The native entrypoint is `app.lawnchair.ui.theme.LawnchairTheme` (retained API compatibility while the source is migrated). It provisions `LocalGlazeMetrics` and continues to use native Material Expressive wallpaper-aware colors. `GlazeAndroidContract.preferenceSurface` provides a stable semantic settings-container role. Native system semantics take precedence over mock web/CSS styling.

| Glaze role | Native mapping | Required review |
| --- | --- | --- |
| Canvas and app surface | Material 3 `surface`, `onSurface` | Contrast in dark/light/dynamic wallpaper |
| Grouped settings | Glaze `preferenceSurface`: `surfaceContainerLow/High` | Surface grouping and contrast |
| Text, secondary content | `onSurface`, `onSurfaceVariant` | TalkBack and scalable type |
| Focus and selection | `primary`, `outline`, `outlineVariant` | Keyboard focus, contrast and visibility |
| Minimum interactive target | `GlazeMetrics.minimumTouchTarget = 48.dp` | Apply to each interactive icon/control |
| Shape | Compact 12dp, cards 20dp, sheets 28dp | Check native affordances and layout breakpoints |
| Motion | Native Material expressive scheme (temporary) | Reduced-motion, input latency and power audit |
| Adaptivity | Android layout/window system (upstream) | Portrait/landscape/tablet/foldable tests |

The presence of tokens or a version string does not establish Glaze conformance. Each user-facing screen and interaction must be audited and accepted against the live Glaze consumer registry, accessibility, privacy/security, performance and representative runtime evidence.
