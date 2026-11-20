# GoreeCloud Launcher — Native Linux

Qt Quick 6.12.0 graphical launcher using PySide6. It discovers installed XDG-visible applications through `Gio.AppInfo.get_all()` and launches selected `Gio.DesktopAppInfo` objects with `.launch([], None)`; it does not evaluate `Exec` in a shell.

## Dependencies

- Linux desktop with Wayland or X11; Python >=3.10; PySide6 == 6.12.0
- GLib GIO via PyGObject (usually distribution package `python3-gi`); select a Python environment that exposes distro GIO bindings.
- On Debian/Ubuntu: `sudo apt install python3-gi gir1.2-gio-2.0` (package names may vary); install Qt wheels into a venv configured to access the bindings if appropriate.

From root:

```sh
python3 -m venv --system-site-packages .venv
. .venv/bin/activate
python -m pip install -r apps/linux/requirements.txt
python apps/linux/main.py
```

Run `python -m unittest discover -s apps/linux/tests` for isolated metadata tests.

This project is an **application launcher window**, not a compositor, desktop shell, taskbar, login screen or system-level replacement for GNOME/KDE. User-installed .desktop files may themselves execute powerful programs; use only trusted applications. GPL/LGPL/commercial license obligations for redistributing PySide6/Qt must be reviewed before packaging. Runtime behavior is not yet verified on real hardware.
