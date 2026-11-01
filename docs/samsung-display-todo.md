# Samsung display TODO

## Automatic wake immediately after a physical short power press

Status: **OPEN / explicitly deferred by the user, 2026-11-02**.
The user requested that the computer-aware display helper remain enabled while
this behavior is tracked separately.

Observed: the screen can reopen immediately after manual power-off with the
camera uncovered. PowerManager logs show WallPanel's wake lock reopening it.
With the camera fully covered and the hand away from the proximity sensor, the
user confirmed it stays off for at least five seconds. The camera's broad view
and automatic wake path are a plausible trigger; do not assume the helper's
Mac-online transition is the cause. Its live state was ONLINE / owned=false.

Before editing, reproduce and correlate physical-key, SCREEN_OFF, camera motion,
proximity and application wake-lock timestamps. Review the manual-off grace
period for Samsung's original Android power path, rather than changing key maps
or reducing useful camera sensitivity globally.

Acceptance:
- A short power press while lit closes the screen without an immediate accidental
  wake from the same hand movement; the original long-power menu remains intact.
- A second short press wakes normally. Motion/proximity can still wake after the
  agreed grace period; first touch and 30-second timeout remain correct.
- Test covered/uncovered camera, dim light, three cycles, Mac online/offline and
  computer-heartbeat restoration. Keep Denver unchanged and preserve rollback.

Do not mark this TODO complete based only on ADB screen state or a successful
build. The complete regression checklist remains mandatory.
