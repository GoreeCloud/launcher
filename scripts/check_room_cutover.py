#!/usr/bin/env python3
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PRODUCTION_ROOT = ROOT / "app" / "src" / "main" / "java"
MAIN_ACTIVITY = PRODUCTION_ROOT / "com" / "goreecloud" / "launcher" / "MainActivity.kt"
AUTHORITY_REPOSITORY = PRODUCTION_ROOT / "com" / "goreecloud" / "launcher" / "core" / "workspace" / "WorkspaceRepository.kt"
PROMOTION_COORDINATOR = PRODUCTION_ROOT / "com" / "goreecloud" / "launcher" / "core" / "workspace" / "db" / "WorkspaceProductionPromotionCoordinator.kt"
POST_CUTOVER_STARTUP_COORDINATOR = PRODUCTION_ROOT / "com" / "goreecloud" / "launcher" / "core" / "workspace" / "db" / "WorkspacePostCutoverStartupCoordinator.kt"
AUTHORITATIVE_PLACEMENT_REPOSITORY = PRODUCTION_ROOT / "com" / "goreecloud" / "launcher" / "core" / "workspace" / "db" / "WorkspaceAuthoritativePlacementRepository.kt"
AUTHORITATIVE_PLACEMENT_OBSERVER = PRODUCTION_ROOT / "com" / "goreecloud" / "launcher" / "core" / "workspace" / "db" / "WorkspaceAuthoritativePlacementObserver.kt"
PRODUCTION_RUNTIME_COORDINATOR = PRODUCTION_ROOT / "com" / "goreecloud" / "launcher" / "core" / "workspace" / "db" / "WorkspaceProductionRuntimeCoordinator.kt"
PLACEMENT_REPOSITORY = PRODUCTION_ROOT / "com" / "goreecloud" / "launcher" / "core" / "workspace" / "db" / "WorkspaceRoomPlacementRepository.kt"
PRIMARY_HOME_SPATIAL_REPOSITORY = PRODUCTION_ROOT / "com" / "goreecloud" / "launcher" / "core" / "workspace" / "db" / "WorkspacePrimaryHomeSpatialRepository.kt"

NON_EXECUTABLE_KOTLIN = re.compile(
    r'""".*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|/\*.*?\*/|//[^\n]*',
    re.DOTALL,
)


def executable_kotlin(text: str) -> str:
    """Remove comments and literals so documentation text cannot satisfy or trip code guards."""
    return NON_EXECUTABLE_KOTLIN.sub("", text)


errors: list[str] = []

for path in PRODUCTION_ROOT.rglob("*.kt"):
    text = executable_kotlin(path.read_text(encoding="utf-8"))
    promotion_calls = text.count("promoteRoomAuthority(")
    if path == AUTHORITY_REPOSITORY:
        if promotion_calls != 1:
            errors.append(
                "WorkspaceRepository.kt must contain exactly the guarded promotion primitive definition."
            )
    elif path == PROMOTION_COORDINATOR:
        if promotion_calls != 1:
            errors.append(
                "WorkspaceProductionPromotionCoordinator.kt must contain exactly one guarded promotion call."
            )
    elif promotion_calls:
        errors.append(
            f"Production Room promotion call is outside the reviewed coordinator: {path.relative_to(ROOT)}"
        )

    coordinator_references = text.count("WorkspaceProductionPromotionCoordinator(")
    if path == PROMOTION_COORDINATOR:
        if coordinator_references != 1:
            errors.append(
                "WorkspaceProductionPromotionCoordinator.kt must contain exactly its class declaration."
            )
    elif path == PRODUCTION_RUNTIME_COORDINATOR:
        if coordinator_references != 1:
            errors.append(
                "WorkspaceProductionRuntimeCoordinator.kt must instantiate the production promotion coordinator exactly once."
            )
    elif coordinator_references:
        errors.append(
            "Production promotion activation is outside WorkspaceProductionRuntimeCoordinator: "
            f"{path.relative_to(ROOT)}"
        )

    startup_references = text.count("WorkspacePostCutoverStartupCoordinator(")
    if path == POST_CUTOVER_STARTUP_COORDINATOR:
        if startup_references != 1:
            errors.append(
                "WorkspacePostCutoverStartupCoordinator.kt must contain exactly its class declaration."
            )
    elif path == PRODUCTION_RUNTIME_COORDINATOR:
        if startup_references != 1:
            errors.append(
                "WorkspaceProductionRuntimeCoordinator.kt must instantiate post-cutover startup recovery exactly once."
            )
    elif startup_references:
        errors.append(
            "Post-cutover startup activation is outside WorkspaceProductionRuntimeCoordinator: "
            f"{path.relative_to(ROOT)}"
        )

    authoritative_references = text.count("WorkspaceAuthoritativePlacementRepository(")
    if path == AUTHORITATIVE_PLACEMENT_REPOSITORY:
        if authoritative_references != 1:
            errors.append(
                "WorkspaceAuthoritativePlacementRepository.kt must contain exactly its class declaration."
            )
    elif path == PRODUCTION_RUNTIME_COORDINATOR:
        if authoritative_references != 1:
            errors.append(
                "WorkspaceProductionRuntimeCoordinator.kt must instantiate the authoritative placement router exactly once."
            )
    elif authoritative_references:
        errors.append(
            "Authoritative placement routing is outside WorkspaceProductionRuntimeCoordinator: "
            f"{path.relative_to(ROOT)}"
        )

    observer_references = text.count("WorkspaceAuthoritativePlacementObserver(")
    if path == AUTHORITATIVE_PLACEMENT_OBSERVER:
        if observer_references != 1:
            errors.append(
                "WorkspaceAuthoritativePlacementObserver.kt must contain exactly its class declaration."
            )
    elif path == PRODUCTION_RUNTIME_COORDINATOR:
        if observer_references != 1:
            errors.append(
                "WorkspaceProductionRuntimeCoordinator.kt must instantiate the authoritative placement observer exactly once."
            )
    elif observer_references:
        errors.append(
            "Authoritative placement observation is outside WorkspaceProductionRuntimeCoordinator: "
            f"{path.relative_to(ROOT)}"
        )

    runtime_references = text.count("WorkspaceProductionRuntimeCoordinator(")
    if path == PRODUCTION_RUNTIME_COORDINATOR:
        if runtime_references != 1:
            errors.append(
                "WorkspaceProductionRuntimeCoordinator.kt must contain exactly its class declaration."
            )
    elif path == MAIN_ACTIVITY:
        if runtime_references != 1:
            errors.append(
                "MainActivity.kt must instantiate WorkspaceProductionRuntimeCoordinator exactly once."
            )
    elif runtime_references:
        errors.append(
            "Production workspace runtime activation is outside MainActivity: "
            f"{path.relative_to(ROOT)}"
        )

    spatial_references = text.count("WorkspacePrimaryHomeSpatialRepository(")
    if path == PRIMARY_HOME_SPATIAL_REPOSITORY:
        if spatial_references != 1:
            errors.append(
                "WorkspacePrimaryHomeSpatialRepository.kt must contain exactly its class declaration."
            )
    elif path == PRODUCTION_RUNTIME_COORDINATOR:
        if spatial_references != 1:
            errors.append(
                "WorkspaceProductionRuntimeCoordinator.kt must instantiate primary Home spatial authority exactly once."
            )
    elif spatial_references:
        errors.append(
            "Primary Home spatial access is outside WorkspaceProductionRuntimeCoordinator: "
            f"{path.relative_to(ROOT)}"
        )

    placement_references = text.count("WorkspaceRoomPlacementRepository(")
    if path == PLACEMENT_REPOSITORY:
        if placement_references != 1:
            errors.append(
                "WorkspaceRoomPlacementRepository.kt must contain exactly its class declaration."
            )
    elif path == AUTHORITATIVE_PLACEMENT_REPOSITORY:
        if placement_references != 1:
            errors.append(
                "WorkspaceAuthoritativePlacementRepository.kt must contain exactly one guarded Room placement repository instantiation."
            )
    elif placement_references:
        errors.append(
            "Production Room placement access is outside the reviewed authoritative router: "
            f"{path.relative_to(ROOT)}"
        )

if errors:
    print("Room cutover guard failed:")
    for error in errors:
        print(f"- {error}")
    raise SystemExit(1)

print(
    "Room cutover guard passed: production activation is confined to MainActivity -> "
    "WorkspaceProductionRuntimeCoordinator -> reviewed promotion, recovery, observation, and routing components."
)
