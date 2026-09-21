# Generating the website changelog

Prose style and audience: the `release-notes` skill owns them. Invoke it for the wording.
This file covers only what is specific to the changelog files.

`docs/wiki/changelog/<version>.json` is the authored source. `docs/wiki/changelog.html` is
generated. Never hand-edit the HTML. Schema and rules: @docs/features/whats-new.md.

1. Pick the target version: the release being prepared, not `AppInfo.VERSION_NAME`.
   release-please bumps that constant only when its PR merges. Never edit the JSON of a
   version that has a git tag.
2. Source list: `git log <last-release-tag>..HEAD --oneline`. Once the release-please
   PR exists, read its body instead — `gh pr view <PR#> --repo <owner>/<repo> --json body`.
   It is grouped into Features/Bug Fixes with commit links, and is ground truth for what shipped.
3. Keep the user-facing `feat`/`fix` commits. Drop chore, refactor, docs, test, and
   internal fixes with no user-visible effect.
4. Reword each one in plain prose, no code symbols, class names, or module names. Same
   audience rule as the rest of `docs/wiki/` — see `docs/wiki/CONTRIBUTING.md`.
5. Add the entries to `docs/wiki/changelog/<target>.json`. Create the file if it is missing.
6. Run `make wiki-changelog`.
7. Cross-check the entries against the release-please PR body so nothing user-visible is dropped.
