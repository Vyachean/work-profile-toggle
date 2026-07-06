# Work Profile Toggle

Android app for pausing, resuming, and scheduling an existing Android work profile.

The main goal is to provide a Digital Wellbeing-style work-profile schedule for devices where the built-in Google or OEM schedule feature is missing or unavailable.

## Status

Work Profile Toggle is an early MVP under real-device validation.

Current validated baseline:

- manual pause and resume work after the required ADB-granted permission is available;
- schedule settings are persisted;
- the app can plan and run work-profile schedule boundaries with Android exact alarms;
- device and OEM behavior still needs broader validation.

## Who it is for

This app is for users who:

- already have an Android work profile;
- want that work profile to be active during chosen work hours and paused outside them;
- do not have a suitable built-in work-profile schedule feature on their device;
- can grant the required protected Android permission with ADB.

## What it does

- Lists switchable Android work profiles associated with the current user.
- Pauses or resumes the selected work profile.
- Toggles the selected work profile state.
- Stores and runs work-hours schedule settings.
- Provides launcher shortcuts and legacy Android shortcuts for automation apps such as MacroDroid.
- Reschedules the next work-profile boundary after reboot, app update, manual time change, timezone change, and exact-alarm access changes.

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

Development APKs are available as GitHub Actions artifacts from successful `main` runs. Signed release APKs are published through GitHub Releases when release signing is configured.

Basic setup:

1. Install the APK.
2. Grant `android.permission.MODIFY_QUIET_MODE` with ADB.
3. Grant exact alarm access on Android versions that require it.
4. Select a work profile and configure the schedule.

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
