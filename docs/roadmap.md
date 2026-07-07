# Roadmap

This roadmap records the current development direction. It should be updated when priorities, scope, or implementation decisions change.

## Current baseline

The app currently supports manual and scheduled work-profile control after setup:

- setup-first Home screen;
- persisted selected work profile;
- pause, resume, and toggle actions;
- dynamic launcher shortcuts;
- legacy shortcut picker for automation apps;
- persisted last manual action result;
- saved schedule settings;
- schedule boundary calculation for same-day and overnight work windows;
- exact Android alarm scheduling for the next schedule boundary;
- exact-alarm access checks and user-facing blocked state;
- bounded asynchronous schedule-boundary reconciliation through a broadcast receiver;
- persisted last schedule runtime result;
- user-facing schedule runtime status with next action or issue;
- copyable schedule runtime diagnostics for real-world reports;
- schedule rescheduling after app update, device reboot, manual time change, timezone change, selected-profile changes, schedule changes, and exact-alarm access changes;
- Jetpack Compose and Material 3 build baseline;
- Compose Home screen skeleton and previews;
- shared schedule date/time display formatter;
- CI debug APK artifacts;
- release APK workflow infrastructure.

## Product direction

The main product direction is to provide a Digital Wellbeing-style work-profile schedule on devices where the built-in Google/OEM work-profile schedule is missing or unavailable.

Manual pause/resume remains important, but it is the setup, fallback, and explicit override path for the larger schedule-focused product.

The next product phase is not just a visual refresh. The goal is to make the schedule-first product understandable and reliable: users should always see whether setup is complete, whether scheduling is blocked, what the next scheduled action is, and what recovery action is available.

## Development strategy

The near-term strategy is incremental migration, not a large rewrite:

1. Keep runtime behavior stable while refactoring UI boundaries.
2. Keep each PR small enough to review thoroughly and verify with CI.
3. Resolve review comments before merging.
4. Prefer pure state models, helpers, and previews before replacing Activity rendering.
5. Move to Compose Home wiring only after the existing View path is decomposed enough to keep callbacks and runtime behavior clear.

Quality rules for this phase:

- Do not merge without green CI for the exact head commit being merged.
- Do not merge with unresolved relevant review comments.
- Do not replace large files just to make a small behavior-neutral change.
- Avoid broad UI rewrites until preview, state, and callback wiring are ready.
- Keep documentation updated in the same PR as behavior, setup, or strategy changes.

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

Implemented baseline:

- Home UI state extracted from direct Activity/system-service rendering.
- Schedule editor UI state extracted from direct controls.
- Advanced UI state extracted for advanced/status sections.
- Shared schedule display formatter added for consistent next-action date/time text.

Remaining work:

- Keep Android system-service calls behind controllers/repositories.
- Make primary UI use product terms: work profile, active, paused, pause, resume, schedule, setup.
- Keep raw Android terms in Advanced/Diagnostics only.
- Keep schedule runtime status derived from structured state rather than direct view logic.
- Reduce `MainActivity` responsibilities before Compose Home wiring.

Status: in progress.

## Stage 3 — Compose Material 3 Home

Goal: replace the current utility-style View UI with a polished Compose Material 3 Home screen.

Direction:

- Jetpack Compose + Material 3 is the selected UI path.
- The current Compose Home screen is still a skeleton and preview surface, not the primary runtime UI.
- The View-based `MainActivity` path remains the runtime UI until Compose callbacks are wired safely.

Near-term work:

- Prepare `HomeScreenActions` wiring without changing runtime behavior.
- Decompose `MainActivity` render logic into smaller helpers where safe.
- Connect Compose Home to the existing `HomeUiState` and existing action handlers.
- Keep the old View path available until parity is verified.
- Verify manual pause/resume, setup states, exact-alarm blocked state, schedule enabled/disabled state, next action, and copy diagnostics on a real device.

Expected polish after wiring:

- Material-style Home screen sections.
- Clear setup checklist.
- Clear selected work-profile state.
- Schedule settings as a settings-style form.
- Better incomplete-schedule validation and guidance.
- Clear schedule runtime status and issue recovery guidance.
- Accessibility, font-scale, light/dark theme, and system inset handling.

Status: in progress.

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
- Exact Android alarm scheduling for the next boundary.
- Exact-alarm access status and setup guidance on Android versions that require it.
- Alarm receiver that reconciles the selected work profile to the expected state.
- Next-boundary rescheduling after each handled boundary.
- Runtime result persistence for diagnostics and UI status.
- Copyable diagnostics payload for blocked or failed real-world reports.
- Rescheduling after reboot, app update, manual time change, timezone change, schedule changes, selected-profile changes, and exact-alarm access changes.

Current limitations:

- Runtime behavior still needs repeated real-device validation across Android/OEM variants.
- Android 12+ exact-alarm special access can block scheduling until the user grants it.
- Exact alarms can still be affected by OEM background restrictions and platform behavior.
- Direct Boot support is not enabled; the app uses normal credential-protected app storage.
- Schedule UI is functional but not yet a polished setup-first flow.

Remaining work before this stage is complete:

- Manual smoke test on a real device with an actual work profile.
- Better setup/status guidance for permission missing, selected profile missing, credential required, Android request rejected, and exact-alarm access missing states.
- Decide whether an inexact fallback mode is useful for users who cannot or do not want to grant exact-alarm access.

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
- Run and document a real-device schedule smoke test.
- Improve schedule setup/status UX.
- Connect Compose Home to runtime state/actions after `MainActivity` responsibilities are reduced.
- Create stable README screenshots after screenshot architecture exists.
- Add stronger automated coverage for schedule editor validation.
- Evaluate whether an optional inexact fallback should exist for devices where exact-alarm access is unavailable.
