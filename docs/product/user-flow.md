# Work Profile Toggle user flow

## Purpose

Work Profile Toggle should feel like a small, friendly replacement for the missing Digital Wellbeing work profile scheduling controls on devices where those controls are unavailable.

The app is for users who already have an Android work profile and want to:

- check whether the work profile is active or paused;
- pause or resume the work profile manually;
- later configure a simple schedule;
- avoid Android internals unless they open Advanced or Diagnostics.

The primary user should not need to understand `quiet mode`, `UserHandle`, profile serial numbers, launcher shortcut internals, or automation tools.

## Product model

The main flow is:

```text
setup -> status -> pause/resume -> schedule
```

The advanced flow is:

```text
ADB setup -> raw profiles -> launcher shortcuts -> diagnostics -> last result
```

The app should always prefer a clear user-facing state before technical detail.

## Main terms

Use these terms in the default UI:

- Work profile
- Active
- Paused
- Pause work profile
- Resume work profile
- Schedule
- Setup
- Advanced
- Diagnostics

Keep these terms out of the default UI unless they appear in Advanced/Diagnostics:

- quiet mode
- UserHandle
- serial number
- profile 0
- dynamic shortcuts
- raw exception details

## First launch states

### Ready

When a switchable work profile exists and the app can control it:

```text
Work Profile

Work profile is active

[Pause work profile]

Schedule
Not configured
[Set up schedule]

Setup
Ready

Advanced
```

If the profile is paused:

```text
Work Profile

Work profile is paused

[Resume work profile]
```

### Setup required

When the app finds a work profile but cannot control it:

```text
Work Profile

Setup required

Work profile: Found
Permission: Missing

[Show setup instructions]
[Check again]

Advanced
```

The app should explain the missing requirement before showing commands or technical details.

### No work profile found

When no switchable profile is available:

```text
No work profile found

This app can only control an existing Android work profile.
Create or enable a work profile first, then check again.

[Check again]
```

The owner profile must not be shown as a controllable profile.

### Multiple profiles found

If exactly one switchable profile exists, the app may select it automatically.

If multiple switchable profiles exist:

```text
Choose work profile

Profile 1
Profile 2
```

After selection, the main screen should manage only the selected profile. Raw identifiers belong in Advanced.

## Permission setup flow

The ADB command should not be the first thing users see. Show it only after the app detects that permission is missing or the user opens setup details.

```text
Permission setup

Connect your phone to a trusted computer and run:

adb shell pm grant io.github.vyachean.workprofiletoggle android.permission.MODIFY_QUIET_MODE

[Copy command]
[Check again]
```

After `Check again`, the app should return to the main screen and show either `Ready` or the still-missing requirement.

## Manual control flow

### Active to paused

Initial state:

```text
Work profile is active
[Pause work profile]
```

After action succeeds:

```text
Work profile is paused
[Resume work profile]
```

### Paused to active

Initial state:

```text
Work profile is paused
[Resume work profile]
```

After action succeeds:

```text
Work profile is active
[Pause work profile]
```

### Action failure

When Android refuses or the app cannot complete the action:

```text
Could not pause work profile

Permission may be missing or the profile may require unlock.

[Show details]
[Check again]
```

`Show details` may include exception class, message, profile serial, operation, and timestamp.

## Schedule flow

Schedule must be introduced only after setup is ready.

### Not configured

```text
Schedule
Not configured
[Set up schedule]
```

### Configure schedule

```text
Schedule

Pause work profile at
18:00

Resume work profile at
09:00

Days
Mon Tue Wed Thu Fri

[Save schedule]
```

### Enabled

```text
Schedule enabled
Next action: Pause today at 18:00
```

### Schedule setup blocked

If Android alarm access is required and unavailable, the app must not silently enable scheduling.

```text
Schedule setup required

Android requires alarm access to run this schedule reliably.

[Open alarm access settings]
[Check again]
```

## Advanced section

Advanced may contain technical and power-user surfaces:

```text
Advanced

ADB setup
Profiles
Launcher shortcuts
Last result
Diagnostics
```

Allowed Advanced details:

- profile serial numbers;
- `UserHandle` values;
- raw quiet-mode state;
- shortcut counts and legacy shortcut picker;
- last action result;
- exception class and message;
- Android API/device limitation notes.

## Empty and edge states

### Selected profile disappeared

```text
Selected work profile is unavailable

Choose another profile or check again.

[Choose profile]
[Check again]
```

### Credential required

If resuming the work profile fails because unlock is required:

```text
Could not resume work profile

Unlock may be required before this profile can be resumed.

[Check again]
[Show details]
```

### Permission revoked

```text
Setup required

Permission: Missing

[Show setup instructions]
[Check again]
```

## Non-goals for the main flow

Do not add these to the primary user path:

- MacroDroid-first setup;
- Shizuku/Magisk-like access control;
- external automation permission management;
- widgets;
- profile provisioning;
- app cloning;
- network features;
- telemetry.

## Acceptance criteria for the first UI implementation

A user should be able to open the app and understand within a few seconds:

1. whether a work profile was found;
2. whether the app is ready to control it;
3. whether the profile is active or paused;
4. which primary action is available;
5. what to do when setup is incomplete;
6. where to find technical details.
