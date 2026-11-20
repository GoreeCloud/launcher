#!/usr/bin/env python3
"""GoreeCloud Launcher native Linux Qt Quick GUI."""
from __future__ import annotations
import sys
from pathlib import Path

from catalog import discover


def main() -> int:
    try:
        import gi
        gi.require_version("Gio", "2.0")
        from gi.repository import Gio
        from PySide6.QtCore import QObject, Property, Signal, Slot, QUrl
        from PySide6.QtGui import QGuiApplication
        from PySide6.QtQml import QQmlApplicationEngine
    except (ImportError, ValueError) as exc:
        print(f"Missing native UI/GIO dependency: {exc}", file=sys.stderr)
        return 2

    class LauncherModel(QObject):
        entriesChanged = Signal()
        statusChanged = Signal()

        def __init__(self) -> None:
            super().__init__()
            self._entries: list[dict[str, str]] = []
            self._status = ""
            self.refresh()

        @Property("QVariantList", notify=entriesChanged)
        def entries(self) -> list[dict[str, str]]:
            return self._entries

        @Property(str, notify=statusChanged)
        def status(self) -> str:
            return self._status

        @Slot()
        def refresh(self) -> None:
            self._entries = discover(Gio.AppInfo.get_all())
            self.entriesChanged.emit()

        @Slot(str)
        def launch(self, desktop_id: str) -> None:
            if desktop_id not in {entry["id"] for entry in self._entries}:
                self._status = "Application is not in the visible installed catalog."
                self.statusChanged.emit()
                return
            try:
                info = Gio.DesktopAppInfo.new(desktop_id)
                if info is None or not info.should_show():
                    raise RuntimeError("Desktop entry is unavailable")
                if not info.launch([], None):
                    raise RuntimeError("The application declined the launch request")
                self._status = f"Opened {info.get_display_name()}"
            except (Exception) as exc:
                self._status = f"Unable to start application: {exc}"
            self.statusChanged.emit()

    app = QGuiApplication(sys.argv)
    app.setApplicationName("GoreeCloud Launcher")
    engine = QQmlApplicationEngine()
    launcher = LauncherModel()
    engine.rootContext().setContextProperty("launcher", launcher)
    engine.load(QUrl.fromLocalFile(str(Path(__file__).resolve().parent / "Main.qml")))
    if not engine.rootObjects():
        print("Failed to load GoreeCloud Launcher QML", file=sys.stderr)
        return 1
    return app.exec()


if __name__ == "__main__":
    raise SystemExit(main())
