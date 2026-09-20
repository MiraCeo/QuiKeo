# Capture Coordinates

Collect map links from other apps into an on-device list, then save them as a route. Overlay is
not required — capture is a background intercept while this app is the default browser.

Key files: `:app/LinkInterceptorActivity.kt`, `:core:data/CaptureCoordinatesRepository.kt`,
`:feature:map:impl/CaptureCoordinatesViewModel.kt`, `:core:common/util/CaptureLink.kt`,
`:core:designsystem/component/LjGuidedStepCard.kt`

## Behaviour

Capture mode is **off** by default. Capture is a top-level destination (`CAPTURE_ROUTE`),
listed on Home and in the navigation drawer next to Map / Routes / Favorites. It is **not**
a map FAB. `shouldSkipIdleRedirect` includes `CAPTURE_ROUTE` so the page stays when the user
leaves for Android settings or another app.

The Capture screen shows the feature, its one-time OS setup guidance, and its history:

- **Capture mode + List / Jump** — one overall DataStore switch followed by two independent
  checkboxes (`CaptureCoordinatesRepository`; not part of `ExportData`). List appends a point; Jump
  teleports immediately through `TeleportUseCase`. Either action, both together, and neither are
  supported. If captured points already exist when the overall mode is enabled, a dialog asks
  whether to **Clear** (primary/default action) or **Keep** them before enabling.
- Ready banner when Capture mode and either action are on **and** this app is the default browser
  (that `isDefaultBrowser` boolean is refreshed on `ON_RESUME`, same as the setup cards
  below). Its text explains whether links
  will be listed, jumped to, or both. A concise pass-through banner appears while the overall mode
  is off or neither action is selected.
- Lists captured points in an orange-outlined read-only box (skip exact duplicate of the last point)
- Point order defaults to **Optimize proximity** (`orderedCapturedPoints` → `orderByProximity`,
  same nearest-neighbor as paste coordinates). The on-screen list stays in capture order so
  **Last** (remove last) is unambiguous; proximity is applied when copying or saving the route.
- **Copy** / **Last** / **Clear** sit on one icon row immediately above the route-name field
  (gap between Copy and the remove actions) and Save as route
- Save as a straight route via `RouteRepository.insertRoute` when there are ≥2 points

The Capture screen also shows its one-time OS-role setup guidance directly, as onboarding-style
step cards (`LjGuidedStepCard`, shared with @docs/features/onboarding.md's permission cards —
icon, title, description, and an action button that hides once the step is verified):

- **Default browser** — action opens Default apps (`ACTION_MANAGE_DEFAULT_APPS_SETTINGS`) so the
  user can set **Browser app** to this app. `RoleManager.createRequestRoleIntent(ROLE_BROWSER)` is
  a no-op on many OEMs (including Samsung) and is not used. This is the only one of the four cards
  that can detect its own state (`isCaptureDefaultBrowser()`) — it shows a checkmark and hides its
  button once this app is the default browser; the other three below always show their button,
  since this app cannot detect whether the user has completed them.
- **Turn off Google Maps supported links** — action opens Google Maps' "Open by default" screen.
  The same action reopens that screen later if the user wants to turn the setting back on.
- **Turn on supported links for this app** — action opens this app's own "Open by default" screen.
- **Restore your default browser** — advisory reminder to switch the default browser back once
  done, and reverse the two steps above. Android will not assign the previous browser back
  programmatically; the previous `ROLE_BROWSER` holder is saved in DataStore when the user
  completes the first card, so this app knows which browser to offer as a pass-through choice
  (see below) even before it's restored as the system default.

Below the four cards, a **Pass-through** row opens an in-app browser picker, used when a captured
link's mode doesn't list or jump it (see the intercept table below).

The floating widget overlay is **not** part of intercept. Link handling depends on the
overall **Capture mode** switch and List/Jump actions, not overlay visibility.

## Intercept

`LinkInterceptorActivity` (`singleInstance`, `excludeFromRecents`, `noHistory`, catch-all
`http`/`https` VIEW + BROWSABLE, **and** the Maps host filters that used to live on
`MainActivity`). Catch-all is **not** on `MainActivity` — `MainActivity` keeps `geo:` /
`google.navigation:` / share for chooser → pin. Maps `https` filters were moved off
`MainActivity` so a captured URL does not open the map screen or take focus from the
calling app.

`decideCaptureLink(captureModeEnabled, captureEnabled, jumpEnabled, coords)`:

| Mode | List | Jump | Parsed coords | Action |
|------|------|------|---------------|--------|
| On | On | Off | yes | Append point, toast, finish; the calling app stays in front |
| On | Off | On | yes | Teleport with `TeleportUseCase`, toast, finish; do not append |
| On | On | On | yes | Append, then teleport with `TeleportUseCase`, toast, finish |
| Off | Either | Either | either | Forward without capture or teleport |
| On | Off | Off | either | Forward without capture or teleport |
| On | Either | Either | no | Forward to the chosen pass-through browser |

Coordinates are parsed with `parseUrlCoords` / `parseDeepLinkCoords` (`DeepLinkParser.kt`),
including `https://www.google.com/maps/search/?api=1&query=LAT,LON` through implicit VIEW
intents. Short Maps share links are resolved via
`GoogleMapsShortLinkResolver` before parse.

The app cannot forward to Android's current default browser while it is itself the default, so it
stores the browser that held the role before setup and lets the user choose another installed
browser on the Capture page. Forwarding excludes this app to prevent a loop. With Capture mode off,
Google Maps web links are first sent explicitly to the Google Maps package; this still works when
Maps' supported-links switch is off. If Maps is unavailable, the selected browser is used.

Browser discovery combines installed web-link handlers with apps that advertise a browser launcher.
This matters while the app owns Android's browser role: some phones return only the current role
holder for a generic web query even though other browsers remain installed. The list refreshes when
the Capture screen resumes after a system Settings change.

## Defaults

Capture mode, List, and Jump default **off**. Upgrading from the earlier two-toggle design keeps the
overall mode on when either old action was on. Point order defaults to **Optimize proximity**. The
`AppFeature.CAPTURE_COORDINATES` map-FAB flag may still exist in Settings on older installs
but is no longer rendered on the map or floating map.

The widget panel exposes Capture only as an anchored long-press shortcut on its paste button; the
controls and captured list remain on the main-app Capture screen. Intercept does not require the overlay.
