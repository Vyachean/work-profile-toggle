# Work Profile Toggle

Minimal Android app for advanced users. It provides shortcuts for Android profile quiet mode.

## Scope

The app is intentionally small:

- List switchable Android profiles associated with the current user.
- Enable quiet mode for a selected profile.
- Disable quiet mode for a selected profile.
- Toggle quiet mode for a selected profile.
- Provide dynamic launcher shortcuts for supported launchers.
- Provide a legacy Android shortcut picker for automation apps such as MacroDroid.

## Non-goals

This app is not a Shelter, Island, or work-profile manager replacement. It must not:

- Create, provision, or delete profiles.
- Install, clone, freeze, or manage apps inside a profile.
- Provide schedules, automations, or background policies.
- Duplicate Tasker, MacroDroid, or launcher functionality.
- Add a complex settings UI unless a platform limitation makes it unavoidable.

## Platform assumptions

The implementation is based on Android profile quiet-mode APIs:

- `UserManager.getUserProfiles()` for associated user/profile handles.
- `UserManager.isQuietModeEnabled(UserHandle)` for current quiet-mode state.
- `UserManager.requestQuietModeEnabled(...)` for changing quiet mode.

Verified constraints:

- Profile discovery and quiet-mode readback work from a regular APK.
- Changing quiet mode from a regular APK requires additional access.
- ADB-granted `android.permission.MODIFY_QUIET_MODE` was verified to allow `UserManager.requestQuietModeEnabled(...)` on a real device.
- Disabling quiet mode may require user credentials and can return `false`.
- Profile display names may not be available to ordinary apps, so shortcuts use stable technical labels.
- The owner profile is hidden because Android does not allow toggling quiet mode for it.
- Devices and OEM ROMs may behave differently.

## Package name

```text
io.github.vyachean.workprofiletoggle
```

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

If Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, the currently installed APK was signed with a different key. Remove it once, then install the current debug APK again:

```sh
adb uninstall io.github.vyachean.workprofiletoggle
adb install app-debug.apk
```

After installing or reinstalling, grant the required permission again.

## ADB permission setup

The app is intended for advanced users. Quiet-mode write access requires the protected Android permission `android.permission.MODIFY_QUIET_MODE`.

Grant the permission from a computer with ADB:

```sh
adb shell pm grant io.github.vyachean.workprofiletoggle android.permission.MODIFY_QUIET_MODE
```

Verify that the permission is granted:

```sh
adb shell "dumpsys package io.github.vyachean.workprofiletoggle | grep MODIFY_QUIET_MODE"
```

The output should show `granted=true` for `android.permission.MODIFY_QUIET_MODE`.

Expected successful setup behavior:

- The app can list associated profile handles.
- The app can read quiet-mode state.
- Quiet-mode actions return `true` when Android accepts the requested state change.

Known failure modes:

- `SecurityException: Can't modify quiet mode, caller is neither foreground default launcher nor has MANAGE_USERS/MODIFY_QUIET_MODE permission` means the permission is missing or was not accepted by the device.
- `pm grant` can fail on some devices or ROMs because `MODIFY_QUIET_MODE` is a protected permission. Some OEM Android distributions require additional developer options for privileged ADB operations; for example, Xiaomi/MIUI devices may require enabling `USB Debugging (Security Settings)`.
- Disabling quiet mode can return `false` when Android requires profile credentials or refuses the request.

To revoke the permission:

```sh
adb shell pm revoke io.github.vyachean.workprofiletoggle android.permission.MODIFY_QUIET_MODE
```

## Launcher shortcuts

Supported launchers can show the app's dynamic shortcuts through the app icon context menu. The app creates quiet-mode actions for switchable profiles only:

- Enable quiet mode.
- Disable quiet mode.
- Toggle quiet mode.

The owner profile is intentionally skipped.

## MacroDroid and automation apps

Some automation apps do not show Android dynamic launcher shortcuts. For compatibility, the app also exposes a legacy `ACTION_CREATE_SHORTCUT` picker.

In MacroDroid, add an action that launches an Android shortcut, choose Work Profile Toggle, then select the required profile/action pair.

Picker-created shortcuts run through a no-display action activity. They are intended to switch quiet mode without bringing Work Profile Toggle to the foreground.

If a shortcut was created before this no-display action path existed, recreate it in MacroDroid.

## Debug APK signing

GitHub Actions debug APKs are signed with a committed debug-only CI keystore so development artifacts can update each other with `adb install -r`.

Expected debug APK certificate SHA-256:

```text
5f49f7e574cac855329af8151a480f4757615e5b28afae1372e7991a5215cb77
```

CI verifies this fingerprint and fails if the debug APK signing certificate changes.

This key is only for development APKs. It must not be used for release builds.

## Release APK signing

Release APKs are signed only by the tag-based `Release` GitHub Actions workflow. The release key must be stored in GitHub Secrets and must not be committed to the repository.

Required repository secrets:

```text
RELEASE_KEYSTORE_BASE64
RELEASE_KEYSTORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
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

Create a release by pushing a version tag:

```sh
git tag v0.1.0
git push origin v0.1.0
```

The workflow builds `assembleRelease`, verifies the APK with `apksigner`, renames it to `work-profile-toggle-<tag>.apk`, and publishes it to the GitHub Release for the tag.

A debug APK and a release APK cannot update each other unless they are signed with the same certificate. This project intentionally uses separate debug and release signing identities, so switching between debug and release installs requires uninstalling the existing package first.

## Development

Requirements:

- JDK 17.
- Android SDK with platform 36.

Build and verify locally:

```sh
./gradlew lint test assembleDebug
```

If the executable bit is lost on a Unix-like system, run:

```sh
chmod +x ./gradlew
./gradlew lint test assembleDebug
```

## Development status

The project currently contains a minimal Android application proving profile discovery, quiet-mode control through an ADB-granted permission, dynamic launcher shortcuts, MacroDroid-compatible legacy shortcuts, stable CI debug APK updates, and release APK publishing infrastructure.

The first signed release still requires configuring release signing secrets and pushing a version tag.

## License

MIT License.
