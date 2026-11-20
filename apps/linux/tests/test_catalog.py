import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from catalog import discover


class StubInfo:
    def __init__(self, desktop_id, name, description="", shown=True):
        self.id = desktop_id
        self.name = name
        self.description = description
        self.shown = shown

    def get_id(self):
        return self.id

    def get_display_name(self):
        return self.name

    def get_name(self):
        return self.name

    def get_description(self):
        return self.description

    def should_show(self):
        return self.shown


class DiscoveryTests(unittest.TestCase):
    def test_visible_deduplicated_and_sorted(self):
        results = discover([
            StubInfo("b.desktop", "Browser"),
            StubInfo("a.desktop", "Editor"),
            StubInfo("b.desktop", "Browser (duplicate)"),
            StubInfo("secret.desktop", "Hidden", shown=False),
            StubInfo("invalid", "No ID"),
        ])
        self.assertEqual([item["id"] for item in results], ["b.desktop", "a.desktop"])
        self.assertEqual(len(results), 2)

    def test_empty_name_excluded(self):
        self.assertEqual(discover([StubInfo("empty.desktop", "")]), [])


if __name__ == "__main__":
    unittest.main()
