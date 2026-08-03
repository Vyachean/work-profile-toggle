# Product model

## Product goal

Work Profile Toggle is an Android app for controlling an existing Android work profile.

The main product goal is to provide a Digital Wellbeing-style work-profile schedule for devices where the built-in Google or OEM schedule feature is missing or unavailable.

The app should provide a clear setup flow for selecting and controlling one work profile manually, then keep that selected profile aligned with the configured schedule.

Advanced Android and automation details should remain available but secondary.

## Primary users

- Users who already have an Android work profile on their device.
- Users whose device does not provide a built-in work-profile schedule feature.
- Users who want their work profile to follow chosen active days and work hours.
- Users who want a small dedicated app for manually pausing or resuming that work profile.
- Advanced users who can grant the required Android permission with ADB.
- Automation users who need launcher shortcuts or legacy Android shortcuts for tools such as MacroDroid.

## User-facing terminology

Use these terms in the primary UI and user-facing documentation:

- Work profile
- Active
- Paused
- Pause
- Resume
- Schedule
- Pause time
- Resume time
- Active days
- Setup
- Advanced
- Diagnostics

Avoid exposing these terms in the main user flow unless the user opens an advanced or diagnostic context:

- Quiet mode
- UserHandle
- Serial number
- Raw profile identifiers
- Dynamic shortcut internals
- Raw exceptions

`Resume time` is the start of the active work interval. `Pause time` is the end of the active work interval.

## Product behavior target

The schedule feature models work-profile availability:

- the user chooses the active days;
- the user chooses when the work profile should resume;
- the user chooses when the work profile should pause;
- during the resulting active interval, the work profile should be active;
- outside that interval, the work profile should be paused;
- the user can still pause or resume manually;
- the next schedule boundary reconciles the selected profile back to the state expected by the saved schedule;
- the app clearly communicates when the schedule is disabled, incomplete, invalid, blocked, or missing required setup.

The schedule is not a generic automation engine. It is specifically for work-profile pause/resume.

## Current scope

The current app can:

- discover switchable work profiles associated with the current user;
- persist one selected work profile;
- pause, resume, or toggle a profile after setup;
- expose dynamic launcher shortcuts;
- expose a legacy Android shortcut picker for automation apps;
- save and run schedule settings for the selected work profile;
- calculate same-day and overnight schedule boundaries;
- schedule the next boundary through Android exact alarms;
- block enabled scheduling and show guidance when exact-alarm access is missing;
- reconcile the selected work profile at a schedule boundary;
- reschedule after handled boundaries, schedule changes, selected-profile changes, reboot, app update, manual time changes, timezone changes, and exact-alarm access changes;
- persist the last manual action result;
- persist the last schedule runtime result;
- show saved schedule values, the next scheduled action, and current blocking issues;
- expose technical details and advanced profile actions through Diagnostics;
- build stable CI debug APK artifacts;
- publish signed release APKs through the configured GitHub Actions release workflow.

Release publication and real-device validation are separate states. A published APK is not considered validated until the release smoke test is completed.

## Non-goals

The app should not become a full Android enterprise/work-profile manager.

It must not:

- create, provision, or delete profiles;
- install, clone, freeze, or manage apps inside a profile;
- replace Shelter, Island, or enterprise provisioning tools;
- duplicate Tasker, MacroDroid, or launcher functionality;
- become a generic task scheduler or automation engine;
- add broad device-management policies unrelated to work-profile pause/resume.

## Main flow

```text
Detect switchable work profiles
  -> select or auto-select one work profile
  -> show missing work-profile control permission when required
  -> copy and run the ADB permission command
  -> show current active or paused state
  -> allow manual Pause or Resume
  -> configure Resume time, Pause time, and Active days
  -> grant exact-alarm access when the configured schedule requires it
  -> enable the schedule
  -> show saved values, next action, and current issue
```

Exact-alarm access is schedule-specific. It should not block profile selection or manual Pause/Resume.

## Schedule flow

```text
Configure Resume time
  -> configure Pause time
  -> choose Active days
  -> app shows exact-alarm access state
  -> grant access when Android requires it
  -> enable schedule
  -> app schedules the next boundary when setup is complete
  -> work profile is active inside configured work hours
  -> work profile is paused outside configured work hours
  -> app reports the next scheduled action or current blocked state
```

For an overnight interval, the selected active day is the day on which the active interval begins. Monday resume at 22:00 and pause at 06:00 means active from Monday 22:00 until Tuesday 06:00.

The runtime is implemented as a baseline and remains in real-device validation. It should not be described as broadly validated until the smoke-test checklist is completed on real devices.

## Advanced flow

The Home screen keeps an Advanced card as a secondary entry point.

Diagnostics may expose:

- raw profile data;
- shortcut status and compatibility details;
- diagnostic errors;
- last manual action result;
- advanced per-profile Pause, Resume, and Toggle actions.

Configured schedules provide a separate copyable schedule diagnostics payload. ADB setup guidance remains in the Setup card only while the permission is missing.
