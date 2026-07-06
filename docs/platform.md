# Platform notes

This document records Android platform assumptions and limitations that should remain available without overloading the README.

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
- disabling quiet mode may require user credentials and can return `false`;
- profile display names may not be available to ordinary apps, so shortcuts use stable technical labels;
- the owner profile is hidden because Android does not allow toggling quiet mode for it;
- devices and OEM ROMs may behave differently.

## Why setup uses ADB

`MODIFY_QUIET_MODE` is a protected Android permission. The app cannot request it through the normal runtime permission dialog used for common permissions such as camera, location, or notifications.

The current supported setup path is an explicit ADB grant:

```sh
adb shell pm grant io.github.vyachean.workprofiletoggle android.permission.MODIFY_QUIET_MODE
```

Some devices or ROMs can still reject the grant.

## Schedule runtime platform behavior

Automatic scheduling depends on Android exact alarms.

The runtime recalculates and reschedules the next boundary after:

- schedule changes;
- selected-profile changes;
- handled schedule boundaries;
- device reboot;
- app update;
- manual time changes;
- timezone changes;
- exact-alarm access changes.

Android background execution and alarm delivery are platform-dependent. Schedule behavior should continue to be validated on real devices and OEM ROMs.

## Shortcut behavior

Launcher shortcuts are best-effort Android integrations:

- dynamic launcher shortcuts are shown only by launchers that support them;
- the legacy Android shortcut picker exists for automation apps that do not expose dynamic launcher shortcuts;
- shortcut actions use a no-display action activity and are intended to switch quiet mode without opening the main UI.
