# Generating the website changelog

Prose style and audience: the `release-notes` skill owns them. Invoke it for the wording.
This file covers only what is specific to `docs/wiki/changelog.html`.

1. Source list: `git log <last-release-tag>..HEAD --oneline`. Once the release-please PR
   exists, read its body instead — `gh pr view <PR#> --repo <owner>/<repo> --json body`.
   It is already grouped into Features/Bug Fixes with commit links, and is ground truth
   for what shipped.
2. Keep the user-facing `feat`/`fix` commits. Drop chore, refactor, docs, test, and
   internal fixes with no user-visible effect.
3. Reword each one in plain prose, no code symbols, class names, or module names. Same
   audience rule as the rest of `docs/wiki/` — see `docs/wiki/CONTRIBUTING.md`.
4. Prepend an `<h3 id="vXYZ">` section to `docs/wiki/changelog.html`, newest release
   first, linking to `https://github.com/<owner>/<repo>/releases/tag/vX.Y.Z`. Adding it
   before the tag exists is fine — release-please creates the tag when the PR merges.
5. Cross-check the final bullets against the release-please PR body so nothing
   user-visible is dropped.
