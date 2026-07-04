# Product model

## Product goal

Work Profile Toggle is an Android app for controlling an existing Android work profile.

The app should provide a clear user-facing flow for pausing and resuming a work profile, while keeping advanced Android and automation details available but secondary.

## Primary users

- Users who already have an Android work profile on their device.
- Users who want a small dedicated app for pausing or resuming that work profile.
- Advanced users who can grant the required Android permission with ADB.
- Automation users who need launcher shortcuts or legacy Android shortcuts for tools such as MacroDroid.

## Core concepts

Use these user-facing terms in primary UI and documentation:

- Work profile
- Active
- Paused
- Pause work profile
- Resume work profile
- Schedule
- Setup
- Advanced
- Diagnostics

Avoid exposing these terms in the main user flow unless the user is in an advanced or diagnostic context:

- Quiet mode
- UserHandle
- Serial number
- Raw profile identifiers
- Dynamic shortcut internals
- Raw exceptions

## Current scope

The current app can:

- discover switchable work profiles associated with the current user;
- persist the selected work profile;
- pause, resume, or toggle the selected work profile after setup;
- expose dynamic launcher shortcuts;
- expose a legacy Android shortcut picker for automation apps;
- store schedule settings for future automatic pause/resume support;
- persist the last action result;
- build stable CI debug APK artifacts;
- publish signed release APKs after release secrets are configured.

## Non-goals

The app should not become a full Android enterprise/work-profile manager.

It must not:

- create, provision, or delete profiles;
- install, clone, freeze, or manage apps inside a profile;
- replace Shelter, Island, or enterprise provisioning tools;
- duplicate Tasker, MacroDroid, or launcher functionality;
- execute background schedule changes before the scheduling runtime is explicitly implemented and tested;
- add broad device-management policies unrelated to work-profile pause/resume.

## Main flow

```text
Setup required
  -> grant required permission with ADB
  -> select or auto-detect work profile
  -> show current state
  -> pause or resume work profile
  -> show persisted last action result
```

## Schedule flow

Current state:

```text
Schedule settings can be saved.
Automatic schedule execution is not active yet.
```

Target state:

```text
Configure pause time
  -> configure resume time
  -> choose active days
  -> enable schedule
  -> app executes schedule reliably through a tested Android background mechanism
```

## Advanced flow

Advanced or diagnostic surfaces may expose:

- ADB setup details;
- raw profile data;
- shortcut compatibility details;
- diagnostic errors;
- last action result details.

These details should not dominate the Home screen.
