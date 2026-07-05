# Roadmap

This roadmap records the current development direction. It should be updated when priorities, scope, or implementation decisions change.

## Current baseline

The app currently supports manual and scheduled work-profile control after ADB setup:

- setup-first Home screen;
- persisted selected work profile;
- pause, resume, and toggle actions;
- dynamic launcher shortcuts;
- legacy shortcut picker for automation apps;
- persisted last manual action result;
- saved schedule settings;
- schedule boundary calculation for same-day and overnight work windows;
- inexact Android alarm scheduling for the next schedule boundary;
- bounded asynchronous schedule-boundary reconciliation through a broadcast receiver;
- persisted last schedule runtime result;
- user-facing schedule runtime status with next action or issue;
- schedule rescheduling after app update, device reboot, manual time change, and timezone change;
- CI debug APK artifacts;
- release APK workflow infrastructure.

## Product direction

The main product direction is to provide a Digital Wellbeing-style work-profile schedule on devices where the built-in Google/OEM work-profile schedule is missing or unavailable.

Manual pause/resume remains important, but it is the setup, fallback, and explicit override path for the larger schedule-focused product.

## Stage 1 — Documentation and product clarity

Goal: make the repository understandable without reading issue or chat history.

Planned work:

- Keep README as the short public entry point.
- Keep product model, roadmap, setup, release, schedule runtime, and screenshot/testing plans under `docs/`.
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
- Keep schedule runtime status derived from structured state rather than direct view logic.

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
- Clear schedule runtime status and issue recovery guidance.
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

This fills the product gap on devices where the built-in Digital Wellbeing/OEM work-profile schedule is absent.

Implemented baseline:

- State reconciliation model instead of fire-and-forget toggles.
- Local schedule calculation from saved days/start/end settings.
- Same-day and overnight active windows.
- Invalid and incomplete schedule blocking.
- Inexact Android alarm scheduling for the next boundary.
- Alarm receiver that reconciles the selected work profile to the expected state.
- Next-boundary rescheduling after each handled boundary.
- Runtime result persistence for diagnostics and UI status.
- Rescheduling after reboot, app update, manual time change, and timezone change.

Current limitations:

- Runtime behavior still needs repeated real-device validation across Android/OEM variants.
- The first implementation uses inexact alarms, so Android battery restrictions may delay boundaries.
- Exact-alarm mode is not enabled.
- Direct Boot support is not enabled; the app uses normal credential-protected app storage.
- Schedule UI is functional but not yet a polished setup-first flow.

Remaining work before this stage is complete:

- Manual smoke test on a real device with an actual work profile.
- Stronger unit coverage for `AndroidScheduleWorkProfileReconciler` failure paths.
- Better setup/status guidance for permission missing, selected profile missing, credential required, and Android request rejected states.
- Diagnostics wording and copyable advanced details for blocked schedule runs.
- Decide whether exact alarms are worth the Android special-access UX cost.

Status: in progress.

## Stage 6 — Release readiness

Goal: make the app safe to publish and easy to install.

Planned work:

- Configure release signing secrets.
- Produce the first signed release APK.
- Document release process and versioning rules.
- Validate install/update path between debug and release builds.
- Keep ADB permission setup clear for advanced users.
- Validate the schedule runtime on at least one real device before publishing it as a stable feature.

Status: planned.

## Known follow-ups

- Keep issue #25 aligned with this roadmap.
- Add `AndroidScheduleWorkProfileReconciler` unit tests.
- Run and document a real-device schedule smoke test.
- Improve schedule setup/status UX.
- Create stable README screenshots after screenshot architecture exists.
- Decide between Compose Material 3 and Material Components Views.
- Add stronger automated coverage for schedule editor validation.
- Improve diagnostics wording and copyable error details.
