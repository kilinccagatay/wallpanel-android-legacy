# Denver delayed restart investigation — 2026-11-03

A dark Denver was reported after a long idle period. The recorded trigger was
**memory maintenance**, not Home Assistant availability: the watchdog saw
211,056 kB RSS (limit 180,000 kB), after 304 continuous dark seconds, and entered
its force-stop/relaunch path. Its completion log appeared about 144 minutes later.
The camera cannot provide motion wake while its application is stopped.

The original watchdog logged only before the restart sequence and after `am start`.
It did not timestamp force-stop completion, the keyguard check or activity-launch
entry. These historical logs cannot identify which individual command waited.
The underlying Binder/command/kernel cause is therefore **UNCONFIRMED**; it must
not be described as a proven deep-sleep bug or as a repaired root cause.

## Controlled observations

Two temporary on-device scripts executed the same force-stop, two-second delay,
keyguard check and BrowserActivityLegacy launch. The existing watchdog was paused
for each test and resumed by an exit trap. All other helpers remained active.
ADB was disconnected during execution and reconnected afterwards to read results.

- First test: restart sequence completed in approximately four seconds.
- Second test: user covered the camera; Android brightness was zero before the
  test. The restart again completed in approximately four seconds.
- Wall-clock and uptime stage markers agreed in both tests; neither reproduced
  the historical stall. Kernel PowerManagerService wake-lock counters remained
  active across the second handoff, providing no support for the proposed
  suspend explanation in that test.
- Camera ownership returned to the new WallPanel PID. The watchdog was running
  again, not left SIGSTOPped. Preferences and installed watchdog contents
  compared identical to their pre-test snapshots.

These tests used the current app's lower memory footprint. They do not reproduce
the original high-memory condition, exclude an intermittent command stall, or
establish overnight reliability. No memory-pressure stress test was performed.
No APK, helper source, brightness policy, camera setting or HA configuration was
changed. Only this investigation document was added to the repository.

## Remaining diagnostic work

A future reproduction needs monotonic stage timestamps around `am force-stop`,
the keyguard query and `am start`, plus the child PID/wait channel while a command
is actually blocked. Bounded command execution may be an appropriate hardening
change, but it has not been deployed or accepted by this investigation.
The complete regression checklist remains applicable to any resulting runtime fix.
