# Rendered Theme Manager settings navigation

Status: Development

This slice adds a rendered Settings destination host on top of the validated saveable `LauncherSettingsNavigation` model.

`LauncherSettingsDestinationHost` renders the existing root Settings content for `ROOT` and the first-party `ThemeManagerSurface` for `THEME_MANAGER`. Theme selection still delegates to the caller-provided theme persistence callback; the destination host itself gains no workspace, launcher-role, or persistence authority.

## Remaining integration step

`LauncherBetaRoot` still needs to replace its direct Appearance theme-cycle action with saveable destination state that calls this host. That wiring is intentionally kept separate from this bounded rendered-host slice so the large root surface is not rewritten without its own review and build evidence.

This is Development evidence only and is not a Stable navigation acceptance claim.
