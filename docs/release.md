# Release process

Stable APKs are published by the `Release` GitHub Actions workflow.

## Preferred release path

1. Configure release signing repository secrets (see [Release APK signing](#release-apk-signing)).
2. Increase both `versionName` and `versionCode` in `app/build.gradle.kts` for every new stable APK. For example, `versionName = "0.1.3"` uses tag `v0.1.3`.
3. Merge the version change to `main`.
4. Let the `Create release tag` workflow create `v<versionName>` and dispatch the `Release` workflow.
5. Install the generated APK on a real device and grant `android.permission.MODIFY_QUIET_MODE` again after a fresh install (see [Setup guide](setup.md#grant-work-profile-control-permission)).
6. On Android 12 and newer, grant exact alarm access if the app reports that it is missing.
7. Validate manual pause, manual resume, exact alarm setup, schedule saving, boundary execution, next-action refresh, reboot recovery, shortcuts, and update with `adb install -r`.

## First release

If the current `versionName` is already correct and no version-change commit is being merged, run `Create release tag` manually from GitHub Actions. It creates the missing `v<versionName>` tag from the current `main` commit and dispatches the `Release` workflow.

## Debug APK signing

GitHub Actions debug APKs are signed with a committed debug-only CI keystore so development artifacts can update each other with `adb install -r`.

Expected debug APK certificate SHA-256:

```text
5f49f7e574cac855329af8151a480f4757615e5b28afae1372e7991a5215cb77
```

CI verifies this fingerprint and fails if the debug APK signing certificate changes.

This key is only for development APKs. It must not be used for release builds.

## Release APK signing

Release APKs are signed only by the `Release` GitHub Actions workflow. The release key must be stored in GitHub Secrets and must not be committed to the repository.

Required repository secrets:

```text
RELEASE_KEYSTORE_BASE64
RELEASE_KEYSTORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

Recommended repository secret:

```text
RELEASE_CERT_SHA256
```

Create a release keystore locally and keep it private:

```sh
keytool -genkeypair \
  -v \
  -keystore work-profile-toggle-release.keystore \
  -alias work-profile-toggle \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Encode it for `RELEASE_KEYSTORE_BASE64`:

```sh
base64 -w 0 work-profile-toggle-release.keystore
```

On systems where `base64` does not support `-w`, remove line breaks before storing the value as a secret.

Set the other secrets to the keystore password, key alias, and key password used when creating the keystore.

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

For user-facing installation and permission setup, see [Setup guide](setup.md).
