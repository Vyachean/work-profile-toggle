# Release smoke test

Use this checklist for every signed APK before treating a release as usable.

## Preconditions

- Install the signed release APK from the GitHub Release asset.
- Use a device that already has an Android work profile.
- Grant the quiet-mode permission with ADB after a fresh install:

```sh
adb shell pm grant io.github.vyachean.workprofiletoggle android.permission.MODIFY_QUIET_MODE
```

- On Android 12 and newer, grant exact alarm access when the app reports that it is missing.

## Install and permission checks

1. Install or update the release APK:

   ```sh
   adb install -r work-profile-toggle-<version>.apk
   ```

2. Verify quiet-mode permission:

   ```sh
   adb shell "dumpsys package io.github.vyachean.workprofiletoggle | grep MODIFY_QUIET_MODE"
   ```

3. Expected result: `granted=true` appears for `android.permission.MODIFY_QUIET_MODE`.

## Manual quiet-mode actions

1. Open the app.
2. Confirm that a switchable work profile is selected.
3. Tap pause.
4. Confirm that the work profile becomes paused.
5. Tap resume.
6. Confirm that the work profile becomes active.
7. Tap toggle.
8. Confirm that the state changes and the last result is updated.

## Exact alarm setup

1. Temporarily revoke exact alarm access for the app on Android 12+.
2. Open the app.
3. Expected result: the Schedule section reports exact alarm access missing.
4. Tap the settings button.
5. Grant exact alarm access.
6. Return to the app.
7. Expected result: the app refreshes schedule planning and no longer reports missing exact alarm access.

## Schedule execution

1. Configure resume and pause times so the next boundary is only a few minutes away.
2. Enable the schedule.
3. Confirm that the Schedule section shows the expected next action.
4. Wait for the boundary.
5. Confirm that Android applies the expected work-profile state.
6. Reopen the app.
7. Confirm that the next action moved to the following boundary, not the boundary that just passed.

## Reboot recovery

1. Keep a valid enabled schedule configured.
2. Reboot the device.
3. Open the app after boot.
4. Confirm that the saved schedule is still displayed.
5. Confirm that the next action is calculated from the current time.
6. Wait for the next boundary and confirm that the expected work-profile state is applied.

## Shortcut behavior

1. Add or refresh launcher shortcuts for the selected work profile.
2. Run Pause, Resume, and Toggle shortcuts.
3. Expected result: each shortcut performs its action without bringing the main app UI to the foreground.
4. If an old launcher or MacroDroid shortcut opens the app UI, recreate the shortcut.

## Release acceptance

A release is acceptable when:

- The signed APK installs or updates correctly.
- `MODIFY_QUIET_MODE` is granted and manual actions work.
- Exact alarm access status is clear and recoverable from the app UI.
- Schedule boundaries apply the expected state.
- Next action updates after each boundary.
- Reboot keeps schedule state and reschedules the next boundary.
- Shortcuts perform actions without opening the main UI.
