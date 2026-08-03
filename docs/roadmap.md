# Roadmap

This document is the canonical current product direction and implementation status. Update it whenever scope, runtime behavior, validation, or release readiness changes.

## Product goal

Work Profile Toggle provides a Digital Wellbeing-style work-profile schedule on Android devices where Google or the OEM does not expose one.

The primary flow is:

1. detect and select an existing work profile;
2. complete the required ADB permission setup;
3. show whether the profile is active or paused;
4. allow manual Pause or Resume;
5. configure Resume time, Pause time, and Active days;
6. grant exact-alarm access when the configured schedule requires it;
7. enable and run a reliable schedule;
8. keep technical details in Diagnostics.

## Current baseline

The app supports:

- Android 9 / API 28 and newer, with validation priority on recent Android versions;
- setup-first work-profile status;
- persisted selected profile;
- manual Pause, Resume, and advanced Toggle actions;
- launcher shortcuts and the legacy shortcut picker;
- persisted last manual action result;
- saved Resume time, Pause time, and Active days;
- same-day and overnight work intervals;
- exact Android alarms for the next schedule boundary;
- exact-alarm access checks, explicit blocking, and recovery action;
- bounded asynchronous schedule reconciliation;
- persisted runtime result, next action, issue, and copyable diagnostics;
- rescheduling after app update, reboot, manual time change, timezone change, profile changes, schedule changes, and exact-alarm access changes;
- initial real-device validation of schedule-driven Pause and Resume in version 0.1.3;
- a Compose Material 3 runtime Home screen with dynamic light and dark color schemes;
- profile selection, ADB setup copying, exact-alarm recovery, schedule editing, and Diagnostics actions from the Compose Home screen;
- CI checks for lint, unit tests, debug APK assembly, release APK assembly, and debug certificate verification;
- signed release workflow infrastructure.

## Development rules

- Keep PRs focused on one clear product state.
- Require green CI for the exact head commit before merging.
- Resolve relevant review feedback before merging.
- Preserve schedule runtime behavior while changing UI architecture.
- Keep user-facing terminology aligned with `docs/product.md`.
- Keep schedule behavior aligned with `docs/schedule-runtime.md`.
- Treat release publication and real-device validation as separate states.
- Update documentation with behavior, setup, architecture, testing, and release changes.
- Treat profile and background behavior as device-dependent until verified on real hardware.

## Stage 1 — Product and documentation clarity

Goal: keep the repository understandable without chat history.

Status: **ongoing maintenance**.

Completed in the 0.1.5 documentation audit:

- aligned README, setup, product, platform, runtime, release, smoke-test, screenshot, roadmap, and agent documentation;
- documented Android 9 minimum support and recent-version validation priority;
- aligned terminology with the Compose UI;
- separated prepared, published, validated, and broadly validated release states;
- replaced stale View-era screenshot planning;
- established canonical documentation authority.

Remaining work:

- update validation status after each real-device test cycle;
- keep old issues from contradicting canonical documents;
- add approved README screenshots when deterministic baselines exist.

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
- primary work-profile state and Pause/Resume action;
- setup status, profile picker, and ADB setup copying;
- saved Resume time, Pause time, and Active days display;
- schedule status, next action, issue, editor actions, and exact-alarm recovery;
- Diagnostics access with raw profile and shortcut information;
- advanced per-profile Pause, Resume, and Toggle actions;
- dynamic color on Android 12+ and light/dark fallback themes;
- compact and state-specific previews;
- typed event-dispatch unit coverage.

Status: **implementation complete; real-device visual and behavioral validation required**.

Validation required before calling the UI broadly validated:

- setup missing and setup ready states;
- one and multiple work-profile selection;
- manual Pause and Resume;
- incomplete, disabled, enabled, and exact-alarm-blocked schedules;
- same-day and overnight schedule semantics;
- time and day pickers;
- Diagnostics and advanced profile actions;
- font scaling, dark theme, system insets, and compact screens.

## Stage 4 — Deterministic screenshots

Goal: generate stable documentation screenshots without requiring a real work profile in hosted CI.

Available prerequisites:

- deterministic `HomeUiState`;
- pure Compose Home rendering from state;
- state-specific and compact previews;
- no real profile requirement for fake-state rendering.

Status: **planned infrastructure**.

Next work:

- prove one Compose-compatible screenshot tool in the current Gradle and CI environment;
- fix dimensions, density, font scale, locale, time zone, and fallback theme;
- generate required light, dark, compact, and increased-font states;
- upload PNG and diff artifacts from CI;
- define baseline approval rules;
- commit approved screenshots under `docs/screenshots/` and link them from README.

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

- complete the expanded signed-release smoke test;
- broaden testing across Android and OEM variants;
- record behavior under battery restrictions and delayed process start;
- validate credential-required and Android-request-rejected recovery paths;
- decide whether an optional inexact fallback provides enough value without weakening expectations.

## Stage 6 — Release readiness

Goal: produce a version that is safe to install and straightforward to validate.

Current candidate: **0.1.5**.

Prepared:

- Compose runtime migration merged after green exact-head CI;
- no unresolved review threads in the implementation and release PRs;
- final migration diff reviewed for lost runtime behavior;
- documentation audited against current code and workflows;
- `versionName` increased to 0.1.5 and `versionCode` increased to 6;
- tag `v0.1.5` exists.

Still required before recording 0.1.5 as validated:

- confirm the signed GitHub Release APK asset was published from tag `v0.1.5`;
- install or update from 0.1.4;
- confirm selected profile and schedule persistence;
- validate the Compose Home screen in light, dark, compact, and increased-font configurations;
- run manual Pause, Resume, and advanced Toggle;
- validate same-day and overnight schedule behavior;
- confirm the next scheduled Pause and Resume;
- verify exact-alarm recovery, time/timezone rescheduling, reboot recovery, shortcuts, and Diagnostics privacy;
- record the device, Android/OEM version, install path, and result.

## Known follow-ups

- Issue #25 is the original roadmap and should be marked superseded by this document rather than maintained as a second live roadmap.
- Add deterministic screenshot tests and README screenshots.
- Expand real-device schedule validation.
- Review accessibility and large-font behavior after runtime UI validation.
- Reassess an inexact scheduling fallback only with a clearly documented reliability contract.
