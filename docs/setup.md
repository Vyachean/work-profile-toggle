# Setup guide

This guide covers installation, required Android access, profile selection, scheduling, shortcuts, and common setup failures.

Work Profile Toggle requires Android 9 (API 28) or newer.

## Install a signed release APK

Download `work-profile-toggle-v<version>.apk` from the matching GitHub Release.

Install for the first time:

```sh
adb install work-profile-toggle-v<version>.apk
```

Update an existing release install:

```sh
adb install -r work-profile-toggle-v<version>.apk
```

A published APK is not considered validated until the real-device [release smoke test](smoke-test.md) is completed.

## Install a development APK

Download the `work-profile-toggle-debug-apk` artifact from a successful GitHub Actions run on `main`, then extract `app-debug.apk`.

Install for the first time:

```sh
adb install app-debug.apk
```

Update an existing development install:

```sh
adb install -r app-debug.apk
```

Release and development APKs use different signing certificates. If Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, uninstall the existing package once, then install the required build:

```sh
adb uninstall io.github.vyachean.workprofiletoggle
adb install app-debug.apk
```

Uninstalling clears the selected profile, saved schedule, permission grant, and app diagnostics.

## Select a work profile

Open the app and select the existing work profile that it should control.

- If one switchable profile is available, the app may select it automatically.
- If several switchable profiles are available, use **Choose profile**.
- The Android owner profile is intentionally excluded because quiet mode cannot be applied to it.

The selected profile is persisted across app restarts and updates that keep app data.

## Grant work-profile control permission

Work-profile pause and resume require the protected Android permission `android.permission.MODIFY_QUIET_MODE`.

When the permission is missing, the Setup card shows a copy action for the required command. Run it from a trusted computer with ADB:

```sh
adb shell pm grant io.github.vyachean.workprofiletoggle android.permission.MODIFY_QUIET_MODE
```

Verify that the permission is granted:

```sh
adb shell "dumpsys package io.github.vyachean.workprofiletoggle | grep MODIFY_QUIET_MODE"
```

The output should show `granted=true` for `android.permission.MODIFY_QUIET_MODE`.

Return to the app and tap **Check again**.

To revoke the permission:

```sh
adb shell pm revoke io.github.vyachean.workprofiletoggle android.permission.MODIFY_QUIET_MODE
```

## Expected behavior after permission setup

After setup succeeds:

- the app can discover associated work-profile handles;
- the app can read the selected work-profile state;
- manual pause and resume actions should work when Android accepts the requested change;
- the schedule runtime can apply the expected work-profile state at schedule boundaries.

## Known permission failure modes

- `SecurityException: Can't modify quiet mode, caller is neither foreground default launcher nor has MANAGE_USERS/MODIFY_QUIET_MODE permission` means the permission is missing or was not accepted by the device.
- `pm grant` can fail on some devices or ROMs because `MODIFY_QUIET_MODE` is a protected permission.
- Some OEM Android distributions require additional developer options for privileged ADB operations. Xiaomi and MIUI devices may require enabling `USB Debugging (Security Settings)`.
- Resuming a profile can return `false` when Android requires profile credentials or refuses the request.

## Configure the schedule

The Schedule card uses these user-facing settings:

- **Resume time** — when the work profile should become active;
- **Pause time** — when the work profile should become paused;
- **Active days** — days on which the active work interval begins.

Configure all three values. When the schedule is complete, the app offers **Enable schedule**.

The intended state is:

```text
inside configured work hours  -> work profile active
outside configured work hours -> work profile paused
```

For an overnight interval, the selected active day is the day on which the active interval begins. For example, Monday resume at 22:00 and pause at 06:00 means active from Monday 22:00 until Tuesday 06:00.

Manual pause and resume actions remain available. The next schedule boundary reconciles the selected profile back to the state expected by the enabled schedule.

## Exact alarm access

Automatic scheduling uses Android exact alarms so boundaries are applied close to the configured time. On Android 12 and newer, Android may require explicit Alarms & reminders access.

After schedule values are configured, the Schedule card shows one of these states:

```text
Exact alarm access: Granted
Exact alarm access: Missing
Exact alarm access: Not required on this Android version
```

When access is missing, use the settings action shown by the app. Grant access, then return to Work Profile Toggle. The app refreshes schedule planning after returning from settings and when Android broadcasts exact-alarm access changes.

An enabled schedule with missing exact-alarm access is shown as blocked and no exact boundary alarm is scheduled.

## Diagnostics

The Home screen keeps technical information under **Advanced → Diagnostics**.

Diagnostics includes:

- the last manual action result;
- profile discovery and raw profile details;
- launcher shortcut status;
- advanced per-profile Pause, Resume, and Toggle actions.

Configured schedules also provide **Copy schedule diagnostics**. The copied payload is intended for troubleshooting and excludes profile names, serial numbers, and user handles.

## Launcher shortcuts

Supported launchers can show dynamic shortcuts through the app icon context menu. The app creates actions for switchable profiles only:

- Pause work profile.
- Resume work profile.
- Toggle work profile.

Dynamic launcher shortcuts run through a no-display action activity and are intended to switch the profile without bringing Work Profile Toggle to the foreground.

## MacroDroid and automation apps

Some automation apps do not show Android dynamic launcher shortcuts. For compatibility, the app also exposes a legacy `ACTION_CREATE_SHORTCUT` picker.

In MacroDroid, add an action that launches an Android shortcut, choose Work Profile Toggle, then select the required profile/action pair.

Picker-created shortcuts run through a no-display action activity. If a shortcut created by an older app version opens the main UI, recreate it.
