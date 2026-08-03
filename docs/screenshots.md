# Screenshots

## Goal

The repository should include stable screenshots in README so users can understand the app before installing it.

Stable screenshots should be committed under:

```text
docs/screenshots/
```

README should link committed images, not temporary CI artifacts.

## Current UI foundation

The application uses a Compose Material 3 runtime Home screen.

The screenshot prerequisites available are:

- deterministic `HomeUiState` models;
- a pure `HomeScreen` composable driven by state and typed events;
- state-specific Compose previews;
- compact-screen preview coverage;
- no requirement for a real work profile when rendering the composable from fake state.

## Current proof of concept

The repository uses the official experimental Compose Preview Screenshot Testing tool as a provisional proof of concept.

Current setup:

- plugin and validation API version `0.0.1-alpha15`;
- dedicated `screenshotTest` source set;
- one debug-only fixture for an active work profile with an enabled schedule;
- one light-mode screenshot preview;
- fixed API level 30 so the fallback Material color scheme is used instead of dynamic color;
- fixed viewport of 360 dp by 800 dp at 420 dpi;
- fixed English locale and font scale `1.0`;
- pinned `ubuntu-24.04` CI runner with `TZ=UTC` and a stable UTF-8 locale.

The `Compose screenshot POC` CI job:

1. removes previous generated references;
2. renders the preview with `updateDebugScreenshotTest` and build cache disabled;
3. records PNG SHA-256 hashes;
4. removes the generated references again;
5. performs a second clean render;
6. compares both hash lists;
7. uploads both renders, hashes, and available test reports as a 14-day artifact.

The first CI proof completed successfully. Both independent renders were byte-identical and the generated image was reviewed visually.

The screenshot job remains non-blocking while cross-run stability and baseline workflow are being calibrated. No golden reference image is committed yet.

The tool is still in alpha. Keep the integration isolated and reevaluate it if plugin upgrades introduce material build or rendering instability.

## Implementation sequence

Completed:

1. Prove that the official Compose screenshot tool compiles and renders in the current AGP, Kotlin, JDK, and CI environment.
2. Fix the initial viewport, density, API level, font scale, locale, light appearance, runner image, and host timezone.
3. Prove byte-identical output from two clean renders in one CI run.
4. Upload generated PNG files and hashes as a non-blocking CI artifact.

Next:

1. Repeat the same scenario across separate workflow runs and dependency-cache states.
2. Extract shared screenshot fixtures rather than duplicating state construction as scenarios grow.
3. Add the required light, dark, compact, and increased-font scenarios.
4. Add representative setup, blocked, disabled, enabled, and runtime-issue states.
5. Define baseline review and update rules.
6. Commit approved reference images and enable `validateDebugScreenshotTest` as a required comparison check only after stability is demonstrated.
7. Commit selected documentation images under `docs/screenshots/` and link them from README.

## Required scenarios

Minimum user-facing baseline set:

- No work profile found.
- Work profile selection required.
- Permission setup required.
- Work profile active.
- Work profile paused.
- Work profile state unknown.
- Schedule not configured.
- Incomplete schedule.
- Complete schedule disabled.
- Schedule enabled with next action.
- Schedule blocked by missing exact-alarm access.
- Schedule runtime issue.
- Advanced card visible as a secondary surface.

## Required display variants

At least the release-candidate states should be checked in:

- light appearance;
- dark appearance;
- compact phone width and height;
- increased font scale;
- a normal phone viewport.

Dynamic color should not be used for golden baselines unless the test controls the complete color scheme deterministically. Stable fallback themes are preferable for documentation screenshots.

## Baseline policy

Reference images must not become a required merge gate until:

- rendering is deterministic across repeated CI runs, not only twice inside one run;
- runner image, API level, viewport, density, locale, font scale, theme, and relevant clock inputs are controlled;
- baseline update rules are documented;
- validation failures provide usable actual, expected, and diff outputs;
- maintainers can intentionally review and approve baseline changes.

A baseline update must be reviewed as a user-visible change. A green hash or pixel comparison does not establish that the represented product state is correct.

## Anti-patterns

Avoid:

- `adb screencap` from an uncontrolled hosted emulator state;
- screenshots that depend on a real work profile, current wall-clock time, device locale, or OEM theme;
- temporary CI artifacts linked directly from README;
- enabling a required screenshot gate before cross-run stability is demonstrated;
- approving pixel changes without checking the represented product state;
- keeping obsolete View-specific screenshot guidance after the runtime UI is Compose.
