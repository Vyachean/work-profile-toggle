# Screenshots

## Goal

The repository should include stable screenshots so users can understand the app before installing it and maintainers can review user-visible UI changes.

Documentation screenshots should be committed under:

```text
docs/screenshots/
```

README should link committed images, not temporary CI artifacts.

## Current foundation

The application uses a Compose Material 3 runtime Home screen.

The screenshot prerequisites available are:

- deterministic `HomeUiState` models;
- a pure `HomeScreen` composable driven by state and typed events;
- no requirement for a real work profile when rendering from fake state;
- the official experimental Compose Preview Screenshot Testing plugin `0.0.1-alpha15`;
- a dedicated `screenshotTest` source set that keeps fixtures out of debug and release APKs.

The plugin is still alpha. Keep the integration isolated and reevaluate it if an upgrade introduces material build or rendering instability.

## Current calibration matrix

The active-profile and enabled-schedule fixture is rendered in four controlled configurations:

- phone light: 360 dp by 800 dp, font scale `1.0`;
- phone dark: 360 dp by 800 dp, font scale `1.0`;
- compact light: 320 dp by 480 dp, font scale `1.0`;
- large-font light: 360 dp by 800 dp, font scale `1.5`.

Every configuration also fixes:

- API level 30, so fallback Material colors are used instead of dynamic color;
- density at 420 dpi;
- portrait orientation;
- English locale;
- UTC host timezone;
- `ubuntu-24.04`, JDK 17, and a stable UTF-8 process locale.

The fixture represents a reachable product state: the work profile is active, the complete schedule is enabled, exact-alarm access is granted, and a fixed next Pause action is present.

## CI calibration

The non-blocking `Compose screenshot calibration` job:

1. removes generated reference output from previous tasks;
2. renders all previews with `updateDebugScreenshotTest`, build cache disabled, and tasks forced to rerun;
3. calculates stable, path-relative SHA-256 hashes for every generated PNG;
4. compares them with the reviewed manifest in `.github/compose-screenshot-baselines.sha256`;
5. uploads generated PNG files, expected and actual hashes, and available test reports for 14 days.

The hash manifest was produced by an earlier workflow run after all four images were reviewed visually. A later run comparing new renders with that committed manifest checks stability across workflow boundaries rather than only twice inside one process.

This hash comparison is a calibration mechanism, not the final golden-image workflow. It detects every byte change but does not provide an expected/actual/diff image report by itself.

## Calibration update rules

Do not update the reviewed hash manifest merely to make CI green.

For an intentional screenshot change:

1. Keep the UI or fixture change in a draft PR.
2. Let CI render the new images and upload the calibration artifact.
3. Inspect every changed PNG for product correctness, including reachable state, wording, theme, spacing, wrapping, clipping, and scroll behavior.
4. Confirm that an unexpected dependency, host, locale, or time input did not cause the change.
5. Update `.github/compose-screenshot-baselines.sha256` only after the visual change is accepted.
6. Keep the accepted hash update in the same PR as the corresponding UI, fixture, or rendering-environment change.

Renaming a `@PreviewTest` function or preview changes generated filenames and must be treated as a baseline migration, not a harmless refactor.

## Next implementation steps

1. Add representative setup, disabled, blocked, enabled, and runtime-issue states using the shared fixture architecture.
2. Commit reviewed PNG reference images under `app/src/screenshotTestDebug/reference/`.
3. Replace hash-only calibration with `validateDebugScreenshotTest` expected/actual/diff reporting.
4. Prove stable validation across repeated clean CI runs and dependency-cache states.
5. Make screenshot comparison a required check only after the alpha tool and baseline workflow are operationally reliable.
6. Select user-facing images, copy them to `docs/screenshots/`, and link them from README.

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

At least release-candidate states should be checked in light, dark, compact, increased-font, and normal-phone configurations.

Dynamic color should not be used for golden baselines unless the complete color scheme is controlled deterministically. Stable fallback themes are preferable for documentation screenshots.

## Required-gate policy

Screenshot comparison must not become a required merge gate until:

- rendering is deterministic across repeated CI runs, not only inside one run;
- runner image, API level, viewport, density, locale, font scale, theme, and relevant clock inputs are controlled;
- approved PNG references are versioned;
- validation failures provide usable expected, actual, and diff outputs;
- baseline updates are intentional and reviewable;
- maintainers can recover from plugin or Layoutlib regressions without blocking unrelated runtime fixes.

A green hash or pixel comparison establishes rendering stability, not that the represented product state is correct.

## Anti-patterns

Avoid:

- `adb screencap` from an uncontrolled hosted emulator state;
- screenshots that depend on a real work profile, current wall-clock time, device locale, or OEM theme;
- temporary CI artifacts linked directly from README;
- enabling a required screenshot gate before cross-run stability is demonstrated;
- approving baseline changes without checking the represented product state;
- updating hashes solely to silence a failure;
- keeping test fixtures in production or debug APK source sets.
