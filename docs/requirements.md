# Requirements

## Goal

Provide a minimal Android app for advanced users that exposes launcher shortcuts for switching quiet mode for Android profiles.

## User-facing behavior

The app must support these shortcut actions:

1. Enable quiet mode for a selected profile.
2. Disable quiet mode for a selected profile.
3. Toggle quiet mode for a selected profile.

The launcher shortcut surface is sufficient for the MVP. A full settings UI is not required unless Android APIs make profile selection impossible without it.

## Profile handling

The app should discover profiles available to the calling user and create actions for each supported profile.

When human-readable profile names are not available, the app may use stable technical labels, for example:

- `Profile 1: Enable quiet mode`
- `Profile 1: Disable quiet mode`
- `Profile 1: Toggle quiet mode`

The exact fallback label format must be deterministic and documented.

## Permission and setup handling

The app must not silently fail when quiet-mode control is unavailable.

Required behavior:

- detect and report `SecurityException`;
- report unsuccessful `requestQuietModeEnabled(...)` calls;
- document any required ADB setup;
- keep the app useful for advanced users even if setup requires manual commands.

## Out of scope

- Work profile creation or provisioning.
- Shelter/Island-like app management.
- App freezing, cloning, installation, or deletion.
- Scheduling or background automation.
- Widgets.
- Telemetry, analytics, or network access.
