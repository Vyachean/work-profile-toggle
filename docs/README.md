# Project documentation

This directory contains the durable project documentation for product behavior, setup, platform constraints, schedule runtime, release, testing, screenshots, and roadmap status.

## Documents

- [Setup guide](setup.md) — signed and development APK installation, profile selection, ADB permission setup, schedule configuration, exact-alarm access, Diagnostics, shortcuts, and troubleshooting.
- [Product model](product.md) — product goal, user-facing terminology, scope, non-goals, and the current Home and schedule flows.
- [Platform notes](platform.md) — supported Android policy, Android API assumptions, protected permission constraints, exact-alarm behavior, reboot storage limits, and shortcut behavior.
- [Schedule runtime design](schedule-runtime.md) — canonical schedule calculation, execution, result, diagnostics, rescheduling, and real-device validation contract.
- [Release process](release.md) — versioning, tagging, signing, publication, and the distinction between prepared, published, and validated releases.
- [Release smoke test](smoke-test.md) — canonical real-device acceptance checklist for a signed APK.
- [Screenshots](screenshots.md) — deterministic Compose screenshot strategy, required states, display variants, and baseline policy.
- [Roadmap](roadmap.md) — canonical implementation stage, release readiness, and known follow-up status.
- [Documentation maintenance](maintenance.md) — documentation authority, consistency rules, and audit checklist.

Repository-level working rules are in [`AGENTS.md`](../AGENTS.md). The concise public product entry point is [`README.md`](../README.md).

## Authority rule

When documents overlap:

- product terms and user flow come from `product.md`;
- schedule behavior comes from `schedule-runtime.md`;
- release acceptance comes from `smoke-test.md`;
- current plan and readiness come from `roadmap.md`.

GitHub issues and pull requests preserve work history but must not contradict these canonical documents.

## Update rule

Any change that affects user-facing behavior, product scope, setup, platform assumptions, schedule execution, release process, testing, screenshots, or planned work must update the relevant canonical document in the same pull request.
