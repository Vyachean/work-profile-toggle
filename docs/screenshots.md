# Screenshots

## Goal

The repository should include stable screenshots in README so users can understand the app before installing it.

Stable screenshots should be committed under:

```text
docs/screenshots/
```

README should link committed images, not temporary CI artifacts.

## Current UI foundation

The application now uses a Compose Material 3 runtime Home screen.

The screenshot prerequisites already available are:

- deterministic `HomeUiState` models;
- a pure `HomeScreen` composable driven by state and typed events;
- state-specific Compose previews;
- compact-screen preview coverage;
- no requirement for a real work profile when rendering the composable from fake state.

The remaining work is screenshot-test infrastructure and reviewed baselines, not UI-state extraction.

## Recommended implementation sequence

1. Create a small proof of concept with a Compose-compatible deterministic screenshot test tool.
2. Reuse the existing fake Home states and add any missing screenshot-only states.
3. Render each state with fixed dimensions, density, font scale, locale, light/dark appearance, and time zone.
4. Compare generated images against reviewed baselines.
5. Upload generated PNG files and comparison reports as CI artifacts.
6. Review selected images for product correctness, not only pixel stability.
7. Commit approved images under `docs/screenshots/`.
8. Link approved images from README.

Do not lock the project to a screenshot library before a minimal proof of concept demonstrates stable Compose rendering in the repository's Gradle and CI environment.

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

## CI policy

Screenshot generation may begin as a non-blocking artifact job while the harness is being calibrated.

It should become a required comparison check only after:

- rendering is deterministic across repeated CI runs;
- font and platform dependencies are controlled;
- baseline update rules are documented;
- failures provide usable image diffs;
- maintainers can intentionally approve baseline changes.

## Anti-patterns

Avoid:

- `adb screencap` from an uncontrolled hosted emulator state;
- screenshots that depend on a real work profile, current wall-clock time, device locale, or OEM theme;
- temporary CI artifacts linked directly from README;
- approving pixel changes without checking the represented product state;
- keeping obsolete View-specific screenshot guidance after the runtime UI is Compose.
