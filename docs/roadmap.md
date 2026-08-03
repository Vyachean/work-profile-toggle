# Roadmap

This document records the current product direction and implementation status. Update it whenever scope, runtime behavior, or release readiness changes.

## Product goal

Work Profile Toggle provides a Digital Wellbeing-style work-profile schedule on Android devices where Google or the OEM does not expose one.

The primary flow is:

1. detect and select an existing work profile;
2. complete the required permission setup;
3. show whether the profile is active or paused;
4. allow manual pause or resume;
5. configure and run a reliable schedule;
6. keep technical details in Diagnostics.

## Current baseline

The app supports:

- setup-first work-profile status;
- persisted selected profile;
- manual pause, resume, and toggle actions;
- launcher shortcuts and the legacy shortcut picker;
- persisted last manual action result;
- saved schedule settings;
- same-day and overnight work windows;
- exact Android alarms for the next schedule boundary;
- exact-alarm access checks and recovery action;
- bounded asynchronous schedule reconciliation;
- persisted runtime result, next action, issue, and copyable diagnostics;
- rescheduling after app update, reboot, manual time change, timezone change, profile changes, schedule changes, and exact-alarm access changes;
- initial real-device validation of schedule-driven pause and resume in version 0.1.3;
- a Compose Material 3 runtime Home screen with dynamic light and dark color schemes;
- profile selection, ADB setup copying, exact-alarm recovery, schedule editing, and Diagnostics actions from the Compose Home screen;
- CI checks for lint, unit tests, debug APK assembly, release APK assembly, and debug certificate verification;
- signed release workflow infrastructure.

## Development rules

- Keep PRs focused on one clear product state.
- Require green CI for the exact head commit before merging.
- Resolve relevant review feedback before merging.
- Preserve schedule runtime behavior while changing UI architecture.
- Keep user-facing terminology simple and move Android implementation details to Diagnostics.
- Update documentation with behavior, setup, architecture, and release changes.
- Treat profile and background behavior as device-dependent until verified on real hardware.

## Stage 1 — Product and documentation clarity

Goal: keep the repository understandable without chat history.

Status: **ongoing maintenance**.

Remaining work:

- keep README concise and user-oriented;
- keep setup, platform limitations, schedule runtime, release, and testing details under `docs/`;
- update validation status after each real-device test cycle.

## Stage 2 — UI architecture cleanup

Goal: keep UI state deterministic and separate from Android platform actions.

Implemented:

- `HomeUiState` and schedule editor state factories;
- structured runtime status and issue models;
- resource-backed Compose screen text;
- typed Home events and actions;
- Compose runtime hosting from `MainActivity`;
- Android dialogs, clipboard, profile operations, exact-alarm settings, and scheduling actions kept at the Activity boundary.

Status: **complete for the current release scope**.

Possible later cleanup:

- move platform dialog construction into focused helpers if `MainActivity` grows again;
- introduce a lifecycle-aware state holder only when asynchronous state sources require it.

## Stage 3 — Compose Material 3 Home

Goal: replace the utility-style View UI with a clear modern runtime interface.

Implemented:

- Material 3 app bar and structured cards;
- primary work-profile state and pause/resume action;
- setup status, profile picker, and ADB setup copying;
- saved pause time, resume time, and active-day display;
- schedule status, next action, issue, editor actions, and exact-alarm recovery;
- Diagnostics access with raw profile and shortcut information;
- advanced per-profile pause, resume, and toggle actions;
- dynamic color on Android 12+ and light/dark fallback themes;
- compact and state-specific previews;
- typed event-dispatch unit coverage.

Status: **implementation complete; real-device visual and behavioral validation required**.

Validation required before calling the UI broadly validated:

- setup missing and setup ready states;
- one and multiple work-profile selection;
- manual pause and resume;
- incomplete, disabled, enabled, and exact-alarm-blocked schedules;
- time and day pickers;
- Diagnostics and advanced profile actions;
- font scaling, dark theme, system insets, and compact screens.

## Stage 4 — Deterministic screenshots

Goal: generate stable documentation screenshots without requiring a real work profile in hosted CI.

Status: **planned**.

Planned work:

- add deterministic fake Home states;
- add Roborazzi/Robolectric or an equivalent screenshot test tool;
- upload PNG artifacts from CI;
- review and commit stable screenshots under `docs/screenshots/`;
- link approved screenshots from README.

## Stage 5 — Schedule runtime hardening

Goal: make schedule-driven work-profile state reliable across supported Android devices.

Implemented baseline:

- state reconciliation rather than blind toggles;
- exact next-boundary alarms;
- next-boundary rescheduling after every handled boundary;
- reboot, update, time, timezone, profile, schedule, and permission rescheduling;
- persisted diagnostic results;
- initial real-device validation.

Status: **in progress**.

Remaining work:

- broaden smoke testing across Android and OEM variants;
- record behavior under battery restrictions and delayed process start;
- validate credential-required and Android-request-rejected recovery paths;
- decide whether an optional inexact fallback provides enough value without weakening expectations.

## Stage 6 — Release readiness

Goal: produce a version that is safe to install and straightforward to validate.

Release candidate: **0.1.5**.

Prepared:

- Compose runtime migration merged after green exact-head CI;
- no unresolved review threads;
- final migration diff reviewed for lost runtime behavior;
- README and release smoke-test instructions updated;
- versionName increased to 0.1.5 and versionCode increased to 6.

Required after the signed release APK is produced:

- install or update from 0.1.4;
- confirm selected profile and schedule persistence;
- validate the Compose Home screen in light, dark, compact, and increased-font configurations;
- run manual pause/resume;
- confirm the next scheduled pause and resume;
- verify exact-alarm recovery and Diagnostics;
- record the result in project documentation.

## Known follow-ups

- Keep issue #25 aligned with this roadmap.
- Add deterministic screenshot tests and README screenshots.
- Expand real-device schedule validation.
- Review accessibility and large-font behavior after runtime UI validation.
- Reassess an inexact scheduling fallback only with a clearly documented reliability contract.
