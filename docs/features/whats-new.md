# What's New Popup

An optional badge on the home screen lets users review this version's changes. Closing the
badge acknowledges the version without opening the modal. It stays hidden until the next version.

Key files: `:app/WhatsNewPopup.kt`, `:app/WhatsNewViewModel.kt`,
`:core:data/WhatsNewRepository.kt`.

## Behaviour

- The badge compares the running version with the locally acknowledged version. It appears
  only on the home screen and works offline.
- Opening it acknowledges the version and reads that version's packaged changelog JSON.
  Entries are grouped by category and scope, using the same grouping as the wiki.
- Missing or invalid packaged content shows an inline error; the full changelog link remains
  available. Opening that link requires a browser and network connection.
- Dismissing the badge without opening it does not read any changelog content.

## Single Source of Truth

`docs/wiki/changelog/<version>.json` is the only authored release-note source. Its schema is:

```json
{
  "version": "...",
  "date": "YYYY-MM-DD",
  "entries": [
    {"category": "feat", "scope": "General", "summary": "A user-visible change."}
  ]
}
```

Category is `feat` or `fix`. Scope matches an AGENTS.md Feature Specifications name or
`General`. Summaries are plain, user-facing text.

The `:core:data` asset source set packages these same JSON files into the APK.
`WhatsNewRepository.fetchEntries(version)` reads the matching asset after removing any version
suffix. There is no network fetch and no second Kotlin list to maintain.
`groupWhatsNewEntries()` places features before fixes, with scopes alphabetical within each category.

## Storage

Acknowledgment uses the per-device `whats_new_last_seen_version` DataStore key, defaulting to
an empty string. It is not exported as app data.

## Maintaining the Changelog

1. Pick the target version. It is the **next** release, not `AppInfo.VERSION_NAME`: release-please
   bumps that constant only when it merges the release PR, so until then it names the version
   already shipped. Never edit the JSON of a version that has a git tag (`git tag --list 'v*'`).
   If `docs/wiki/changelog/<next>.json` does not exist, create it.
2. Author that JSON with one entry per user-visible change.
3. Run `make wiki-changelog` to generate `docs/wiki/changelog.html`. Do not edit generated HTML.
4. Verify the target version has a nonempty entries array; tests cover parsing and packaged reads.

Release tooling controls version bumps. No app-side list or URL needs updating for a new release.
