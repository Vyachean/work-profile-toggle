# Work Profile Toggle

Minimal Android app for advanced users. It provides launcher shortcuts for Android profile quiet mode.

## Scope

The app is intentionally small:

- create launcher shortcuts for available Android profiles;
- enable quiet mode for a selected profile;
- disable quiet mode for a selected profile;
- toggle quiet mode for a selected profile.

## Non-goals

This app is not a Shelter, Island, or work-profile manager replacement. It must not:

- create, provision, or delete profiles;
- install, clone, freeze, or manage apps inside a profile;
- provide schedules, automations, or background policies;
- duplicate Tasker, MacroDroid, or launcher functionality;
- add a complex settings UI unless a platform limitation makes it unavoidable.

## Platform assumptions

The implementation should be based on Android profile quiet mode APIs:

- `UserManager.getUserProfiles()` for associated user/profile handles;
- `UserManager.isQuietModeEnabled(UserHandle)` for current quiet-mode state;
- `UserManager.requestQuietModeEnabled(...)` for changing quiet mode.

Known constraints to verify during implementation:

- The caller must be valid for quiet-mode changes. Android documents support for the foreground default launcher or callers with `MANAGE_USERS` / `MODIFY_QUIET_MODE`.
- Disabling quiet mode may require user credentials and can return `false`.
- Profile display names may not be available to ordinary apps; shortcuts may need stable technical labels when profile names cannot be resolved.
- Devices and OEM ROMs may behave differently. The first implementation task must prove the API flow on a real device.

## Planned package name

Tentative package name:

```text
io.github.vyachean.workprofiletoggle
```

This can still change before the first release.

## Development status

No APK is available yet. The repository is currently being bootstrapped.

## License

Apache License 2.0.
