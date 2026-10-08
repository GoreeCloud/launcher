# Theme Manager settings navigation

Status: Development

This slice adds the saveable destination model needed to wire the already-implemented native Theme Manager surface into Launcher Settings without overloading the Home/Apps top-level surface state.

The model currently recognizes Settings root and Theme Manager destinations. Unknown persisted values fail closed to Settings root.

## Authority boundary

Navigation state owns presentation destination only. It does not change theme persistence, Glaze UI acceptance, launcher role, workspace placement, icon packs, wallpaper access, Privacy Shield, or any other platform authority.

The existing Theme Manager still supports only the currently implemented System, Light, and Dark appearance modes. This slice does not imply Deep Dark, wallpaper-derived palettes, icon masking, icon-pack discovery, or full Glaze UI 2.1 application acceptance.

## Next composition step

Launcher Settings can replace its current appearance-cycle button with an entry into `ThemeManagerSurface`, persist the settings sub-destination through this model, and return cleanly to Settings root when Theme Manager closes.
