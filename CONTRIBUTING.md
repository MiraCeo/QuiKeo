# Contributing

## Building

Prerequisites: Android Studio (or JDK 17 + Android SDK command-line tools), API 28+ SDK.

```bash
git clone https://github.com/shortcuts/locationjoystick.git
cd locationjoystick
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Release build: `./gradlew assembleRelease`. AAB for Play Store: `make bundle`.

Releases are automated via [release-please](https://github.com/googleapis/release-please): merging to `main` opens/updates a release PR from Conventional Commits; merging that PR tags the version and triggers CI to build, sign, and upload the APK to GitHub Releases.

## Before Opening a PR

1. Read [AGENTS.md](AGENTS.md) — code standards, modules, arch rules, agent tooling.
2. One feature/fix per PR.
3. New feature: open issue first.

## Required Checks

All 3 must pass:

```bash
make format
make lint
make test
```

Fix all lint **error**s before done. Warnings ok; errors not. Never suppress `Errors` rules. Never `@file:Suppress`.

## Reference Docs

| Topic | File |
|-------|------|
| Architecture & modules | [docs/architecture.md](docs/architecture.md) |
| Code style rules | [docs/code-style.md](docs/code-style.md) |
| Testing strategy | [docs/testing.md](docs/testing.md) |
| Domain models | [docs/domain-models.md](docs/domain-models.md) |
| Constants | [docs/constants.md](docs/constants.md) |
| Permissions | [docs/permissions.md](docs/permissions.md) |
| Technical constraints | [docs/technical-constraints.md](docs/technical-constraints.md) |
| Feature specs | [docs/features/](docs/features/) |