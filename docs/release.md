# Release process

Stable APKs are published by the `Release` GitHub Actions workflow.

## Preferred release path

1. Configure release signing repository secrets (see [Release APK signing](../README.md#release-apk-signing)).
2. Increase both `versionName` and `versionCode` in `app/build.gradle.kts` for every new stable APK. `versionName = "0.1.3"` uses tag `v0.1.3`.
3. Merge the version change to `main`.
4. Let the `Create release tag` workflow create `v<versionName>` and dispatch the `Release` workflow.
5. Install the generated APK on a real device and grant `android.permission.MODIFY_QUIET_MODE` again after a fresh install (see [ADB permission setup](../README.md#adb-permission-setup)).
6. On Android 12 and newer, grant exact alarm access if the app reports that it is missing.
7. Validate manual pause, manual resume, exact alarm setup, schedule saving, boundary execution, next-action refresh, reboot recovery, shortcuts, and update with `adb install -r`.

## First release

If the current `versionName` is already correct and no version-change commit is being merged, run `Create release tag` manually from GitHub Actions. It creates the missing `v<versionName>` tag from the current `main` commit and dispatches the `Release` workflow.

## Smoke test

Use [Release smoke test](smoke-test.md) before treating a signed APK as usable.

The minimum accepted release check is:

- signed APK installs or updates correctly;
- `MODIFY_QUIET_MODE` is granted and manual actions work;
- exact alarm access status is clear and recoverable from the app UI;
- schedule boundaries apply the expected state;
- next action updates after a boundary;
- reboot keeps the schedule and reschedules the next boundary;
- shortcuts perform actions without opening the main UI.

## Manual fallback

A maintainer can still create a release by pushing a version tag manually. The tag must point at a commit whose `app/build.gradle.kts` has the matching `versionName`.

## Notes

Debug APKs and release APKs use different signing certificates. Moving from debug to release requires uninstalling the debug package once. Future stable APKs must use the same release signing certificate to update existing stable installs.
