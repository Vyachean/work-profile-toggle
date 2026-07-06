# Release process

Stable APKs are published by the `Release` GitHub Actions workflow.

## Preferred release path

1. Configure release signing repository secrets (see [Release APK signing](../README.md#release-apk-signing)).
2. Keep `versionName` in `app/build.gradle.kts` and the release tag aligned: `versionName = "0.1.0"` uses tag `v0.1.0`.
3. Increase `versionCode` in `app/build.gradle.kts` for every new stable APK.
4. Merge the version change to `main`.
5. Let the `Create release tag` workflow create `v<versionName>` and dispatch the `Release` workflow.
6. Install the generated APK on a real device and grant `android.permission.MODIFY_QUIET_MODE` again after a fresh install (see [ADB permission setup](../README.md#adb-permission-setup)).
7. Validate manual pause, manual resume, schedule saving, reboot recovery, and update with `adb install -r`.

## First release

If the current `versionName` is already correct and no version-change commit is being merged, run `Create release tag` manually from GitHub Actions. It creates the missing `v<versionName>` tag from the current `main` commit and dispatches the `Release` workflow.

## Manual fallback

A maintainer can still create a release by pushing a version tag manually. The tag must point at a commit whose `app/build.gradle.kts` has the matching `versionName`.

## Notes

Debug APKs and release APKs use different signing certificates. Moving from debug to release requires uninstalling the debug package once. Future stable APKs must use the same release signing certificate to update existing stable installs.
