# Glaze Motion Experimental Evaluation — GoreeCloud Launcher

## Evaluation target

- Design system: `GoreeCloud/goreecloud-glaze-ui`
- Glaze Motion tier: Motion Core
- Lifecycle: Experimental 0.4
- Reviewed canonical revision: `e8f68770540d00499b5613a00310ac7002a674fd`
- Consumer: GoreeCloud Launcher
- Evaluation mode: native Android semantic mapping, test-only
- Production dependency: no

This record evaluates whether the historical Glaze Motion 0.4 semantics can map onto Launcher's existing ordering domain. It is downstream development evidence only. It does not activate Experimental Glaze Motion in the production application, certify Launcher, or promote Glaze Motion to Candidate or Stable.

## Real consumer surface

Launcher already has two ordering paths for Favorites and Dock items:

- explicit move-earlier/move-later commands backed by `WorkspaceMoveDirection` and `WorkspaceCodec.moved()`;
- direct drag/drop backed by `WorkspaceCodec.movedToTarget()` through the same application-owned workspace ordering domain.

The direct-manipulation UI clears transient drag state on cancellation. Workspace truth changes only when an application ordering command is invoked.

## Evaluated semantic mapping

`GlazeMotionExperimentalConsumerTest.kt` maps the following Experimental Motion Core concepts to Launcher's real workspace operations without adding production source code:

- Previous/Next reorder commands map to the existing EARLIER/LATER workspace domain operations.
- First/Last reorder commands map to the existing stable-key target operation.
- Results expose `fromIndex`, `toIndex`, one-based `position`, `total`, and `changed` metadata rather than hard-coded announcement text.
- Missing stable keys fail closed and cannot invent a workspace transition.
- Optional settling is rejected under reduced motion or when the local concurrent-settling budget is saturated.
- Semantic ordering completes independently of any optional animation or settling decision.

Consumer-localized accessibility announcements remain the responsibility of Launcher. This evaluation intentionally does not prescribe English announcement copy.

## Production quarantine

The Experimental mapping exists only under `app/src/test`. `scripts/check_glaze_motion_evaluation.py` fails if the `GlazeMotionExperimental` marker appears anywhere under `app/src/main`.

The guard also verifies that the current production consumer still exposes both the explicit non-drag ordering path and direct-manipulation cancellation semantics before the evaluation is accepted as representative evidence.

No JavaScript runtime, web artifact, external dependency, analytics, telemetry, or third-party motion library is introduced.

## Known acceptance gaps

Launcher is not yet a conformant Glaze Motion consumer. Before any production activation, at minimum the application still needs:

- centralized native mapping of the applicable motion roles rather than isolated Compose defaults or local constants;
- Android reduced-motion behavior tied to the appropriate platform accessibility setting;
- rendered and physical-device verification of direct manipulation, cancellation, focus, semantics, and reordered-state feedback;
- localized assistive feedback for ordering position changes where appropriate;
- representative-device frame pacing, input latency, power, and settling-workload evidence;
- confirmation that optional motion never delays or controls workspace state;
- application-specific compatibility and rollback review.

Existing drag transforms, alpha, and scale feedback are therefore treated as application behavior under evaluation, not as proof of Glaze Motion conformance.

## Promotion boundary

This historical evaluation remains evidence that a first-party consumer mapped Experimental motion semantics against Launcher's actual ordering domain. It is insufficient for Candidate promotion by itself and does not establish adoption of the current Glaze Motion Experimental revision.

Glaze UI 2.2.0 Stable is the production design-system authority. Glaze Motion remains Experimental and test-only; Motion Studio and Motion Spatial remain outside this historical evaluation.
