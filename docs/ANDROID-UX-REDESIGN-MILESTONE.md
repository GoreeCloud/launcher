# GoreeCloud Launcher Android UX Redesign Milestone

Status: Active implementation target

This milestone follows the failed physical-device visual acceptance pass on 2026-09-07.

## Milestone goals

- Make Home read immediately as a finished launcher rather than a development/configuration surface.
- Reduce card stacking and visual chrome.
- Preserve wallpaper as the primary canvas.
- Make app drawer access natural and discoverable.
- Keep role/setup guidance compact and non-dominant.
- Preserve existing workspace persistence, dock/favorites state, search integration, and launcher-role behavior.

## Home layout target

- Remove the persistent `Launcher settings` hero-like button from the normal visual hierarchy; move Settings to a compact secondary affordance or long-press/overflow path.
- Remove the full-width `Apps` button from the bottom of Home; replace with swipe-up drawer opening and an optional small handle/chevron for discoverability.
- Make the default-home prompt compact and dismissible when the launcher is not yet the HOME role.
- Remove translucent label pills behind app names; labels should render directly over the wallpaper with contrast assistance only when necessary.
- Reduce search card height and visual mass. Swipe-down remains the primary GoreeCloud universal-search gesture.
- Reduce dock height and surface opacity while preserving touch targets.
- Keep app icons optically consistent and align them to the common GoreeCloud Android icon system.

## App drawer target

- Compact search field at the top.
- Dense, consistent grid with clear labels.
- Settings and Home become secondary controls rather than top-level visual peers with apps.
- Preserve long-press placement/favorite/dock actions.

## Acceptance

- Physical Nord N200 acceptance is mandatory before visual-stability promotion.
- HOME, Back, Recents, gesture navigation, and 3-button navigation must remain functional.
- Reboot must preserve launcher role and workspace state.
- Wallpaper crop, light/dark icon label contrast, and dock readability must be verified with GoreeCloud wallpapers.
