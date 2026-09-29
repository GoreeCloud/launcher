#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
METRICS = ROOT / "app/src/main/java/com/goreecloud/launcher/ui/theme/GlazeMetrics.kt"
CURRENT_AUTHORITY = ROOT / "app/src/main/java/com/goreecloud/launcher/ui/theme/GlazeCurrentAuthority.kt"
POLICY = ROOT / "app/src/main/java/com/goreecloud/launcher/ui/theme/GlazeV16PresentationPolicy.kt"
ATMOSPHERE = ROOT / "app/src/main/java/com/goreecloud/launcher/ui/theme/GlazeAtmosphere.kt"
THEME = ROOT / "app/src/main/java/com/goreecloud/launcher/ui/theme/GlazeTheme.kt"
THEME_MANAGER = ROOT / "app/src/main/java/com/goreecloud/launcher/ui/theme/ThemeManagerSurface.kt"
THEME_CATALOG = ROOT / "app/src/main/java/com/goreecloud/launcher/ui/theme/GlazeThemeManagerCatalog.kt"
ADOPTION = ROOT / "docs/glaze-ui-adoption.md"
DEVELOPMENT = ROOT / "docs/development/saveable-theme-manager-settings-composition.md"
PLATFORM = ROOT / "goreecloud.platform.yaml"

TARGET_VERSION = "1.6.0"
SOURCE_REVISION = "a7180679ea851389e0f3004515f9a25f420e716d"


def fail(message: str) -> None:
    raise SystemExit(f"GLAZE UI V1.6 Launcher source mapping failed: {message}")


def read(path: Path, label: str) -> str:
    if not path.is_file():
        fail(f"missing {label}: {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def require(text: str, marker: str, label: str) -> None:
    if marker not in text:
        fail(f"{label} missing `{marker}`")


def main() -> None:
    metrics = read(METRICS, "native metric map")
    authority = read(CURRENT_AUTHORITY, "current Glaze authority")
    policy = read(POLICY, "V1.6 presentation policy")
    atmosphere = read(ATMOSPHERE, "Launcher atmosphere")
    theme = read(THEME, "native theme")
    manager = read(THEME_MANAGER, "Theme Manager")
    catalog = read(THEME_CATALOG, "Theme Manager catalog")
    adoption = read(ADOPTION, "V1.6 adoption record")
    development = read(DEVELOPMENT, "Theme Manager development record")
    platform = read(PLATFORM, "Platform Contract manifest")

    for marker in (
        f'const val targetVersion = "{TARGET_VERSION}"',
        f'const val sourceRevision = "{SOURCE_REVISION}"',
        "val inheritedCoarseTargetFloor: Dp = 44.dp",
        "val inheritedPointerCompactFloor: Dp = 32.dp",
        "val minimumTarget: Dp = 48.dp",
        "val touchAssistanceTarget: Dp = 56.dp",
        "space5: Dp = 20.dp // Launcher-owned convenience.",
        "space10: Dp = 40.dp // Launcher-owned convenience.",
    ):
        require(metrics, marker, "GlazeMetrics")

    for marker in (
        f'const val currentRequiredVersion = "{TARGET_VERSION}"',
        f'const val currentStableSourceRevision = "{SOURCE_REVISION}"',
        "fun sourceMigrationRequired(): Boolean",
        "fun consumerAcceptanceRequired(): Boolean",
        "const val currentConsumerConformanceEstablished = false",
    ):
        require(authority, marker, "GlazeCurrentAuthority")

    for marker in (
        f'const val stableVersion = "{TARGET_VERSION}"',
        f'const val stableSourceRevision = "{SOURCE_REVISION}"',
        "GlazeV16MaterialRole.FUNCTIONAL_GLASS",
        "GlazeV16MaterialRole.CLEAR_GLASS",
        "GlazeV16MaterialRole.SOLID",
        "GlazeV16PerformanceLevel.ESSENTIAL",
        "GlazeV16PerformanceLevel.EFFICIENT",
        "GlazeV16MotionMode.MINIMAL",
        "context.reducedTransparency",
        "context.reducedMotion",
        "context.largeText || context.extraLargeText",
        "context.keyboardFirst",
        "staticCompositionLocalOf",
    ):
        require(policy, marker, "V1.6 presentation policy")

    for marker in (
        "Launcher-owned decorative atmospheric primitives",
        "never establish protection, privacy, identity",
    ):
        require(atmosphere, marker, "Launcher atmosphere authority boundary")

    for marker in (
        "Launcher-owned structural palettes mapped under the GLAZE UI V1.6 semantic presentation model",
        "LocalGlazeV16PresentationContext provides effectivePresentationContext",
    ):
        require(theme, marker, "GlazeTheme")

    for marker in (
        "GlazeV16PresentationPolicy.resolve(",
        "LocalGlazeV16PresentationContext.current",
        "current GLAZE UI V1.6 source mapping",
        "Launcher appearance under V1.6 presentation semantics",
    ):
        require(manager, marker, "Theme Manager")

    for marker in (
        "GLAZE UI V1.6 semantics",
        "System",
        "Light",
        "Dark",
        "Deep Dark",
    ):
        require(catalog, marker, "Theme Manager catalog")

    for marker in (
        "Status: **Development source mapping integrated / application acceptance pending**",
        f"Exact Stable release source authority: `{SOURCE_REVISION}`",
        "sourceMigrationRequired()",
        "consumerAcceptanceRequired()",
        "44 px-equivalent coarse target floor",
        "Reduced Transparency converts glass requests to solid presentation",
        "Production eligible on the Glaze UI gate: **no**",
    ):
        require(adoption, marker, "V1.6 adoption record")

    for marker in (
        "Status: Development — GLAZE UI V1.6 source mapping integrated",
        SOURCE_REVISION,
        "Reduced Transparency can force glass to solid",
        "System, Light, Dark, and Deep Dark are reachable",
    ):
        require(development, marker, "Theme Manager Development record")

    for marker in (
        'schema_version: "0.4"',
        '  glaze_ui:\n    result: applicable-blocked\n    version: "1.6.0"',
        "GlazeV16PresentationPolicy.kt",
        "GlazeV16PresentationPolicyTest.kt",
        '  glaze_ui_required: "1.6.0"',
        "glaze-ui==1.6.0",
        "conformance:\n  status: nonconformant",
        "consumer acceptance remains blocked",
    ):
        require(platform, marker, "Platform Contract manifest")

    stale_active_markers = {
        "metrics": (metrics, 'targetVersion = "1.1.0"'),
        "authority": (authority, 'implementedBaselineVersion = "1.1.0"'),
        "platform": (platform, 'version: "1.1.0"'),
        "theme-manager": (manager, "current V1.1 structural mapping"),
        "catalog": (catalog, "GLAZE UI V1.1"),
    }
    for label, (content, stale) in stale_active_markers.items():
        if stale in content:
            fail(f"{label} retains stale active marker `{stale}`")

    print(
        "GLAZE UI V1.6 Launcher source mapping passed: exact Stable provenance, "
        "presentation-only policy, current Platform Contract target, and fail-closed "
        "application-acceptance boundary are synchronized."
    )


if __name__ == "__main__":
    main()
