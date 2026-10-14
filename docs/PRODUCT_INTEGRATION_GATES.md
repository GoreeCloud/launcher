# GoreeCloud Launcher — integration and Home acceptance

## Interim web policy
Brave Search is the sole web-search provider. The selector maps any legacy provider ID to Brave. Search destinations use `https://search.brave.com/search?q=...` with URI-encoded queries. Local queries are not transmitted automatically: external navigation requires choosing the explicit web result. Remote web suggestions are disabled.

## Future integration gates
Do not enable GoreeCloud Search until both GoreeCloud Search **and GoreeCloud Index** have verified Stable status and security, privacy, availability, and acceptance evidence. Once both qualify, permanently replace Brave, including legacy provider migration and QA. Integrate GoreeCloud AI in Universal Search **only** when GoreeCloud AI independently qualifies as Stable; it must have explicit source enablement, privacy controls, and distinct AI result treatment. These gates are not met or bypassed by source compilation.

## Personal and Work badges
Home, dock and drawer icon rendering uses compact P (Personal) and W (Work/non-primary Android profile) badges. Defaults to top left; bottom-left and bottom-right are available. Notification dots retain the top-right corner. Search results enumerate authorized Android user profiles. Device QA is needed for managed/clone profiles and themed icons.

## Home overview and Feeds
Empty-area Home long-press enters scaled edit overview with a preview strip, page navigation, page reordering, default page, and controls for the bundled GoreeCloud wallpaper picker, widgets, Add page, Settings and Done. Added pages persist using Workspace's screen-order API. Empty secondary pages can be removed; populated pages require content relocation to avoid data loss. The first page remains protected. Feeds UI, bridge, client, preferences and overlay connection have been removed; external minus-one overlay attachment is not allowed.

## Acceptance outstanding
Physical device: preview correctness, page persistence across restart/reload, 4/5/6 grid and screen widths, Work profile badges/permissions, notifications collision, Brave-only network traffic, long-press and gesture priority, keyboard/TalkBack navigation, landscape and foldable behavior. Verify compilation, APK package/permissions/signing before installation. Development-only pending gates.

## Mockup-aligned Home edit and app context acceptance (Development)
Empty-area long-press enters the scaled live Home workspace with neighboring pages, accessible Home/dot/Add markers, a page toolbar (set default, safely remove blank page, finish), and three footer actions: Wallpapers & themes, Widgets, Home screen settings. Empty secondary pages alone are removable; moving content is required before deleting a populated page. The icon long-press menu uses Android's genuine Select/edit mode, Remove shortcut, Edit customization, App info, published app shortcuts and a separate, profile-matched notification card only when notification access and device-unlocked privacy conditions permit. Availability of controls depends on source Android APIs; real-device visual accuracy, TalkBack, landscape, drag/drop and notification consent are unverified until tested.
