# Documentation maintenance

## Rule

Documentation must be updated in the same pull request as any change that affects product behavior, user-facing terminology, setup, release, testing, architecture, or roadmap.

## README role

README is the short public entry point. It should explain:

- what the app does;
- who it is for;
- current status;
- what the app does and does not do (scope and non-goals);
- the minimum setup requirement;
- screenshots when stable screenshots exist;
- links to deeper documentation.

README should not become the only source of product, platform, setup, architecture, or release knowledge.

## Docs role

Use `docs/` for durable project knowledge:

- `docs/setup.md` for installation, ADB permission setup, exact alarm access, shortcuts, and user-facing troubleshooting;
- `docs/product.md` for product model, terms, scope, and non-goals;
- `docs/platform.md` for Android API assumptions, platform constraints, OEM behavior, and low-level runtime notes;
- `docs/roadmap.md` for planned stages, open decisions, and roadmap status;
- `docs/screenshots.md` for screenshot strategy;
- `docs/release.md` for release process, signing, release workflows, and release-specific checks;
- `docs/smoke-test.md` for real-device release validation.

## Pull request checklist

For every PR, check whether it changes any of these:

- user-visible UI text or terminology;
- main user flow;
- setup or permission requirements;
- shortcut behavior;
- schedule behavior or schedule settings;
- diagnostics or error reporting;
- build, CI, release, or signing process;
- roadmap or planned work.

If yes, update README or the relevant `docs/` file in the same PR.

## Keeping plans current

The roadmap should distinguish between:

- current implemented behavior;
- planned work;
- open decisions;
- non-goals.

Do not present planned work as implemented behavior.

When a planned item is implemented, update the roadmap status and move any relevant details into product, setup, testing, platform, or release documentation.
