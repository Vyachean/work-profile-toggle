# Project documentation

This directory contains project documentation that should stay current with product, setup, platform, release, and architecture changes.

## Documents

- [Setup guide](setup.md) — user-facing installation, ADB permission setup, exact alarm access, launcher shortcuts, and troubleshooting.
- [Product model](product.md) — what the app is, what it is not, user-facing terms, and core flows.
- [Platform notes](platform.md) — Android API assumptions, protected permission constraints, exact alarm behavior, reboot handling, and shortcut behavior.
- [Schedule runtime design](schedule-runtime.md) — current schedule execution model and constraints.
- [Release process](release.md) — stable APK release flow, signing setup, workflow behavior, and release checks.
- [Release smoke test](smoke-test.md) — real-device validation before treating a signed APK as usable.
- [Screenshots](screenshots.md) — intended approach for stable README screenshots and screenshot automation.
- [Roadmap](roadmap.md) — planned development stages and known follow-up work.
- [Documentation maintenance](maintenance.md) — rules for keeping docs accurate while the product changes.

## Documentation rule

For any change that affects user-facing behavior, product scope, setup, platform assumptions, release process, testing, or planned work, the relevant document should be updated in the same pull request.
