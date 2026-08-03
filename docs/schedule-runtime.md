# Work-profile schedule runtime

## Status

Implemented baseline, still in real-device validation.

The app calculates the next schedule boundary, schedules an Android exact alarm, reconciles the selected work profile at the boundary, persists a structured runtime result, shows user-facing runtime status, provides copyable schedule diagnostics, and reschedules after key app and system events.

This document is the runtime contract for future changes. Keep it updated in the same PR as behavior changes.

## Product intent

The runtime provides a Digital Wellbeing-style schedule for an existing Android work profile on devices where the built-in Google or OEM schedule is missing or unavailable.

The feature is not a generic automation engine. It decides whether the selected work profile should be active or paused for the current schedule interval.

## User model and terminology

The user configures:

- **Resume time** — when the profile should become active;
- **Pause time** — when the profile should become paused;
- **Active days** — days on which the active interval begins;
- schedule enabled or disabled.

The intended state is:

```text
inside configured work hours  -> work profile active
outside configured work hours -> work profile paused
```

Manual Pause and Resume remain available. The schedule runtime reconciles state at the next boundary and reports when it could not apply the expected state.

## Implemented runtime model

The runtime uses state reconciliation, not fire-and-forget toggles.

At each schedule boundary:

1. Load the saved schedule.
2. Calculate the expected state for the current local time.
3. Discover available work profiles.
4. Resolve the selected work profile.
5. Read the actual selected-profile quiet-mode state.
6. If the actual state already matches the expected state, record a no-op success.
7. If the actual state differs, request the expected state through the shared work-profile action path.
8. Read the final state when possible.
9. Persist the last schedule result.
10. Refresh the next boundary plan.

The runtime does not assume that a previous trigger succeeded.

## Boundary calculation

The schedule engine calculates absolute next boundaries from the saved local schedule whenever planning or runtime reconciliation runs.

Inputs:

- enabled flag;
- active days;
- resume time;
- pause time;
- current local date and time;
- device timezone.

Rules:

- If the schedule is disabled, no runtime boundary is scheduled.
- If active days are empty, the schedule is incomplete and does not run.
- If resume or pause time is missing, the schedule is incomplete and does not run.
- If resume time is before pause time, the active window is same-day.
- If resume time is after pause time, the active window crosses midnight.
- For an overnight active window, an active day is the day when the interval starts.
  - Example: `MONDAY`, resume `22:00`, pause `06:00` means active from Monday 22:00 until Tuesday 06:00.
  - Sunday 22:00 until Monday 06:00 is controlled by `SUNDAY`, not `MONDAY`.
- If resume time equals pause time, the schedule is invalid.
- An invalid schedule must not produce a next boundary or schedule an alarm. This prevents immediate rescheduling loops when a boundary would otherwise be calculated as `now`.
- Daylight-saving and timezone changes are handled by recalculating boundaries from local time, not by repeating a fixed duration.

## Trigger mechanism

The current runtime uses:

- `AlarmManager` for the next boundary;
- `setExactAndAllowWhileIdle(...)` for scheduled boundaries;
- an exact-alarm access check before planning exact alarms on Android versions that require it;
- a `BroadcastReceiver` as the alarm entry point;
- `BroadcastReceiver.goAsync()` for bounded background execution;
- the shared work-profile action dispatcher/controller path for state changes;
- explicit rescheduling after every handled boundary.

The receiver must keep orchestration short and always finish its pending result. If reconciliation grows beyond short bounded work, move execution to a more appropriate background mechanism instead of expanding receiver work.

## Exact-alarm behavior

The runtime currently uses exact alarms because delayed boundaries can leave the work profile active outside work hours or paused inside work hours.

Setup and blocking behavior:

- On Android versions where exact-alarm access is not required, planning can proceed without extra user action.
- On Android versions where access is required, the app checks `canScheduleExactAlarms()` before scheduling.
- If access is missing, the app does not schedule the boundary and records `EXACT_ALARM_ACCESS_MISSING`.
- The Compose Schedule card shows the access state and a settings action for configured schedules.
- An enabled configured schedule is shown as blocked while access is missing.
- The app refreshes planning after returning from settings and after exact-alarm access change broadcasts.

Known limitation:

- Exact alarms reduce timing drift compared with inexact alarms, but Android and OEM power management can still affect delivery and the subsequent work-profile API call.

Future decision:

- Add an optional inexact fallback only if there is a clear product reason. A fallback must be explicit in UI and diagnostics because delayed boundaries are not equivalent to exact schedule behavior.

## Rescheduling events

The runtime reschedules from persisted settings after:

- schedule settings change;
- selected work profile change;
- a handled schedule boundary;
- device reboot;
- app update;
- timezone change;
- manual time change;
- exact-alarm access change;
- returning to the app after exact-alarm settings.

Direct Boot support is not enabled. Schedule state is stored in credential-protected app storage and becomes available after the device has been unlocked following a reboot.

## Manual override behavior

Manual actions remain allowed.

Baseline rule:

- Manual Pause or Resume changes the current state immediately.
- The next schedule boundary may override that manual state according to the configured schedule.
- The Home screen communicates the next scheduled boundary so the behavior is predictable.

Future decision:

- Add an explicit override-until-next-boundary or disable-for-today feature only after a separate product decision.

## Runtime result model

The runtime persists structured results for the last schedule attempt.

The result records:

- trigger time;
- expected state;
- selected profile status;
- requested action;
- action result;
- whether the final state was confirmed;
- next scheduled boundary;
- failure category when blocked or failed.

Failure categories:

- schedule disabled;
- schedule incomplete;
- schedule invalid;
- selected profile missing;
- work profile unavailable;
- permission missing;
- credential required;
- Android request rejected;
- exact alarm access missing;
- runtime exception.

User-facing status should be concise and actionable. Raw Android details belong in Advanced/Diagnostics or explicitly copied diagnostics.

## Copyable diagnostics

The Schedule card provides a copyable diagnostics payload for configured schedules.

The payload is stable plain text intended for debugging real-world reports. It includes:

- app version name;
- current time and timezone;
- saved enabled flag;
- saved active days;
- saved resume time;
- saved pause time;
- exact-alarm access state;
- whether a runtime result is present;
- runtime trigger time;
- expected state;
- selected profile status;
- requested action;
- action result;
- final-state confirmation flag;
- next boundary timestamp and expected state;
- failure category.

The payload must not include profile names, serial numbers, or raw user/profile handles.

## Permission and setup model

Reliable schedule execution requires:

- a selected switchable work profile;
- readable quiet-mode state;
- `MODIFY_QUIET_MODE` or an equivalent platform capability;
- Android acceptance of the requested state change;
- exact-alarm access when Android requires it.

Schedule settings may remain saved when setup is incomplete. Runtime execution must report the missing requirement instead of pretending that scheduling is working.

Exact-alarm access is schedule-specific. It does not block profile selection or manual Pause/Resume.

## Current automated coverage

Existing unit tests cover:

- same-day and overnight active windows;
- overnight active-day semantics;
- inactive days;
- equal resume/pause time invalidation;
- incomplete and disabled schedules;
- timezone-aware boundary calculation;
- alarm scheduling, cancellation, rejected alarms, past/now boundaries, and exact-alarm checks;
- boundary planner result persistence;
- runtime reconciliation orchestration;
- next-boundary refresh after handling;
- runtime failure precedence and exception persistence;
- runtime status summary mapping;
- selected-profile availability, no-op success, Pause/Resume dispatch, read failures, request failures, and unconfirmed final state;
- reschedule receiver action filtering;
- diagnostics formatting for complete and missing runtime results;
- Home state mapping and the typed Home event-to-action contract.

Known automated coverage gaps:

- no end-to-end instrumentation test for `AlarmManager -> BroadcastReceiver -> UserManager.requestQuietModeEnabled`;
- no deterministic screenshot tests;
- no CI real-device or managed-device smoke test;
- no automated reboot, time, timezone, or exact-alarm-settings end-to-end verification.

## Required real-device validation

Use [Release smoke test](smoke-test.md) as the canonical release checklist.

Runtime-specific acceptance must include:

1. Install or update the signed release candidate while preserving app data when testing an update.
2. Confirm the selected profile and saved schedule persist.
3. Confirm the Home screen shows the correct next action.
4. Confirm near-future Pause and Resume boundaries apply the expected state.
5. Confirm a manual action is reconciled at the next boundary.
6. Confirm reboot restores planning after unlock.
7. Confirm manual time and timezone changes refresh the next boundary.
8. Confirm exact-alarm access loss produces a blocked state and recovery action.
9. Copy schedule diagnostics and confirm the payload is readable and contains no profile names, serials, or handles.

## Current follow-ups

- Complete real-device validation of the Compose schedule UI and runtime paths.
- Add deterministic Compose screenshot tests from the existing `HomeUiState` and previews.
- Broaden validation across Android versions and OEM power-management behavior.
- Decide whether an explicit inexact fallback provides enough value without weakening reliability expectations.
