# Documentation maintenance

## Rule

Documentation must be updated in the same pull request as any change that affects product behavior, user-facing terminology, setup, release, testing, architecture, runtime contracts, or roadmap status.

Documentation is part of the implementation contract. A green build does not make a change complete when the durable project description is stale.

## Canonical sources

Use these sources as the authority for their subject:

- `README.md` — concise public product entry point and current high-level status;
- `docs/README.md` — complete documentation index;
- `docs/product.md` — product terminology, scope, non-goals, and user-flow contract;
- `docs/platform.md` — Android API assumptions, supported-version policy, and platform limitations;
- `docs/schedule-runtime.md` — schedule calculation, execution, diagnostics, and validation contract;
- `docs/roadmap.md` — current implementation stages, release readiness, and known follow-ups;
- `docs/release.md` — signing, tagging, publication, and release-state definitions;
- `docs/smoke-test.md` — canonical real-device release acceptance checklist;
- `docs/screenshots.md` — deterministic screenshot strategy and baseline policy;
- `AGENTS.md` — repository working rules for implementation agents.

GitHub issues and pull requests may track work and preserve history, but they must not remain as conflicting roadmap or behavior specifications. Close or clearly mark obsolete planning issues as superseded.

## README role

README should explain:

- what the app does;
- who it is for;
- current release-candidate and validation status;
- supported Android baseline;
- scope and non-goals;
- the minimum setup sequence;
- screenshots when approved screenshots exist;
- links to deeper documentation.

README should not duplicate detailed runtime, platform, signing, or test contracts.

## Pull request checklist

For every PR, check whether it changes any of these:

- app version or release status;
- user-visible UI text or terminology;
- main user flow;
- setup or permission requirements;
- shortcut behavior;
- schedule behavior, fields, boundary semantics, or diagnostics;
- platform support or Android capability assumptions;
- build, CI, release, tagging, or signing process;
- automated or real-device validation requirements;
- screenshot states or visual acceptance;
- roadmap or planned work.

If yes, update the canonical document in the same PR. Update README only when the change affects the public high-level description.

## Consistency checks

During a documentation audit, explicitly search for stale markers such as:

- previous version numbers;
- planned features that are already implemented;
- old View architecture after Compose migration;
- statements that Compose is not the runtime UI;
- old terms such as generic Start time or End time when the UI uses Resume time and Pause time;
- release publication described as real-device validation;
- setup steps ordered differently from the actual Home flow;
- duplicated smoke-test requirements that disagree with `docs/smoke-test.md`;
- roadmap issues that contradict `docs/roadmap.md`.

Verify important claims against current code, Gradle configuration, Android manifest, and GitHub Actions workflows rather than copying previous documentation forward.

## Keeping plans current

The roadmap should distinguish between:

- current implemented behavior;
- automated validation;
- real-device validation;
- prepared, published, and validated release states;
- planned work;
- open decisions;
- non-goals.

When a planned item is implemented:

1. update its roadmap status;
2. move durable behavior details into the appropriate product, setup, platform, runtime, testing, or release document;
3. remove obsolete future-tense wording;
4. close or supersede any issue that still presents the old plan as current.
