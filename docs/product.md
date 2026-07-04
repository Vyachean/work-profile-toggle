# Product model

## Product goal

Work Profile Toggle is an Android app for controlling an existing Android work profile.

The main product goal is to provide a Digital Wellbeing-style work-profile schedule for devices where the built-in Google/OEM schedule feature is missing or unavailable.

The app should provide a clear user-facing flow for pausing and resuming a work profile manually first, then add a reliable schedule experience when the scheduling runtime is implemented.

Advanced Android and automation details should remain available but secondary.

## Primary users

- Users who already have an Android work profile on their device.
- Users whose device does not provide a built-in work-profile schedule feature.
- Users who want their work profile to follow chosen work days and work hours.
- Users who want a small dedicated app for manually pausing or resuming that work profile.
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
- Work days
- Start time
- End time
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

## Product behavior target

The schedule feature should model work-profile availability:

- the user chooses the days when the schedule applies;
- the user chooses when work hours start;
- the user chooses when work hours end;
- during work hours, the work profile should be active;
- outside work hours, the work profile should be paused;
- the user can still pause or resume manually;
- the app should clearly communicate when schedule support is not active, blocked, unreliable, or missing required setup.

The schedule is not meant to be a generic automation engine. It is specifically for work-profile pause/resume.

## Current scope

The current app can:

- discover switchable work profiles associated with the current user;
- persist the selected work profile;
- pause, resume, or toggle the selected work profile after setup;
- expose dynamic launcher shortcuts;
- expose a legacy Android shortcut picker for automation apps;
- store schedule settings for future work-profile schedule support;
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
- become a generic task scheduler or automation engine;
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
Schedule runtime is not active yet.
```

Target state:

```text
Configure work days
  -> configure work start time
  -> configure work end time
  -> enable schedule
  -> work profile is active during configured work hours
  -> work profile is paused outside configured work hours
  -> app reports blocked or missed schedule changes clearly
```

## Advanced flow

Advanced or diagnostic surfaces may expose:

- ADB setup details;
- raw profile data;
- shortcut compatibility details;
- diagnostic errors;
- last action result details.

These details should not dominate the Home screen.
