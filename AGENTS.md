# Agent Instructions

## Project goal

Build a minimal Android app for advanced users that exposes launcher shortcuts for Android profile quiet mode.

## Product constraints

- Keep the app minimal and technical.
- Do not add schedules, background automation, widgets, profile provisioning, app cloning, or Shelter-like management features.
- Launcher shortcuts are the primary UI surface.
- Any additional UI must be justified by an Android platform limitation or unavoidable permission/setup requirement.
- UI text must be in English.

## Technical constraints

- Prefer Kotlin.
- Prefer a simple Android architecture over frameworks or abstractions that are not needed for the MVP.
- Treat Android profile and permission behavior as device-dependent until verified on real hardware.
- Do not assume profile display names are available to ordinary apps.
- Handle `SecurityException` and unsuccessful quiet-mode requests explicitly.
- Keep ADB permission setup documented and reproducible.

## Implementation standards

- First prove quiet-mode control on a real device before investing in UI polish.
- Keep public APIs small and typed.
- Avoid broad catch blocks that hide unexpected errors.
- Do not introduce telemetry or analytics.
- Do not request network permissions unless a future requirement explicitly needs them.

## Validation

Every implementation PR should include:

- what was changed;
- how it was tested;
- known device/API limitations;
- screenshots only when visible UI changed.

CI must pass before merging once the Android project is bootstrapped.
