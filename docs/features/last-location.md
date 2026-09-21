# Last Remembered Location

On app restart, restores the last spoofed position. No manual re-entry needed.

## DataStore Keys (`:core:datastore`)

| Key | Type | Purpose |
|-----|------|---------|
| `REMEMBER_LAST_LOCATION` | `Boolean` | Feature toggle |
| `LAST_LATITUDE` | `Double` | Last spoofed latitude |
| `LAST_LONGITUDE` | `Double` | Last spoofed longitude |

## Behaviour

- On service start: if `REMEMBER_LAST_LOCATION` is `true` and valid coordinates exist, seed initial position from `LAST_LATITUDE`/`LAST_LONGITUDE`.
- The startup restore (`MapController.restoreLastLocationIfNeeded()`, called from `MapController.init` and
  the map screen) is single-flight: an overlapping call is a no-op, a call after a restore that found nothing
  retries. The real-device fallback runs only when location permission is granted.
- While spoofing runs, `MockLocationService.pushLocationUpdate()` — the single 1 Hz tick every mode
  (joystick, walk-to, route replay, roaming, follower catch-up) routes through — writes the current
  position to DataStore, throttled to `AppConstants.LocationConstants.LAST_LOCATION_PERSIST_INTERVAL_MS`
  (5 s) so a battery death or hard reboot loses at most a few seconds of movement instead of resuming
  at wherever the session started. The write is unconditional — `REMEMBER_LAST_LOCATION` gates restore
  only, not persistence.
- `stopSpoofing()` and `onDestroy()` also persist immediately on clean stop / process kill, same as before.
