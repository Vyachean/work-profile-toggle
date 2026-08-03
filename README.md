# Work Profile Toggle

Android app for pausing, resuming, and scheduling an existing Android work profile.

The main goal is to provide a Digital Wellbeing-style work-profile schedule for devices where the built-in Google or OEM schedule feature is missing or unavailable.

## Status

Work Profile Toggle is an early MVP under real-device validation.

Current baseline:

- a Compose Material 3 Home screen shows setup, work-profile state, schedule status, next action, and recovery guidance;
- manual pause and resume work after the required ADB-granted permission is available;
- schedule settings are persisted and displayed in the Home screen;
- the app plans and runs work-profile schedule boundaries with Android exact alarms;
- the schedule-driven pause and resume path was initially validated on a real device in version 0.1.3;
- the new Compose runtime UI introduced for version 0.1.5 still requires release smoke validation;
- device and OEM behavior needs broader validation.

## Who it is for

This app is for users who:

- already have an Android work profile;
- want that work profile to be active during chosen work hours and paused outside them;
- do not have a suitable built-in work-profile schedule feature on their device;
- can grant the required protected Android permission with ADB.

## What it does

- Detects switchable Android work profiles and lets the user select one.
- Pauses or resumes the selected work profile from the Home screen.
- Stores and runs work-hours schedule settings.
- Shows the next scheduled action and relevant runtime issues.
- Opens exact-alarm settings when reliable scheduling is blocked.
- Keeps raw profile information, toggle actions, shortcut status, and runtime details under Diagnostics.
- Provides launcher shortcuts and legacy Android shortcuts for automation apps such as MacroDroid.
- Reschedules the next work-profile boundary after reboot, app update, manual time change, timezone change, profile changes, schedule changes, and exact-alarm access changes.

## What it does not do

Work Profile Toggle is not a Shelter, Island, or Android enterprise management replacement.

It does not:

- create, provision, or delete work profiles;
- install, clone, freeze, or manage apps inside a profile;
- replace Android enterprise/work-profile provisioning tools;
- duplicate Tasker, MacroDroid, or launcher functionality;
- act as a generic task scheduler or automation engine.

## Screenshots

Stable README screenshots should be committed under `docs/screenshots/` and linked from this section.

Automatic screenshot generation is planned as deterministic screenshot tests. See [Screenshots plan](docs/screenshots.md).

## Install and setup

Package name:

```text
io.github.vyachean.workprofiletoggle
```

Development APKs are available as GitHub Actions artifacts from successful `main` runs. Signed release APKs are published through GitHub Releases.

Basic setup:

1. Install the APK.
2. Open the app and select the work profile to control.
3. Copy and run the ADB permission command shown by the app.
4. Grant exact alarm access when the Schedule card reports that it is missing.
5. Configure pause time, resume time, and active days, then enable the schedule.

Full instructions: [Setup guide](docs/setup.md).

## Documentation

- [Setup guide](docs/setup.md)
- [Product model](docs/product.md)
- [Platform notes](docs/platform.md)
- [Roadmap](docs/roadmap.md)
- [Screenshots plan](docs/screenshots.md)
- [Release process](docs/release.md)
- [Release smoke test](docs/smoke-test.md)
- [Documentation maintenance](docs/maintenance.md)
