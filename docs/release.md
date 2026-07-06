# Release process

Stable APKs are published by the tag-based `Release` GitHub Actions workflow.

## Checklist

1. Configure release signing repository secrets.
2. Keep `versionName` and the tag aligned: `versionName = "0.1.0"` requires tag `v0.1.0`.
3. Increase `versionCode` for every new stable APK.
4. Push the version tag and wait for the `Release` workflow.
5. Install the generated APK on a real device and grant `android.permission.MODIFY_QUIET_MODE` again after a fresh install.
6. Validate manual pause, manual resume, schedule saving, reboot recovery, and update with `adb install -r`.

## Notes

Debug APKs and release APKs use different signing certificates. Moving from debug to release requires uninstalling the debug package once. Future stable APKs must use the same release signing certificate to update existing stable installs.
