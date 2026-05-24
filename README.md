# Work Profile Toggle

Minimal Android app for advanced users. It provides launcher shortcuts for Android profile quiet mode.

## Scope

The app is intentionally small:

- Create launcher shortcuts for available Android profiles.
- Enable quiet mode for a selected profile.
- Disable quiet mode for a selected profile.
- Toggle quiet mode for a selected profile.

## Non-goals

This app is not a Shelter, Island, or work-profile manager replacement. It must not:

- Create, provision, or delete profiles.
- Install, clone, freeze, or manage apps inside a profile.
- Provide schedules, automations, or background policies.
- Duplicate Tasker, MacroDroid, or launcher functionality.
- Add a complex settings UI unless a platform limitation makes it unavoidable.

## Platform assumptions

The implementation is based on Android profile quiet-mode APIs:

- `UserManager.getUserProfiles()` for associated user/profile handles.
- `UserManager.isQuietModeEnabled(UserHandle)` for current quiet-mode state.
- `UserManager.requestQuietModeEnabled(...)` for changing quiet mode.

Verified constraints:

- Profile discovery and quiet-mode readback work from a regular APK.
- Changing quiet mode from a regular APK requires additional access.
- ADB-granted `android.permission.MODIFY_QUIET_MODE` was verified to allow `UserManager.requestQuietModeEnabled(...)` on a real device.
- Disabling quiet mode may require user credentials and can return `false`.
- Profile display names may not be available to ordinary apps; shortcuts may need stable technical labels when profile names cannot be resolved.
- Devices and OEM ROMs may behave differently.

## Package name

```text
io.github.vyachean.workprofiletoggle
```

## ADB permission setup

The app is intended for advanced users. Quiet-mode write access requires the protected Android permission `android.permission.MODIFY_QUIET_MODE`.

After installing the APK, grant the permission from a computer with ADB:

```sh
adb shell pm grant io.github.vyachean.workprofiletoggle android.permission.MODIFY_QUIET_MODE
```

Verify that the permission is granted:

```sh
adb shell "dumpsys package io.github.vyachean.workprofiletoggle | grep MODIFY_QUIET_MODE"
```

The output should show `granted=true` for `android.permission.MODIFY_QUIET_MODE`.

Expected successful setup behavior:

- The app can list associated user/profile handles.
- The app can read quiet-mode state.
- Quiet-mode actions return `true` when Android accepts the requested state change.

Known failure modes:

- `SecurityException: Can't modify quiet mode, caller is neither foreground default launcher nor has MANAGE_USERS/MODIFY_QUIET_MODE permission` means the permission is missing or was not accepted by the device.
- `pm grant` can fail on some devices or ROMs because `MODIFY_QUIET_MODE` is a protected permission. Some OEM Android distributions require additional developer options for privileged ADB operations; for example, Xiaomi/MIUI devices may require enabling `USB Debugging (Security Settings)`.
- Disabling quiet mode can return `false` when Android requires profile credentials or refuses the request.

To revoke the permission:

```sh
adb shell pm revoke io.github.vyachean.workprofiletoggle android.permission.MODIFY_QUIET_MODE
```

To remove the debug build during testing:

```sh
adb uninstall io.github.vyachean.workprofiletoggle
```

If Android reports that the package is installed for a specific user, uninstall it for that user explicitly. Use the user ID from the Android error message:

```sh
adb shell pm uninstall --user <USER_ID> io.github.vyachean.workprofiletoggle
```

## Development

Requirements:

- JDK 17.
- Android SDK with platform 36.

Build and verify locally:

```sh
./gradlew lint test assembleDebug
```

If the executable bit is lost on a Unix-like system, run:

```sh
chmod +x ./gradlew
./gradlew lint test assembleDebug
```

## Development status

The project currently contains a diagnostic Android application proving profile discovery and quiet-mode control through an ADB-granted permission. Final launcher shortcuts are not implemented yet.

## License

MIT License.
