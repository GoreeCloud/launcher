"""Testable freedesktop application metadata helpers.

Do not parse Desktop Entry Exec or pass arbitrary user strings to a shell.
GIO handles XDG visibility and desktop entry activation on the runtime host.
"""
from __future__ import annotations
from typing import Iterable, Any


def discover(infos: Iterable[Any]) -> list[dict[str, str]]:
    seen: set[str] = set()
    entries: list[dict[str, str]] = []
    for info in infos:
        desktop_id = info.get_id() or ""
        if not desktop_id.endswith(".desktop") or desktop_id in seen or not info.should_show():
            continue
        name = (info.get_display_name() or info.get_name() or "").strip()
        if not name:
            continue
        seen.add(desktop_id)
        entries.append({
            "id": desktop_id,
            "name": name,
            "description": (info.get_description() or "")[:160],
        })
    return sorted(entries, key=lambda x: (x["name"].casefold(), x["id"]))
