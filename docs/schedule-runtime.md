# Work-profile schedule runtime

## Status

Implemented baseline, still in real-device validation.

The app now has a schedule runtime that calculates the next schedule boundary, schedules an Android exact alarm, reconciles the selected work profile at the boundary, persists a structured runtime result, shows user-facing runtime status, provides copyable schedule diagnostics, and reschedules after key app and system events.

This document is the runtime contract for future changes. Keep it updated in the same PR as behavior changes.

## Product intent

The runtime provides a Digital Wellbeing-style schedule for an existing Android work profile on devices where the built-in Google/OEM work-profile schedule is missing or unavailable.

The feature is not a generic automation engine. It only decides whether the selected work profile should be active or paused for the current schedule boundary.

## User model

The user configures:

- work days;
- work start time;
- work end time;
- schedule enabled or disabled.

The intended state is:

```text
inside configured work hours  -> work profile active
outside configured work hours -> work profile paused
```

Manual pause/resume remains available. The schedule runtime reconciles state at the next schedule boundary and reports when it could not apply the expected state.

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
- work days;
- start time;
- end time;
- current local date/time;
- device timezone.

Rules:

- If the schedule is disabled, no runtime boundary is scheduled.
- If work days are empty, the schedule is incomplete and does not run.
- If start or end time is missing, the schedule is incomplete and does not run.
- If start time is before end time, the active window is same-day.
- If start time is after end time, the active window crosses midnight.
- For an overnight active window, a selected work day is the day when the active window starts.
  - Example: `MONDAY`, `22:00` -> `06:00` means active from Monday 22:00 until Tuesday 06:00.
  - Sunday 22:00 until Monday 06:00 is controlled by `SUNDAY`, not `MONDAY`.
- If start time equals end time, the schedule is invalid.
- An invalid schedule must not produce a next boundary and must not schedule an alarm. This prevents immediate rescheduling loops when the next boundary would otherwise be calculated as `now`.
- Daylight saving and timezone changes are handled by recalculating boundaries from local time, not by repeating a fixed duration.

## Trigger mechanism

The current runtime uses:

- `AlarmManager` for the next boundary;
- exact alarms through `setExactAndAllowWhileIdle(...)` for scheduled boundaries;
- an exact-alarm access check before planning exact alarms on Android versions that require it;
- a `BroadcastReceiver` as the alarm entry point;
- `BroadcastReceiver.goAsync()` for bounded background execution;
- the shared work-profile action dispatcher/controller path for quiet-mode changes;
- explicit rescheduling after every handled boundary.

The receiver must keep orchestration short and always finish its pending result. If reconciliation grows beyond short bounded work, move execution to a more appropriate background mechanism instead of expanding receiver work.

## Alarm precision

The runtime currently uses exact alarms for schedule boundaries.

Rationale:

- work-profile boundaries are user-visible schedule events;
- delayed boundaries can leave the work profile active outside work hours or paused inside work hours;
- exact alarms make real-device validation clearer because expected boundary timing is explicit.

Setup and blocking behavior:

- On Android versions where exact-alarm access is not required, schedule planning can proceed without extra user action.
- On Android versions where exact-alarm access is required, the app checks `canScheduleExactAlarms()` before scheduling.
- If exact-alarm access is missing, the app does not schedule the boundary and records a blocked runtime status with `exact alarm access missing`.
- The UI should show the missing access state and guide the user to the relevant Android settings screen.
- The app refreshes planning after returning from settings and after exact-alarm access change broadcasts.

Known limitation:

- Exact alarms reduce timing drift compared with inexact alarms, but Android/OEM power management can still affect background execution and work-profile APIs. Real-device validation remains required.

Future decision:

- Add an optional inexact fallback only if there is a clear product reason for users who cannot or do not want to grant exact-alarm access. A fallback must be explicit in UI/status because delayed boundaries are not equivalent to the exact schedule behavior.

## Rescheduling events

The runtime reschedules from persisted settings after:

- schedule settings change;
- selected work profile change;
- device reboot;
- app update;
- timezone change;
- manual time change;
- exact-alarm access change;
- returning to the app after exact-alarm settings.

Direct Boot support is not currently enabled. App schedule state is stored in normal app storage and is available after credential-protected storage is unlocked.

## Manual override behavior

Manual actions remain allowed.

Baseline rule:

- Manual pause/resume changes the current state immediately.
- The next schedule boundary may override the manual state according to the configured schedule.
- The UI communicates the next scheduled boundary so the behavior is predictable.

Future decision:

- Add an explicit "override until next boundary" or "disable schedule for today" feature only if users need it. Do not add it without a separate product decision.

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

User-facing diagnostics should be concise and actionable. Raw Android details belong in Advanced/Diagnostics or in explicitly copied diagnostics payloads.

## Copyable diagnostics

The Schedule section provides a copyable diagnostics payload for configured schedules.

The copied payload is stable, plain text, and intended for debugging real-world reports. It includes:

- app version name;
- current time and timezone;
- saved schedule enabled flag;
- saved work days;
- saved resume/start time;
- saved pause/end time;
- exact-alarm access state;
- whether a runtime result is present;
- runtime trigger time;
- runtime expected state;
- selected profile status;
- requested action;
- action result;
- final-state confirmation flag;
- next boundary timestamp and expected state;
- failure category.

The payload must not include profile names or raw user/profile identifiers. If future diagnostics need more context, prefer stable enums and non-personal state fields over names, serials, or handles.

## Permission and setup model

The runtime depends on the same work-profile control capability as manual actions.

Before schedule runtime can work reliably, the app needs:

- a selected switchable work profile;
- readable quiet-mode state;
- permission or platform capability to request pause/resume;
- Android acceptance of the requested state change;
- exact-alarm access when Android requires it.

Schedule settings may remain saved when setup is incomplete, but runtime execution should report the missing requirement instead of silently pretending that scheduling is working.

## Current automated coverage

Existing unit tests cover:

- same-day active window calculation;
- overnight active window calculation;
- overnight active-day semantics, where the selected day is the window start day;
- inactive days;
- start equals end invalid case;
- incomplete schedules;
- disabled schedules;
- current timezone boundary calculation;
- alarm scheduling, cancellation, rejected alarms, past/now boundaries, and exact-alarm access checks inside the alarm abstraction;
- boundary planner result persistence;
- runtime handler reconciliation orchestration;
- next-boundary refresh after runtime handling;
- runtime failure precedence;
- runtime exception persistence;
- runtime status summary mapping for next action and issue states;
- work-profile reconciliation for selected profile availability, no-op success, pause/resume dispatch, read failures, request failures, missing profile, and unconfirmed final state;
- reschedule receiver action filtering;
- schedule diagnostics payload formatting for complete and missing runtime results.

Known automated coverage gaps:

- no end-to-end instrumentation test for `AlarmManager -> BroadcastReceiver -> UserManager.requestQuietModeEnabled`;
- no deterministic screenshot tests;
- no CI real-device or managed-device smoke test;
- no automated reboot/timezone/time-change/exact-alarm-settings end-to-end verification.

## Required real-device smoke test

Before treating schedule runtime as release-ready, validate on a real device with an actual work profile:

1. Install a current debug APK.
2. Grant `android.permission.MODIFY_QUIET_MODE` through ADB.
3. Grant exact-alarm access when Android requires it.
4. Select the work profile in the app.
5. Configure a near-future schedule boundary.
6. Confirm that the UI shows the next action.
7. Confirm that the selected profile pauses/resumes at the boundary.
8. Confirm that manual pause/resume is reconciled at the next boundary.
9. Confirm that reboot preserves and refreshes the next boundary.
10. Confirm that manual time and timezone changes refresh the next boundary.
11. Confirm that exact-alarm access loss is shown as a blocked schedule state.
12. Confirm that blocked states are visible in the schedule runtime status.
13. Copy schedule diagnostics and confirm that the payload is readable and does not include profile names, serials, or handles.

## Current implementation follow-ups

- Improve schedule setup/status UX.
- Decide whether an optional inexact fallback mode is useful.
- Add deterministic screenshots after UI state extraction.
