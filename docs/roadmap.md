# Roadmap

This roadmap records the current development direction. It should be updated when priorities, scope, or implementation decisions change.

## Current baseline

The app currently supports manual work-profile control after ADB setup:

- setup-first Home screen;
- persisted selected work profile;
- pause, resume, and toggle actions;
- dynamic launcher shortcuts;
- legacy shortcut picker for automation apps;
- persisted last action result;
- saved schedule settings;
- CI debug APK artifacts;
- release APK workflow infrastructure.

## Stage 1 — Documentation and product clarity

Goal: make the repository understandable without reading issue or chat history.

Planned work:

- Keep README as the short public entry point.
- Keep product model, roadmap, setup, release, and screenshot/testing plans under `docs/`.
- Update documentation in the same PR as behavior or scope changes.
- Document known platform limitations and OEM variance.

Status: in progress.

## Stage 2 — UI architecture cleanup

Goal: make UI states deterministic, testable, and suitable for stable screenshots.

Planned work:

- Extract Home UI state from direct Activity/system-service rendering.
- Extract Schedule UI state and schedule editor state.
- Keep Android system-service calls behind controllers/repositories.
- Make primary UI use product terms: work profile, active, paused, pause, resume, schedule, setup.
- Keep raw Android terms in Advanced/Diagnostics only.

Status: planned.

## Stage 3 — Modern Android UI

Goal: replace the current utility-style View UI with a more polished Android UI.

Options:

- Jetpack Compose + Material 3 for a modern UI foundation.
- Material Components with Views if an incremental migration is preferred.

Expected work:

- Material-style Home screen sections.
- Clear setup state.
- Clear selected work-profile state.
- Schedule settings as a settings-style form.
- Better incomplete-schedule validation and guidance.
- Accessibility, font-scale, light/dark theme, and system inset handling.

Status: planned.

## Stage 4 — Deterministic screenshots

Goal: generate stable screenshots for documentation.

Planned work:

- Add fake/demo UI states for screenshot scenarios.
- Add screenshot tests through Roborazzi/Robolectric or another deterministic test tool.
- Generate PNG artifacts in CI.
- Review generated images before committing stable screenshots to `docs/screenshots/`.
- Link committed screenshots from README.

Avoid:

- relying on ad-hoc emulator `screencap` output as a required CI check;
- documenting screenshots through expiring GitHub Actions artifacts;
- depending on a real Android work profile in hosted CI.

Status: planned.

## Stage 5 — Schedule runtime

Goal: make saved schedule settings actually execute pause/resume actions.

Open decisions:

- Which Android scheduling mechanism should be used.
- How to handle Doze, OEM background restrictions, reboot, timezone changes, and missed schedule events.
- How to communicate reliability limits to the user.
- Whether schedule execution should require foreground notification or other visible user affordances.

Required before implementation:

- A documented runtime design.
- Tests for schedule calculation and edge cases.
- Clear UX for disabled or unreliable schedule execution.

Status: planned.

## Stage 6 — Release readiness

Goal: make the app safe to publish and easy to install.

Planned work:

- Configure release signing secrets.
- Produce the first signed release APK.
- Document release process and versioning rules.
- Validate install/update path between debug and release builds.
- Keep ADB permission setup clear for advanced users.

Status: planned.

## Known follow-ups

- Create stable README screenshots after screenshot architecture exists.
- Decide between Compose Material 3 and Material Components Views.
- Add stronger automated coverage for schedule editor validation.
- Improve diagnostics wording and copyable error details.
- Keep issue #25 aligned with this roadmap.
