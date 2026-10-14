#!/usr/bin/env python3
"""GoreeCloud Launcher visual/first-run source contracts (not device QA)."""
from pathlib import Path
r = Path(__file__).resolve().parents[1]
setup = (r / "lawnchair/src/app/lawnchair/GoreeCloudSetupActivity.kt").read_text()
home = (r / "lawnchair/src/app/lawnchair/GoreeCloudHomeOnboarding.kt").read_text()
about = (r / "lawnchair/src/app/lawnchair/ui/preferences/about/About.kt").read_text()
for key in ("homeColumns", "dockColumns", "drawerColumns"):
    assert f'private var {key} = 5' in setup
    assert f'state.putInt("{key}", {key})' in setup
    assert f'state?.getInt("{key}")' in setup
assert 'for (columns in 4..6)' in setup
assert all(t in setup for t in (
    'gridChoice(content, "Home screen", homeColumns)',
    'gridChoice(content, "Dock", dockColumns)',
    'gridChoice(content, "App drawer", drawerColumns)',
))
assert 'grid.workspaceColumns.set(homeColumns)' in setup
assert 'grid.hotseatColumns.set(dockColumns)' in setup
assert 'prefs.drawerColumns.set(drawerColumns)' in setup
assert 'onPreferencesChanged(this@GoreeCloudSetupActivity)' in setup
assert 'STEP ${page + 1} OF 6' in setup
assert 'page == 5' in setup
assert 'it % columns, it / columns' in home
assert 'position % columns' in home and 'position / columns' in home
assert 'text = "GoreeCloud Launcher"' in about
assert 'GoreeCloud/launcher' in about
assert 'AboutLicenses' in about
assert 'uiState.coreTeam' not in about and 'uiState.topLinks' not in about
assert 'LawnchairLauncher/lawnchair' not in about
assert 'https://lawnchair.app' not in about
assert 'R.drawable.goreecloud_launcher_foreground' in about
print("PASS: 4/5/6 onboarding grids, scalable placement, GoreeCloud About, retained licenses")
