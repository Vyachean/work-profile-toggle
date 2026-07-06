# Agent Instructions

## Project goal

Build a small user-friendly Android app for controlling and scheduling Android work profile pause/resume behavior.

The app should be a simple alternative for users whose devices do not expose a convenient Digital Wellbeing-style work profile schedule.

## Product constraints

- Keep the main UI friendly and setup-first: status, readiness, pause/resume, then schedule.
- Launcher shortcuts are an advanced/secondary surface, not the primary product flow.
- Schedule and background execution are active product functionality; expand them only when setup/status UX, Android capability checks, and runtime diagnostics stay clear.
- Do not add widgets, profile provisioning, app cloning, network features, telemetry, or Shelter-like management features.
- UI text must be in English.
- Prefer the latest two Android versions. Support older Android versions only when it does not add meaningful code, UX, or testing complexity.

## UX constraints

- The default screen must avoid technical Android terms unless setup is blocked and details are explicitly requested.
- Prefer user-facing terms: Work profile, Pause, Resume, Schedule, Setup, Advanced, Diagnostics.
- Keep `quiet mode`, `UserHandle`, profile serial numbers, raw exceptions, and shortcut internals inside Advanced/Diagnostics.
- If setup is incomplete, show the missing requirement and the next action before technical detail.
- Do not show ADB commands as the first thing users see. Show them only when permission setup is required.
- If multiple switchable profiles exist, the main flow should manage one selected profile and keep the raw profile list in Advanced.

## Technical constraints

- Prefer Kotlin.
- Prefer a simple Android architecture over frameworks or abstractions that are not needed for the current step.
- Treat Android profile, permission, and scheduling behavior as device-dependent until verified on real hardware.
- Do not assume profile display names are available to ordinary apps.
- Handle `SecurityException` and unsuccessful quiet-mode requests explicitly.
- Keep ADB permission setup documented and reproducible.
- Do not silently enable new scheduling modes unless the app has the Android capabilities needed to run them reliably.
- Keep schedule runtime documentation current whenever runtime behavior, limitations, or validation status changes.

## Implementation standards

- Work in small PRs with one clear product state per PR.
- Plan user flow before expanding background scheduling behavior.
- Keep public APIs small and typed.
- Avoid broad catch blocks that hide unexpected errors.
- Do not introduce telemetry or analytics.
- Do not request network permissions unless a future requirement explicitly needs them.
- Keep advanced automation integrations, such as MacroDroid-specific access control or Shizuku-like permission management, out of the main path until the built-in schedule is complete.

## Validation

Every implementation PR should include:

- what was changed;
- how it was tested;
- known device/API limitations;
- documentation updates for user-visible behavior or project rules;
- screenshots only when visible UI changed.

CI must pass before merging once the Android project is bootstrapped.
