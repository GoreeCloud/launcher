#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOC = ROOT / "docs/glaze-motion-evaluation.md"
TEST = ROOT / "app/src/test/java/com/goreecloud/launcher/core/workspace/GlazeMotionExperimentalConsumerTest.kt"
MAIN = ROOT / "app/src/main"
LAUNCHER_ROOT = ROOT / "app/src/main/java/com/goreecloud/launcher/ui/LauncherRoot.kt"
REFERENCE_REVISION = "e8f68770540d00499b5613a00310ac7002a674fd"
MARKER = "GlazeMotionExperimental"


def fail(message: str) -> None:
    raise SystemExit(f"Glaze Motion evaluation failed: {message}")


def main() -> None:
    for path in (DOC, TEST, LAUNCHER_ROOT):
        if not path.is_file():
            fail(f"missing required evidence: {path.relative_to(ROOT)}")

    doc_text = DOC.read_text(encoding="utf-8")
    test_text = TEST.read_text(encoding="utf-8")
    launcher_text = LAUNCHER_ROOT.read_text(encoding="utf-8")

    required_doc = [
        "Lifecycle: Experimental 0.4",
        f"Reviewed canonical revision: `{REFERENCE_REVISION}`",
        "Evaluation mode: native Android semantic mapping, test-only",
        "Production dependency: no",
        "Glaze UI 2.2.0 Stable is the production design-system authority.",
        "insufficient for Candidate promotion by itself",
        "Glaze Motion remains Experimental and test-only",
    ]
    for evidence in required_doc:
        if evidence not in doc_text:
            fail(f"missing lifecycle or evidence boundary `{evidence}`")

    for stale in (
        "Glaze UI 2.1 Stable is the production design-system authority.",
        "Glaze UI 2.0 Stable is the production design-system authority.",
        "Glaze UI 1.6 Stable remains the production design-system authority.",
        "Glaze UI 1.5 Stable remains the production design-system authority.",
    ):
        if stale in doc_text:
            fail(f"stale production-authority boundary remains active: `{stale}`")

    required_test = [
        f'const val REFERENCE_REVISION = "{REFERENCE_REVISION}"',
        "WorkspaceCodec.moved(",
        "WorkspaceCodec.movedToTarget(",
        "WorkspaceMoveDirection.EARLIER",
        "WorkspaceMoveDirection.LATER",
        "allowsOptionalSettling(",
        "reducedMotion",
        "maximumConcurrentSettling",
    ]
    for evidence in required_test:
        if evidence not in test_text:
            fail(f"missing test-only semantic evidence `{evidence}`")

    required_consumer = [
        "detectDragGestures(",
        "onDragCancel = {",
        "onMoveFavorite",
        "onMoveDock",
        "onMoveFavoriteToTarget",
        "onMoveDockToTarget",
    ]
    for evidence in required_consumer:
        if evidence not in launcher_text:
            fail(f"representative Launcher ordering surface no longer exposes `{evidence}`")

    production_hits = []
    for path in MAIN.rglob("*.kt"):
        if MARKER in path.read_text(encoding="utf-8"):
            production_hits.append(str(path.relative_to(ROOT)))
    if production_hits:
        fail(
            "Experimental mapping escaped test quarantine into production source: "
            + ", ".join(production_hits)
        )

    print(
        "Glaze Motion 0.4 Launcher historical test-only evaluation boundary passed under Glaze UI 2.2.0 Stable: "
        "real ordering domain mapped, production source remains quarantined."
    )


if __name__ == "__main__":
    main()
