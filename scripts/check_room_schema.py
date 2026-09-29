#!/usr/bin/env python3
import json
from pathlib import Path

SCHEMA_ROOT = Path("app/schemas")
REQUIRED_TABLES = {"workspace_pages", "workspace_items"}


def main() -> None:
    schemas = sorted(SCHEMA_ROOT.rglob("1.json"))
    if not schemas:
        raise SystemExit("Room schema guard failed: no version-1 schema was exported under app/schemas.")

    matched = []
    for schema_path in schemas:
        try:
            payload = json.loads(schema_path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            raise SystemExit(f"Room schema guard failed: cannot parse {schema_path}: {exc}") from exc

        serialized = json.dumps(payload, sort_keys=True)
        tables = {table for table in REQUIRED_TABLES if table in serialized}
        if tables == REQUIRED_TABLES:
            matched.append(schema_path)

    if not matched:
        raise SystemExit(
            "Room schema guard failed: exported version-1 schemas do not contain "
            "workspace_pages and workspace_items."
        )

    print("Room schema guard passed.")
    for path in matched:
        print(f"Validated schema: {path}")


if __name__ == "__main__":
    main()
