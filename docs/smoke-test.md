# Release smoke test

Use this checklist for every signed APK before treating a release as usable.

## Recorded validation

- 2026-07-07: v0.1.3 was reported to work normally on a real device for schedule-driven work-profile pause and resume.

The Compose runtime Home screen introduced in v0.1.5 has not yet been validated on a real device. The checklist below is required before recording it as validated.

## Preconditions

- Install the signed release APK from the GitHub Release asset.
- Use a device that already has an Android work profile.
- Keep the previous release APK available when testing an update path.

## Install or update

1. Update from v0.1.4 when it is installed:

   ```sh
   adb install -r work-profile-toggle-v0.1.5.apk
   ```

2. Open the app.
3. Confirm that the selected profile and saved schedule remain present.
4. Confirm that the Compose Home screen renders without a blank frame or crash.
5. Confirm that the screen scrolls on a compact display and at increased font size.
6. Check both system light and dark appearance.

For a fresh install, grant the permission after copying the command shown in the Setup card:

```sh
adb shell pm grant io.github.vyachean.workprofiletoggle android.permission.MODIFY_QUIET_MODE
```

## Setup and profile selection

1. Open the profile picker from the Home screen.
2. Confirm that only switchable profiles are offered.
3. Select a profile and reopen the app.
4. Confirm that the selection persists.
5. Revoke or omit `MODIFY_QUIET_MODE` on a fresh install.
6. Confirm that Setup reports the permission as missing and that the copy action copies the correct ADB command.
7. Grant the permission and tap **Check again**.
8. Confirm that Setup becomes ready without reinstalling the app.

## Manual actions

1. Confirm that the Home screen shows the current selected-profile state.
2. Tap **Pause** and confirm that the work profile becomes paused.
3. Tap **Resume** and confirm that the work profile becomes active.
4. Open **Diagnostics**, select the profile from the advanced action dialog, and run **Toggle**.
5. Confirm that the state changes and the last result is updated in Diagnostics.

## Schedule editor

1. Set pause time, resume time, and active days.
2. Confirm that all three saved values are visible in the Schedule card.
3. Enable the schedule.
4. Confirm that the card shows the enabled state and expected next action.
5. Disable and re-enable the schedule.
6. Clear the schedule and confirm that saved values and the next action disappear.

## Exact alarm recovery

1. Configure a valid schedule.
2. Temporarily revoke exact alarm access on Android 12+:

   ```sh
   adb shell appops set io.github.vyachean.workprofiletoggle SCHEDULE_EXACT_ALARM deny
   ```

3. Confirm that the Schedule card reports the blocked state and shows the settings action.
4. Open settings from the app and grant access.
5. Return to the app.
6. Confirm that the blocked state disappears and schedule planning refreshes.

## Schedule execution

1. Configure the next boundary a few minutes ahead.
2. Confirm that the displayed next action matches the expected pause or resume operation.
3. Wait for the boundary.
4. Confirm that Android applies the expected work-profile state.
5. Reopen the app and confirm that the next action moved to the following boundary.

## Reboot recovery

1. Keep a valid enabled schedule configured.
2. Reboot the device.
3. Open the app after boot.
4. Confirm that the profile selection and schedule remain present.
5. Confirm that the next action is calculated from the current time.
6. Wait for the next boundary and confirm that the expected state is applied.

## Shortcut behavior

1. Refresh launcher shortcuts by opening the app with a selected profile.
2. Run Pause, Resume, and Toggle shortcuts.
3. Confirm that each shortcut performs its action without bringing the main UI to the foreground.
4. Recreate old launcher or automation shortcuts if they open the app instead of running the action.

## Release acceptance

A release is acceptable when:

- the signed APK installs or updates from v0.1.4;
- the Compose Home screen is usable in light, dark, compact, and increased-font configurations;
- profile selection and schedule settings persist;
- manual pause and resume work;
- exact alarm access is clear and recoverable;
- schedule boundaries apply the expected state and update the next action;
- reboot restores schedule planning;
- shortcuts perform actions without opening the main UI;
- Diagnostics remains available for technical investigation and advanced toggle actions.
