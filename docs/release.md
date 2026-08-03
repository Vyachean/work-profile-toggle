# Release process

Signed APKs are published by the `Release` GitHub Actions workflow.

Publishing a GitHub Release and validating that release on a real device are separate states. A successfully published APK remains a release candidate until the smoke test is completed and recorded.

## Preferred release path

1. Configure release signing repository secrets (see [Release APK signing](#release-apk-signing)).
2. Merge implementation work only after exact-head CI passes and relevant review feedback is resolved.
3. Increase both `versionName` and `versionCode` in `app/build.gradle.kts` for every new release APK.
4. Update README, roadmap, setup, runtime, and smoke-test documentation when the release changes user-visible behavior or validation scope.
5. Merge the version change to `main`.
6. Let the `Create release tag` workflow create `v<versionName>` and dispatch the `Release` workflow.
7. Confirm that the release workflow builds the tagged commit, verifies the signing certificate, and publishes `work-profile-toggle-v<version>.apk`.
8. Install or update the signed APK on a real device.
9. Complete [Release smoke test](smoke-test.md).
10. Record the validated device, Android/OEM version, install or update path, and result in project documentation.

For example, `versionName = "0.1.5"` uses tag `v0.1.5` and asset name `work-profile-toggle-v0.1.5.apk`.

## Release states

Use these terms consistently:

- **Prepared** — implementation and release documentation are merged; version may not yet be tagged.
- **Published release candidate** — tag and signed APK exist, but real-device acceptance is incomplete.
- **Validated release** — the signed APK passed the documented real-device smoke test.
- **Broadly validated** — behavior has also been checked across additional Android versions or OEMs.

Do not describe a version as validated only because CI, tag creation, signing, or GitHub Release publication succeeded.

## First or manually recovered release

If the current `versionName` is already correct and no version-change commit is being merged, run `Create release tag` manually from GitHub Actions. It creates the missing `v<versionName>` tag from the current `main` commit and dispatches the `Release` workflow.

A maintainer can also push a version tag manually. The tag must point at a commit whose `app/build.gradle.kts` has the matching `versionName`.

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

The workflow verifies the produced APK with `apksigner`. When `RELEASE_CERT_SHA256` is configured, the workflow also compares the signer certificate with the expected fixed fingerprint.

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

## Release smoke test

Use [Release smoke test](smoke-test.md) before treating a signed APK as validated.

The minimum accepted release check includes:

- signed APK installs or updates correctly;
- selected profile and schedule persist across an update;
- the Compose Home screen is usable in required appearance and size variants;
- `MODIFY_QUIET_MODE` setup is recoverable and manual actions work;
- exact-alarm access status is clear and recoverable;
- schedule boundaries apply the expected state;
- next action updates after a boundary and relevant system changes;
- reboot restores planning after unlock;
- shortcuts perform actions without opening the main UI;
- copied schedule diagnostics remain readable and free of profile identifiers.

## Notes

Debug APKs and release APKs use different signing certificates. Moving from debug to release requires uninstalling the debug package once. Uninstalling clears app data and permission grants.

Future stable APKs must use the same release signing certificate to update existing release installs.

For user-facing installation and permission setup, see [Setup guide](setup.md).
