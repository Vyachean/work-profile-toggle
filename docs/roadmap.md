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

## Product direction

The main product direction is to provide a Digital Wellbeing-style work-profile schedule on devices where the built-in Google/OEM schedule feature is missing or unavailable.

Manual pause/resume remains important, but it is the setup and fallback path for the larger schedule-focused product.

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

## Stage 5 — Work-profile schedule runtime

Goal: implement the core schedule behavior: work profile active during configured work hours and paused outside configured work hours.

This should fill the product gap on devices where the built-in Digital Wellbeing/OEM work-profile schedule is absent.

Design baseline:

- Use the [schedule runtime design](schedule-runtime.md) as the implementation contract.
- Prefer state reconciliation over fire-and-forget toggles.
- Calculate next boundaries from local schedule settings each time the runtime runs.
- Persist structured schedule results for diagnostics.

Open decisions:

- Whether the first runtime should use exact or inexact alarms.
- How to communicate reliability limits to the user.
- Whether schedule support should require a foreground notification or another visible user affordance.
- How manual pause/resume should interact with the next scheduled boundary.

Required before implementation:

- Tests for schedule calculation and edge cases.
- Clear UX for disabled or unreliable schedule support.
- Clear diagnostics for missed or blocked schedule changes.

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
- Implement schedule runtime from the documented design.
- Keep issue #25 aligned with this roadmap.
