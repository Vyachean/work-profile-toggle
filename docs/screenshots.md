# Screenshots

## Goal

The repository should eventually include stable screenshots in README so users can understand the app before installing it.

Stable screenshots should be committed under:

```text
docs/screenshots/
```

README should link committed images, not temporary CI artifacts.

## Why screenshots need a test harness

The app UI depends on Android work-profile state and protected Android APIs. A normal hosted GitHub Actions emulator does not provide a real work profile, and real device/OEM state is not deterministic.

For this reason, screenshots should be generated from deterministic UI states rather than from whatever state a hosted emulator happens to have.

## Recommended approach

1. Extract UI state for Home and Schedule screens.
2. Add fake/demo states for screenshot scenarios.
3. Render those states in screenshot tests.
4. Upload generated PNG files as CI artifacts.
5. Review selected PNG files.
6. Commit stable images into `docs/screenshots/`.
7. Link committed images from README.

## Candidate scenarios

- Setup required.
- No work profile found.
- Work profile active.
- Work profile paused.
- Work profile state unknown.
- Schedule not configured.
- Schedule configured but disabled.
- Schedule configured and enabled.
- Advanced diagnostics collapsed or secondary.

## Candidate tools

- Roborazzi with Robolectric for View-based deterministic screenshots.
- Gradle Managed Devices for instrumented screenshots when emulator coverage is required.
- Paparazzi if the UI is later refactored into components that can be rendered cleanly without Activity/system-service dependencies.

## Anti-patterns

Avoid using required CI checks that only run `adb screencap` against a live hosted emulator without a deterministic test state. Such screenshots are brittle and do not represent the product states the README should show.
