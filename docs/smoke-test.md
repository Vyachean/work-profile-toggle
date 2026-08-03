# Release smoke test

Use this checklist for every signed APK before treating a release as validated.

## Recorded validation

- 2026-07-07: v0.1.3 was reported to work normally on a real device for schedule-driven work-profile Pause and Resume.

The Compose runtime Home screen introduced in v0.1.5 has not yet been recorded as validated on a real device.

## Record before testing

Record:

- app version and tag;
- device model;
- Android version and OEM/ROM version;
- fresh install or update path;
- previous installed version when updating;
- whether the tested APK is the signed GitHub Release asset.

## Preconditions

- Use a device that already has an Android work profile.
- Download the signed `work-profile-toggle-vX.Y.Z.apk` asset from the matching GitHub Release, replacing `X.Y.Z` with the version under test.
- Keep the previous release APK available when testing the update path.
- Keep ADB available for permission and time-related checks.

## Install or update

1. Update an existing release install when validating an upgrade:

   ```sh
   adb install -r work-profile-toggle-vX.Y.Z.apk
   ```

2. Open the app.
3. Confirm that the selected profile and saved schedule remain present.
4. Confirm that the Compose Home screen renders without a blank frame or crash.
5. Verify the installed version:

   ```sh
   adb shell "dumpsys package io.github.vyachean.workprofiletoggle | grep versionName"
   ```

6. Confirm that `versionName` matches the release under test.

For a fresh install, install without `-r` and complete profile selection and permission setup below.

## Compose UI checks

1. Confirm that the Home screen scrolls on a compact display.
2. Increase system font size and confirm that cards, labels, values, and actions remain usable.
3. Check system light appearance.
4. Check system dark appearance.
5. Confirm that the primary action, Setup card, Schedule card, and Advanced card remain visible and understandable.
6. Confirm that long active-day text and next-action text do not make essential actions unreachable.

## Setup and profile selection

1. Confirm that the app offers only switchable profiles.
2. With several profiles available, open **Choose profile**, select one, close the app, and reopen it.
3. Confirm that the selected profile persists.
4. On a fresh install, omit or revoke `MODIFY_QUIET_MODE`:

   ```sh
   adb shell pm revoke io.github.vyachean.workprofiletoggle android.permission.MODIFY_QUIET_MODE
   ```

5. Confirm that Setup reports the permission as missing.
6. Use the Setup card copy action and confirm that the clipboard contains:

   ```sh
   adb shell pm grant io.github.vyachean.workprofiletoggle android.permission.MODIFY_QUIET_MODE
   ```

7. Run the command and tap **Check again**.
8. Confirm that Setup becomes ready without reinstalling the app.

## Manual actions

1. Confirm that the Home screen shows the current selected-profile state.
2. Tap **Pause** and confirm that the profile becomes paused.
3. Tap **Resume** and confirm that the profile becomes active.
4. Open **Advanced → Diagnostics**.
5. Open **Advanced** from the Diagnostics dialog, select a profile, and run **Toggle**.
6. Confirm that the state changes.
7. Reopen Diagnostics and confirm that the last result was updated.

## Schedule editor

1. Set Resume time, Pause time, and Active days.
2. Confirm that all three saved values are visible in the Schedule card.
3. Confirm that Resume time represents the start of the active interval and Pause time represents its end.
4. Enable the schedule.
5. Confirm that the card shows the enabled state and expected next action.
6. Disable and re-enable the schedule.
7. Clear the schedule and confirm that saved values and next action disappear.
8. Recreate the schedule for the remaining tests.

## Same-day and overnight schedules

1. Test a same-day interval where Resume time is earlier than Pause time.
2. Confirm that the next action and resulting state match the interval.
3. Test an overnight interval where Resume time is later than Pause time.
4. Confirm that an Active day represents the day on which the overnight interval begins.
5. Confirm that the profile remains active across midnight until Pause time.

## Exact-alarm recovery

1. Configure a valid schedule.
2. Temporarily revoke exact-alarm access on Android 12 or newer:

   ```sh
   adb shell appops set io.github.vyachean.workprofiletoggle SCHEDULE_EXACT_ALARM deny
   ```

   The exact command and effect can vary by Android or OEM version. Use system settings when the app-op command is unsupported.

3. Confirm that the Schedule card reports missing access and shows the settings action.
4. Enable the schedule and confirm that it is reported as blocked rather than silently scheduled inexactly.
5. Open settings from the app and grant access.
6. Return to the app.
7. Confirm that the blocked state disappears and schedule planning refreshes.

## Schedule execution

1. Configure the next Resume or Pause boundary a few minutes ahead.
2. Confirm that the displayed next action matches the expected operation and time.
3. Wait for the boundary without keeping the app in the foreground.
4. Confirm that Android applies the expected work-profile state.
5. Reopen the app and confirm that the next action moved to the following boundary.
6. Perform a manual action opposite to the schedule's current expected state.
7. Confirm that the next boundary reconciles the profile to the scheduled state.

## Time and timezone changes

1. Keep a valid enabled schedule configured.
2. Change the device time so the next expected boundary changes.
3. Open the app and confirm that the displayed next action is recalculated.
4. Restore automatic time.
5. Change the device timezone.
6. Open the app and confirm that the next boundary is recalculated from local schedule time.
7. Restore the original timezone and confirm planning updates again.

## Reboot recovery

1. Keep a valid enabled schedule configured.
2. Reboot the device.
3. Unlock the device.
4. Open the app.
5. Confirm that the profile selection and schedule remain present.
6. Confirm that the next action is calculated from the current time.
7. Wait for a near-future boundary and confirm that the expected state is applied.

## Diagnostics privacy and usefulness

1. Use **Copy schedule diagnostics** for a configured schedule.
2. Confirm that the payload includes app version, current time and timezone, saved schedule values, exact-alarm state, runtime result, next boundary, and failure category when relevant.
3. Confirm that the payload does not include profile names, serial numbers, `UserHandle` values, or raw profile identifiers.
4. Confirm that blocked and failed runtime states are understandable from the payload.

## Shortcut behavior

1. Refresh launcher shortcuts by opening the app with a selected profile.
2. Run Pause, Resume, and Toggle shortcuts.
3. Confirm that each shortcut performs its action without bringing the main UI to the foreground.
4. Test the legacy shortcut picker through the intended automation app when compatibility is part of the release scope.
5. Recreate old shortcuts if they open the app instead of running the action.

## Release acceptance

A release is accepted when:

- the signed APK installs or updates from the previous release;
- selected profile and schedule data persist across update;
- the Compose Home screen is usable in light, dark, compact, and increased-font configurations;
- profile selection and ADB permission recovery work;
- manual Pause, Resume, and advanced Toggle work;
- same-day and overnight schedule values and semantics are correct;
- exact-alarm access is clear, blocking is explicit, and recovery works;
- schedule boundaries apply the expected state and update the next action;
- manual time, timezone, and reboot events restore correct planning;
- shortcuts perform actions without opening the main UI;
- Diagnostics remains useful and copied schedule diagnostics exclude profile identifiers;
- the recorded test result contains the device and software context needed to reproduce failures.
