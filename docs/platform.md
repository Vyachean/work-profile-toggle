# Platform notes

This document records Android platform assumptions and limitations that should remain available without overloading the README.

## Supported Android versions

The application currently declares:

- minimum Android version: Android 9 / API 28;
- target Android version: API 36.

Validation and UX work should prioritize the latest two Android versions. Older supported versions are best-effort when they do not require separate product behavior or significant maintenance complexity.

## Android APIs

The implementation is based on Android profile quiet-mode APIs:

- `UserManager.getUserProfiles()` for associated user and profile handles;
- `UserManager.isQuietModeEnabled(UserHandle)` for current quiet-mode state;
- `UserManager.requestQuietModeEnabled(...)` for changing quiet mode.

## Verified constraints

Current verified behavior:

- profile discovery and quiet-mode readback work from a regular APK;
- changing quiet mode from a regular APK requires additional access;
- ADB-granted `android.permission.MODIFY_QUIET_MODE` was verified to allow `UserManager.requestQuietModeEnabled(...)` on a real device;
- resuming a profile may require user credentials and can return `false`;
- profile display names may not be available to ordinary apps, so shortcuts use stable technical labels;
- the owner profile is hidden because Android does not allow toggling quiet mode for it;
- devices and OEM ROMs may behave differently.

## Why setup uses ADB

`MODIFY_QUIET_MODE` is a protected Android permission. The app cannot request it through the normal runtime permission dialog used for common permissions such as camera, location, or notifications.

The current supported setup path is an explicit ADB grant:

```sh
adb shell pm grant io.github.vyachean.workprofiletoggle android.permission.MODIFY_QUIET_MODE
```

Some devices or ROMs can still reject the grant. The app therefore treats permission and quiet-mode operations as device-dependent capabilities rather than guaranteed behavior.

## Exact alarms

Automatic scheduling depends on Android exact alarms.

On Android versions that require Alarms & reminders access, the app checks `AlarmManager.canScheduleExactAlarms()` before planning a boundary. A missing grant blocks an enabled schedule rather than silently falling back to less reliable timing.

Exact alarms improve timing predictability but do not override all Android or OEM power-management behavior. Alarm delivery and the subsequent work-profile API call still require real-device validation.

## Schedule runtime platform behavior

The runtime recalculates and reschedules the next boundary after:

- schedule changes;
- selected-profile changes;
- handled schedule boundaries;
- device reboot;
- app update;
- manual time changes;
- timezone changes;
- exact-alarm access changes;
- returning from exact-alarm settings.

Schedule state is stored in credential-protected app storage. Direct Boot support is not enabled, so persisted state becomes available after the device has been unlocked following a reboot.

Android background execution and work-profile behavior remain platform-dependent. The detailed runtime contract is in [Schedule runtime design](schedule-runtime.md).

## Shortcut behavior

Launcher shortcuts are best-effort Android integrations:

- dynamic launcher shortcuts are shown only by launchers that support them;
- the legacy Android shortcut picker exists for automation apps that do not expose dynamic launcher shortcuts;
- shortcut actions use a no-display action activity and are intended to switch quiet mode without opening the main UI;
- old shortcuts may need to be recreated after behavior or intent-contract changes.
