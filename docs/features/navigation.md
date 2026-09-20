# Background Navigation

Settings → Menus → Privacy → **Return to home when backgrounded** controls the existing app-level `ON_STOP` redirect. It defaults to **true** to preserve previous releases' behavior.

## Behavior

- Enabled: `LjApp` navigates to `IDLE_ROUTE`, popping destinations above it, when the activity stops. Home, onboarding and Settings remain exempt (including Settings' file-picker flows).
- Disabled: `ON_STOP` does not navigate or clear the back stack. Ordinary app switching keeps the current page.
- An unknown destination or a not-yet-loaded preference never triggers a redirect.
- This is a navigation preference, not screenshot/recents-thumbnail protection. It does not start or stop location simulation.
- A fresh launch without restored state still follows the existing Home/onboarding gate. Explicit notification/deep-link navigation and normal Back behavior are unchanged.
- Rotation and process-death restoration remain subject to each screen's existing saved-state support; this setting does not promise to persist every map camera or editor draft.

## State and persistence

`AppSettings.returnHomeOnBackground` and `SettingsSnapshot.returnHomeOnBackground` default to `true`; DataStore key: `return_home_on_background`. It participates in the settings draft/Save/Discard flow, one-snapshot save, reset, and export/import. Old backups without the field keep the legacy enabled default.

`AppNavigationViewModel` exposes a `StateFlow<Boolean?>` backed by `SharingStarted.Eagerly`, with `null` until the preference is loaded. `LjApp` reads `.value` inside the lifecycle callback instead of capturing a compositional Boolean, so changes are current even when lifecycle-aware UI collectors are suspended. Loading a preference while stopped never triggers a delayed redirect: only an `ON_STOP` event can do so.

## Key files and checks

- `app/.../LjApp.kt`: lifecycle wiring.
- `app/.../AppNavigationViewModel.kt`: current persisted preference.
- `app/.../BackgroundNavigationPolicy.kt`: pure redirect policy.
- `feature/settings/impl`: draft, action, UI and export codec plumbing.
- Tests cover on/off/unloaded/exempt-route policy, eager updates without UI collection, persistence/reset, draft/discard/save, and old/new export compatibility.
- Device regression: toggle and Save, leave/return on map and another content page, check Settings exemption, then repeat after re-enabling. Cold launch and file-picker/deep-link behavior must remain unchanged.
