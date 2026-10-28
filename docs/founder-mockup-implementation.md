# GoreeCloud Launcher — Founder mockup implementation tracking

**Scope:** Three-screen founder-provided reference: Home, app drawer and Home edit. **Status:** Native development candidate originated in [PR #30](https://github.com/GoreeCloud/launcher/pull/30), with wallpaper correction in [PR #32](https://github.com/GoreeCloud/launcher/pull/32) and physical-phone UI fixes in [PR #44](https://github.com/GoreeCloud/launcher/pull/44). All remain in the unmerged feature stack, **not** a Sealed release. The supplied visual is an aesthetic reference, not evidence that any illustrated capability is functional.

| Mockup element | Current implementation | Evidence needed |
| --- | --- | --- |
| Home wallpaper and visual direction | The initial violet artificial gradient was **retired** in PR #32. The native candidate uses actual device wallpaper, a dark fallback and optional original offline GoreeCloud wallpapers | Device-wallpaper contrast, OEM/system-bar behavior, performance/battery review |
| Clock and date cards | Native device-time `TextClock` and date; no invented third-party data | Rotation, accessibility, midnight refresh |
| Weather, calendar events and screen-time cards | **Not implemented as live data providers.** Android widget selection permits compatible external widgets where installed. No false weather, events or usage totals are shown | Provider integration, permission/data-use rules, user opt-in |
| App shortcuts and installed app drawer | Actual Android launchable apps; 4-column default, local search; real swipe-up transition | Emulator runtime, device screenshot, search and gesture acceptance |
| Persistent 5-icon dock | Previous native pin ordering/persistence + transparent mockup-style lens and empty-slot picker | Physical device, adaptive sizing, TalkBack |
| Home workspace pages | Existing editable page model and current-app shortcuts, plus native overview and live page controls | Real-device touch/keyboard, rearrangement and landscape |
| Edit mode | Native `HomeEditActivity` with wallpaper intent, appearance modes, grid, labels, dock and swipe toggles | QA UI hierarchy/screenshots, Android wallpaper resolver matrix |
| Android widgets | `AppWidgetHost`/`AppWidgetManager` system picker, provider configuration, persisted widget IDs, remove-with-confirmation | Actual widget providers, binding/cancel/restart/reconfiguration matrix |
| Folders, arbitrary icon reordering, widget resizing, sophisticated themes | **Still open**; bounded native drag-to-place shortcuts and system widget hosting do not imply complete editing or resizing | Implement and separately validate |

**Safety and honesty:** This update creates no generated images or screenshot assets in the app. All installed app icons are loaded from the corresponding Android package. There are no mock weather readings, calendar appointments or screen-time totals. The QA APK is signed with an ephemeral debug key and a per-run isolated app ID; it is not a managed upgrade/release. Retain issue #1 for physical-device and release-acceptance gates and issue #2 for missing advanced launcher functions. Keep Home defaults under explicit Android consent; do not change the user's current launcher or remove retained app data as part of QA.

## Physical-device visual QA and provenance — October 27, 2026

I reviewed actual phone screenshots of a development QA build. They revealed oversized drawer spacing, a poorly structured three-step onboarding experience, stacked Settings buttons, and a sort menu obscuring app icons. The native fixes in [PR #44](https://github.com/GoreeCloud/launcher/pull/44) subsequently passed all twelve Android workflows and were merged into the **feature branch**, with screenshot-led verification of the dense app drawer, onboarding at 200% system text size, and safe insets. This is not evidence of complete physical-device acceptance, GC UI compliance, or a distributed release. Current task and acceptance tracking remain [issue #1](https://github.com/GoreeCloud/launcher/issues/1) and [issue #2](https://github.com/GoreeCloud/launcher/issues/2).
