# Launcher Transition Performance Instrumentation

Status: Development measurement infrastructure  
Scope: Home -> Apps drawer and Apps drawer -> Home transitions  
Production telemetry: none

## Purpose

This harness gives GoreeCloud Launcher a repeatable source-controlled way to collect Android frame timing around the existing Home and Apps transitions. It exists to support issue #80 performance stabilization and regression investigation.

The harness does not change Launcher transition timing, application inventory authority, GoreeCloud Index authority, workspace state, networking, telemetry, package visibility, profile scope, or production behavior.

## Measurement path

The Android instrumentation test:

1. launches the normal Development `MainActivity`;
2. waits for the existing Launcher Home surface to become active;
3. performs two unmeasured Home <-> Apps warm-up round trips;
4. performs six measured round trips using real injected vertical touch gestures;
5. samples `Window.FrameMetrics.TOTAL_DURATION` while each transition is active;
6. reports frame count, median, p95, maximum, frames above 16.67 ms, and frames above 33.34 ms for each direction.

The process-local `LauncherTransitionDiagnostics` helper exposes only the current surface name and an observation counter in debug builds. It stores nothing durably, sends nothing over the network, does not participate in product authority, and is ignored by release builds.

## CI

The dedicated `transition-performance-emulator` job runs the single performance instrumentation class on an Android 16 x86_64 emulator with system animations enabled.

The existing `room-runtime-emulator` lane remains separate and keeps its deterministic animation-disabled behavior for Room/recovery runtime validation.

## Acceptance boundary

Emulator measurements are diagnostic regression evidence only. They are not representative-device performance acceptance and must not be used by themselves to close Launcher issue #80, establish GLAZE UI consumer performance acceptance, approve Quickstep/Recents behavior, or support Release Candidate, production, or Stable qualification.

The 16.67 ms and 33.34 ms counts are descriptive 60 Hz / 30 Hz reference bands, not repository pass/fail budgets. A governed representative-device budget must be applied to real supported hardware and refresh-rate context before performance acceptance is claimed.

The instrumentation test fails only when the normal Home/Apps gestures stop reaching their expected surfaces or when Android emits no frame timing samples, because those conditions make the measurement invalid.
