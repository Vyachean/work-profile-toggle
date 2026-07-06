# Setup guide

This guide covers installation, required Android access, scheduling, and shortcuts.

## Install a development APK

Download the `work-profile-toggle-debug-apk` artifact from the latest successful GitHub Actions run on `main`, then extract `app-debug.apk`.

Install for the first time:

```sh
adb install app-debug.apk
```

Update an existing development install:

```sh
adb install -r app-debug.apk
```

If Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, the currently installed APK was signed with a different key. Remove it once, then install the current APK again:

```sh
adb uninstall io.github.vyachean.workprofiletoggle
adb install app-debug.apk
```

After installing or reinstalling, grant the required permission again.

## Grant work-profile control permission

Work-profile pause and resume require the protected Android permission `android.permission.MODIFY_QUIET_MODE`.

Grant the permission from a computer with ADB:

```sh
adb shell pm grant io.github.vyachean.workprofiletoggle android.permission.MODIFY_QUIET_MODE
```

Verify that the permission is granted:

```sh
adb shell "dumpsys package io.github.vyachean.workprofiletoggle | grep MODIFY_QUIET_MODE"
```

The output should show `granted=true` for `android.permission.MODIFY_QUIET_MODE`.

To revoke the permission:

```sh
adb shell pm revoke io.github.vyachean.workprofiletoggle android.permission.MODIFY_QUIET_MODE
```

## Expected behavior after permission setup

After setup succeeds:

- the app can list associated work-profile handles;
- the app can read the current work-profile state;
- manual pause and resume actions should work when Android accepts the requested change;
- the schedule runtime can apply the expected work-profile state at schedule boundaries.

## Known permission failure modes

- `SecurityException: Can't modify quiet mode, caller is neither foreground default launcher nor has MANAGE_USERS/MODIFY_QUIET_MODE permission` means the permission is missing or was not accepted by the device.
- `pm grant` can fail on some devices or ROMs because `MODIFY_QUIET_MODE` is a protected permission.
- Some OEM Android distributions require additional developer options for privileged ADB operations. Xiaomi and MIUI devices may require enabling `USB Debugging (Security Settings)`.
- Disabling quiet mode can return `false` when Android requires profile credentials or refuses the request.

## Exact alarm access

Automatic scheduling uses Android exact alarms so work-profile boundaries are applied close to the configured time. On Android 12 and newer, Android may require explicit special access for exact alarms.

The app shows the exact alarm access state in the Schedule section:

```text
Exact alarm access: Granted
Exact alarm access: Missing
Exact alarm access: Not required on this Android version
```

When access is missing, the saved schedule is blocked and the app shows a settings button. Grant exact alarm access in Android settings, then return to the app. The app refreshes schedule planning after returning from settings and when Android broadcasts exact-alarm access changes.

## Work-profile schedule

The schedule applies one selected work-profile state at configured boundaries:

```text
inside configured work hours  -> work profile active
outside configured work hours -> work profile paused
```

The app schedules the next boundary with an exact Android alarm, executes that boundary, recalculates the next boundary, and stores the latest runtime status.

Manual pause and resume actions remain available. The next schedule boundary reconciles the selected profile back to the state expected by the saved schedule.

## Launcher shortcuts

Supported launchers can show dynamic shortcuts through the app icon context menu. The app creates quiet-mode actions for switchable profiles only:

- Pause work profile.
- Resume work profile.
- Toggle work profile.

The owner profile is intentionally skipped.

Dynamic launcher shortcuts run through a no-display action activity and are intended to switch quiet mode without bringing Work Profile Toggle to the foreground.

## MacroDroid and automation apps

Some automation apps do not show Android dynamic launcher shortcuts. For compatibility, the app also exposes a legacy `ACTION_CREATE_SHORTCUT` picker.

In MacroDroid, add an action that launches an Android shortcut, choose Work Profile Toggle, then select the required profile/action pair.

Picker-created shortcuts run through a no-display action activity. They are intended to switch quiet mode without bringing Work Profile Toggle to the foreground.

If a shortcut was created before this no-display action path existed, recreate it in MacroDroid.
