# Documentation maintenance

## Rule

Documentation must be updated in the same pull request as any change that affects product behavior, user-facing terminology, setup, release, testing, architecture, or roadmap.

## README role

README is the short entry point. It should explain:

- what the app does;
- who it is for;
- current scope and non-goals;
- basic setup;
- links to deeper documentation.

README should not become the only source of product and architecture knowledge.

## Docs role

Use `docs/` for durable project knowledge:

- product model and terms;
- roadmap and planned stages;
- setup and platform limitations;
- release process;
- testing and screenshot strategy;
- architecture decisions that should survive individual PRs.

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

When a planned item is implemented, update the roadmap status and move any relevant details into product, setup, testing, or release documentation.
