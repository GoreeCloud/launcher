#!/usr/bin/env python3
"""GoreeCloud Launcher contact-search policy and SQLite wildcard regression.

Uses synthetic contacts only. Verifies source invariants and actual SQLite LIKE
escaping; it does NOT replace Android instrumentation, contacts consent, or
representative-device search acceptance.
"""
from pathlib import Path
import sqlite3

ROOT = Path(__file__).resolve().parents[1]
src = (ROOT / "lawnchair/src/app/lawnchair/search/algorithms/engine/provider/ContactsSearchProvider.kt").read_text()
prefs = (ROOT / "lawnchair/src/app/lawnchair/preferences/PreferenceManager.kt").read_text()

checks = {
    "contacts remain opt-in": 'searchResultPeople = BoolPref("pref_searchResultPeople", false' in prefs,
    "contacts permission gate": "checkPermission(context)" in src and "Manifest.permission.READ_CONTACTS" in src,
    "literal LIKE escape": 'DISPLAY_NAME + " LIKE ? ESCAPE \'!\'"' in src,
    "bound search results": "max.coerceIn(0, MAX_VISIBLE_CONTACTS)" in src and "MAX_VISIBLE_CONTACTS = 10" in src,
    "bound scanned rows": "scannedRows++ < MAX_SCANNED_ROWS" in src and "MAX_SCANNED_ROWS = 512" in src,
    "bound query size": "normalizedQuery.length > MAX_QUERY_LENGTH" in src and "MAX_QUERY_LENGTH = 128" in src,
    "cooperative cancellation": "currentCoroutineContext().ensureActive()" in src,
    "cancellation propagated": "catch (e: CancellationException)" in src and "throw e" in src,
    "no query-bearing diagnostics": 'Log.e("ContactSearch"' not in src and
        'Log.w("ContactSearch", "Local contacts unavailable")' in src,
    "escape exclamation": 'query.replace("!", "!!")' in src,
    "escape percent": '.replace("%", "!%")' in src,
    "escape underscore": '.replace("_", "!_")' in src,
}
for title, passed in checks.items():
    if not passed:
        raise SystemExit("FAIL: " + title)

def escape_like(term: str) -> str:
    return term.replace("!", "!!").replace("%", "!%").replace("_", "!_")

with sqlite3.connect(":memory:") as db:
    db.execute("CREATE TABLE contacts (display_name TEXT NOT NULL)")
    db.executemany("INSERT INTO contacts VALUES (?)", [
        ("Alex_One",), ("AlexXOne",), ("50% Deal",), ("500 Deal",),
        ("A!B",), ("AB",), ("Amy",),
    ])
    for query, expected in [
        ("Alex_", ["Alex_One"]),
        ("50%", ["50% Deal"]),
        ("A!", ["A!B"]),
        ("Amy", ["Amy"]),
    ]:
        actual = [row[0] for row in db.execute(
            "SELECT display_name FROM contacts WHERE display_name LIKE ? ESCAPE '!' ORDER BY display_name",
            ("%" + escape_like(query) + "%",),
        )]
        if actual != expected:
            raise SystemExit(f"FAIL: literal LIKE query {query!r}: {actual!r} != {expected!r}")

print("PASS: opted-in contact search is bounded, cancellable and literal-wildcard safe in source")
print("PASS: synthetic SQLite wildcard matching")
print("Device contacts-provider and accessibility acceptance remain pending")
