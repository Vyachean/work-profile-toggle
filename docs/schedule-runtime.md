# Work-profile schedule runtime design

## Status

Design draft. Do not implement schedule runtime before this document is reviewed and reflected in tests.

## Product intent

The runtime should provide a Digital Wellbeing-style schedule for an existing Android work profile on devices where the built-in Google/OEM work-profile schedule is missing or unavailable.

The feature is not a generic automation engine. It only decides whether the selected work profile should be active or paused for the current schedule boundary.

## Target behavior

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

Manual pause/resume remains available. The schedule runtime should reconcile state at the next schedule boundary and should clearly report when it could not apply the expected state.

## Runtime model

Use a state reconciliation model, not a fire-and-forget toggle model.

At each trigger:

1. Load the saved schedule.
2. Load the selected work profile.
3. Calculate the expected state for the current local time.
4. Read the actual work-profile state.
5. If actual state already matches expected state, record a no-op success.
6. If actual state differs, request the expected state.
7. Read or observe the resulting state when possible.
8. Persist the last schedule result.
9. Schedule the next boundary.

The runtime should not assume that a previous trigger succeeded.

## Boundary calculation

The schedule engine should calculate absolute next boundaries from the saved local schedule every time it runs.

Inputs:

- enabled flag;
- work days;
- start time;
- end time;
- current local date/time;
- device timezone.

Rules:

- If the schedule is disabled, no runtime boundary should be scheduled.
- If work days are empty, the schedule is incomplete and should not run.
- If start or end time is missing, the schedule is incomplete and should not run.
- If start time is before end time, the active window is same-day.
- If start time is after end time, the active window crosses midnight.
- If start time equals end time, treat the schedule as invalid unless a later product decision explicitly defines this as always-active or always-paused.
- Daylight saving and timezone changes must be handled by recalculating boundaries from local time, not by repeating a fixed duration.

## Trigger mechanism

The first implementation should prefer a simple Android alarm-based trigger with explicit rescheduling after every run.

Candidate approach:

- Use `AlarmManager` for the next boundary.
- Use a `BroadcastReceiver` as the alarm entry point.
- Perform only short work in the receiver.
- Delegate state changes through the existing action dispatcher/controller path.
- Reschedule the next boundary after every run.

Open decision:

- Start with inexact alarms if product tolerance allows delayed boundaries.
- Use exact alarms only if the product requires near-exact work start/end behavior and the required Android special access is acceptable.

Android documentation notes that inexact alarms respect battery-saving restrictions such as Doze, while exact alarms are intended for precise moments and may require the Android 12+ "Alarms & reminders" special access. If exact alarms are used, the app must check whether exact alarm access is granted and guide the user to settings when needed.

## Rescheduling events

The runtime should reschedule from persisted settings after:

- app update;
- device reboot;
- timezone change;
- time change;
- schedule settings change;
- selected work profile change;
- exact alarm access grant or revoke, if exact alarms are used.

The app should record a diagnostic result if rescheduling is blocked because setup is incomplete.

## Manual override behavior

Manual actions should remain allowed.

Baseline rule:

- Manual pause/resume changes the current state immediately.
- The next schedule boundary may override the manual state according to the configured schedule.
- The UI should communicate the next scheduled boundary so the behavior is predictable.

Future decision:

- Add an explicit "override until next boundary" or "disable schedule for today" feature only if users need it. Do not add it to the first runtime implementation.

## Failure handling

The runtime must persist structured results for the last schedule attempt.

Record at least:

- trigger time;
- expected state;
- selected profile status;
- requested action;
- action result;
- whether the final state was confirmed;
- next scheduled boundary;
- failure category when blocked.

Failure categories:

- schedule disabled;
- schedule incomplete;
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

Before enabling schedule runtime, the app should verify:

- a selected switchable work profile exists;
- quiet-mode state can be read;
- pause/resume requests are allowed by current setup;
- if exact alarms are selected, alarm access is available.

If setup is incomplete, schedule settings may remain saved but runtime should stay inactive and the UI should show what is missing.

## Testing requirements

Add tests before runtime implementation is considered complete:

- same-day active window calculation;
- overnight active window calculation;
- inactive days;
- start equals end invalid case;
- next boundary after start;
- next boundary after end;
- timezone change recalculation;
- disabled schedule;
- incomplete schedule;
- selected profile missing;
- action success;
- action failure;
- next boundary rescheduling after every run.

Use fake clocks and fake dispatchers for unit tests. Do not rely on real wall-clock time in tests.

## Initial implementation plan

1. Extract a pure schedule calculation component.
2. Add tests for expected state and next boundary calculation.
3. Add a persisted schedule runtime result model.
4. Add a scheduler abstraction around Android alarm APIs.
5. Add a receiver entry point that performs reconciliation.
6. Add setup checks and user-facing runtime status.
7. Add diagnostics for missed or blocked schedule changes.
8. Only then enable the schedule toggle as real runtime behavior.
