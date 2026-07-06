# Work-profile schedule runtime

## Status

Implemented baseline, still in real-device validation.

The app now has a schedule runtime that calculates the next schedule boundary, schedules an Android alarm, reconciles the selected work profile at the boundary, persists a structured runtime result, shows user-facing runtime status, and reschedules after key system events.

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
- inexact alarms for the first shipped runtime path;
- a `BroadcastReceiver` as the alarm entry point;
- `BroadcastReceiver.goAsync()` for bounded background execution;
- the shared work-profile action dispatcher/controller path for quiet-mode changes;
- explicit rescheduling after every handled boundary.

The receiver must keep orchestration short and always finish its pending result. If reconciliation grows beyond short bounded work, move execution to a more appropriate background mechanism instead of expanding receiver work.

## Alarm precision

The first runtime path uses inexact alarms.

Rationale:

- lower Android special-access burden;
- simpler setup UX;
- acceptable first implementation while reliability is being validated.

Known limitation:

- Android battery restrictions, Doze, and OEM background policies may delay inexact alarms.

Exact alarms are not currently enabled as a product path. If exact alarms are added later, the app must check Android 12+ exact-alarm access, guide the user to settings when missing, handle access loss, and update tests and documentation in the same PR.

## Rescheduling events

The runtime reschedules from persisted settings after:

- schedule settings change;
- selected work profile change;
- device reboot;
- app update;
- timezone change;
- manual time change.

Direct Boot support is not currently enabled. App schedule state is stored in normal app storage and is available after credential-protected storage is unlocked.

If exact alarms are introduced later, exact-alarm access changes must be handled by checking current access state when the app opens and when the runtime plans work. Do not rely only on a revoke broadcast.

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

User-facing diagnostics should be concise and actionable. Raw Android details belong in Advanced/Diagnostics.

## Permission and setup model

The runtime depends on the same work-profile control capability as manual actions.

Before schedule runtime can work reliably, the app needs:

- a selected switchable work profile;
- readable quiet-mode state;
- permission or platform capability to request pause/resume;
- Android acceptance of the requested state change.

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
- reschedule receiver action filtering.

Known automated coverage gaps:

- no end-to-end instrumentation test for `AlarmManager -> BroadcastReceiver -> UserManager.requestQuietModeEnabled`;
- no deterministic screenshot tests;
- no CI real-device or managed-device smoke test;
- no automated reboot/timezone/time-change end-to-end verification.

## Required real-device smoke test

Before treating schedule runtime as release-ready, validate on a real device with an actual work profile:

1. Install a current debug APK.
2. Grant `android.permission.MODIFY_QUIET_MODE` through ADB.
3. Select the work profile in the app.
4. Configure a near-future schedule boundary.
5. Confirm that the UI shows the next action.
6. Confirm that the selected profile pauses/resumes at the boundary.
7. Confirm that manual pause/resume is reconciled at the next boundary.
8. Confirm that reboot preserves and refreshes the next boundary.
9. Confirm that manual time and timezone changes refresh the next boundary.
10. Confirm that blocked states are visible in the schedule runtime status.

## Current implementation follow-ups

- Improve schedule setup/status UX.
- Add copyable diagnostics for blocked runtime results.
- Decide whether exact alarms are needed.
- Add deterministic screenshots after UI state extraction.
