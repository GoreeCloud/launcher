# GoreeCloud Launcher — Home Screen Dock

Requirement level: Mandatory  
Product: GoreeCloud Launcher  
Lifecycle: Development  
Document type: Repository-native product specification  
Authority boundary: This document defines Dock product requirements and records implementation state; it does not by itself establish runtime, release, Production, Stable, Seal, or Anchor acceptance.

## Product definition

The Home Screen Dock is a persistent, customizable application, folder, shortcut, and Launcher-action surface positioned along the primary Home-screen edge. It remains independent from individual Home-page content so frequently used destinations stay reachable while the user changes Home pages.

A fresh starter workspace begins with five preferred application roles when matching apps are available: Phone, Messages, Email/Mail, Browser, and Camera. Those five items are a starter configuration, never a capacity limit.

## Capacity and Dock pages

The Dock must support more items than fit safely in one row. It must preserve the resolved GoreeCloud interaction floor instead of shrinking touch targets below an accessible size. When the available width cannot safely present the requested density, Launcher must introduce horizontal Dock navigation or overflow first.

Multiple Dock pages are an approved capability. Dock-page navigation is independent from Home-page navigation. A typical organization may use separate Everyday, Productivity, and Media pages while retaining one ordered, persistent Dock collection.

The user may choose a preferred items-per-page density. Device width, orientation, display posture, accessibility scale, and resolved minimum target size can reduce the effective number of visible slots so the interaction floor remains intact. Optional page looping is user-controlled in the current Development candidate. It uses bounded virtual edge pages and returns immediately to the equivalent logical edge page after a completed swipe so the visible page order remains predictable.

## Applications, folders, shortcuts, and actions

The Dock is intended to support individual applications, Launcher folders, supported shortcuts, and Launcher-owned actions. Folder presentation must reuse the Launcher folder model and Glaze styling rather than creating a second folder system. Profile-qualified Android identity must remain intact for all app-backed items.

The current Development workspace authority persists ordered application Dock membership. Dock-folder persistence and non-app Dock item authority remain planned and must not be represented as implemented until Room/workspace evidence proves them.

## Drag, drop, and reordering

Dock content participates in the Launcher direct-manipulation workspace model. Users must be able to add apps from Apps, move supported items between Home and Dock, reorder Dock items with exact insertion behavior, remove Dock placement without uninstalling the application, and keep unrelated Home content unchanged.

Layout Lock must prevent Dock placement mutation while leaving normal launching available. Non-gesture management alternatives must remain available for accessibility.

## Adaptive layout and icon geometry

The Dock must adapt to phone, tablet, landscape, large-display, and supported foldable compositions. Launcher-owned interactive targets retain at least the resolved 48 dp minimum floor under the normal policy. Visual artwork may scale within its bounded presentation range before interaction targets are reduced.

Dock icons use the same normalized icon pipeline, selected adaptive mask, profile badge rules, and optical geometry as Home, Apps, Search, and folder previews. Labels, when enabled, reserve consistent geometry and use bounded single-line presentation rather than shifting neighboring slots.

## Universal Search integration

The Dock may expose a direct GoreeCloud Launcher Universal Search affordance. That affordance opens the same Launcher-owned Search surface used by swipe-down and fixed/movable Home Search modes. It must remain useful for local sources without requiring a GoreeCloud account, network connection, advertising service, sponsored provider, or remote Search backend.

Enabling a Dock Search affordance does not implicitly enable connected providers or sensitive local sources. Existing provider consent, permission, file-root, and connected-account controls remain authoritative.

## Styling

The Dock participates in the Glaze Theme Engine and may provide Clear/transparent, Glaze, Solid, Raised, and Edge treatments. Wallpaper-aware translucency must remain selective and must fail safely to an accessible solid/raised presentation when reduced transparency or presentation policy requires it.

Dock styling must preserve icon contrast, labels, badges, focus, drag feedback, and page-navigation visibility in light and dark compositions.

## Notification badges and profiles

Notification badges remain optional and permission-gated. Dock functionality must not depend on notification-listener access. Badge content is minimized, local, profile-aware, non-advertising, and non-tracking.

Personal, Work, and other supported Android profile identities must never be collapsed merely because two applications share a package label or visual brand. Profile identity governs launch, badges, shortcuts, App Lock interaction, and workspace state.

## Privacy

The Dock is local-first. Ordering, page presentation, local favorites, folders, and presentation preferences remain on-device by default. The Dock does not require advertising SDKs, sponsored placement, affiliate ranking, behavioral tracking, remote analytics, a GoreeCloud account, or cloud connectivity. Payment or sponsorship must never influence Dock placement.

## Persistence, backup, and recovery

Dock organization is persistent workspace state. Long-term backup/recovery requirements include ordered identities, page assignment or deterministic page reconstruction, folder membership, relevant appearance preferences, and safe handling of missing apps/profiles. Current portable-preference v1 does not yet claim complete Dock-page/folder restoration; versioned recovery expansion remains planned.

## Accessibility

Dock navigation and management must support minimum target sizes, TalkBack semantics, Switch Access, clear focus order, large text, reduced motion, reduced transparency, sufficient contrast, predictable non-gesture alternatives, and supported phone/tablet/foldable layouts.

## Development checkpoint — October 4, 2026

Draft PR #239 implements the first adaptive paged-Dock tranche on authoritative main `33c7a995e4d47f497f9fdbf569618c33dc521a77`:

- unbounded ordered application Dock remains the workspace authority;
- preferred 4/5/6/7 items-per-page density with automatic earlier paging when the interaction floor would be violated;
- horizontally swipeable Dock pages independent from Home pages;
- optional looping across the first/last Dock page, disabled by default;
- compact page indicators and page accessibility state;
- optional Dock labels;
- optional direct Universal Search affordance that can keep the Dock visible even when it contains no app placements;
- Clear, Glaze, Solid, Raised, and Edge material choices;
- existing Home↔Dock drag/reorder, layout-lock, icon-mask, profile, and local persistence paths are retained;
- focused page-planning and preference tests are included.

This checkpoint is Development source only until exact-head protected build/JVM/lint/schema, Android 16 runtime, transition-performance, migration-provenance, Foundation, required-gate, and protected-promotion evidence succeeds. Representative-device visual/accessibility/form-factor acceptance also remains open.

## Remaining Dock work

Dock folders and other non-app Dock item persistence; direct cross-page drag handoff while a drag is active; deeper Dock padding/edge-position controls; context-aware or suggested Dock content; Dock widgets; versioned portable backup/restore expansion; and complete representative-device accessibility, profile, performance/power, recovery, protected Development signing/update continuity, and release qualification remain planned or gated.
