# Samsung HTTP error-page recovery

This is an independent Samsung SM-J510FN / Android 6 deployment. Do not install
Denver's watchdog or display helpers. No APK or firmware update is involved.

Install `device-root/samsung-http-recovery.sh` as
`/data/local/tmp/samsung-http-recovery.sh` and
`device-root/samsung-http-recovery-service.sh` as
`/data/adb/service.d/93-samsung-http-recovery.sh`. Both files must be root-owned
and mode 700. Put the private health URL in the root-owned mode-600 file
`/data/local/tmp/samsung-http-recovery.url`.

For tested HoaryDash 0.13.0 the health URL ends in `/office%2f`, not `/office/`:
it checks the generated public static HTML without opening unused warming
WebSockets. The browser's real launch address remains unchanged. This path choice
is version-specific; verify 200 for a real directory and 404 for a missing one
when upgrading HoaryDash. No token is stored on the phone for this check.

Run the service script once as root for immediate activation. Magisk runs it at
boot. A PID check plus atomic directory lock prevents duplicate supervisors;
the supervisor starts a replacement worker ten seconds after one exits. The
installed Android shell did not support the tested inherited-flock launch
method, so this service uses a directory lock and validates PID command lines.
The lock is released on orderly exit and stale locks are recovered at startup.

The worker probes with the existing system curl every 30 seconds, with a
five-second timeout. Explicit HTTP 404 or two transport failures arm recovery.
Two later 200 responses permit one panel restart, with a five-minute cooldown.
It restarts only `com.thanksmister.iot.wallpanellegacy.samsung` through its
verified WelcomeActivity path. It never restarts Android or Home Assistant.

Recovery waits while Android is asleep or the foreground window is not the
panel browser. It does not override the Mac computer-display helper, physical
power state, navigation-lock permission windows or Android Settings. Camera,
brightness, motion thresholds, orientation and preferences are not edited.
The server-side HoaryDash guard remains shared by both panels.

Diagnostics: `/data/local/tmp/samsung-http-recovery.log` (bounded),
`/data/local/tmp/samsung-http-recovery-boot.log`, and the supervisor PID file
`/data/local/tmp/samsung-http-recovery.pid`. No meeting names, tokens or health
URLs are logged by the worker.

## Rollback

Remove only `93-samsung-http-recovery.sh` from Magisk's service directory, then
terminate the **verified** supervisor PID from the PID file. Its trap stops its
worker and releases its own lock. Keep the existing 88/90/91 scripts, APK,
preferences and Mac configuration untouched. After stopping the supervisor,
its dedicated worker, private URL and directory lock can be removed.

## Installed verification — 2026-11-03

- Policy tests: `sh tests/root/samsung-http-recovery.test.sh` PASS for 404,
  transport failures, two healthy samples, sleeping/foreign-focus deferral and cooldown.
- Shell syntax checks PASS for worker and service.
- Actual HoaryDash outage and recovery occurred during installation. The phone
  logged one automatic panel reload after the server returned healthy.
- Re-running the launcher retained the same supervisor; stopping its verified
  worker caused a replacement to start without restarting the panel.
- Installed worker SHA-256:
  `5ed05e19e987951d48f349175c2b285cd0608fa5a031e9b619120f62350539c4`.
- Installed service SHA-256:
  `e105d0c9c76017618ecac11e26f1efbdc2c1eaad9d9f3ad847630177817e6427`.
- Existing 88/90/91 service files and preference elements compared identical to
  pre-install backups; camera remained attached to the Samsung app. Build 20,
  45% wake brightness and the existing display profile were retained.

| Acceptance checks | Status | Evidence / limit |
| --- | --- | --- |
| D01, D12 | PENDING | Magisk boot entry installed; phone cold boot not performed. |
| D02 | PASS | User confirmed the requested covered-camera 35-second darkening test after installation. |
| D03–D08 | PENDING overall | User confirmed normal panel, camera wake at the requested 45% level and first touch. Dim-room/three-cycle/two-minute, power-key and full first-touch checks remain pending. |
| D09–D10 | N/A | Samsung is batteryless, native battery badge intentionally disabled; navigation lock is a separate pending check. |
| D11 | PASS for new helper | Duplicate launch and worker recovery tested; other helpers preserved, not deliberately crashed. |
| D13 | PENDING | 30-minute/overnight observation not completed. |
| D14 | PASS | No APK update and all preference elements unchanged. |
| U01 | N/A | Denver's reference tab arrangement is not this Samsung office profile. |
| U02–U07 | PENDING | Complete installed UI, gesture and sound regression not repeated. No UI code changed. |
| H01–H06 | PENDING | No household actuation performed. |
| H07–H08 | N/A | Samsung has neither robot nor delayed all-lights-off controls. |
| H09 | PENDING | Full meeting/calendar expiry regression not repeated. |
| H10 | PENDING overall | Actual HTTP recovery PASS; full unavailable-control/meeting reconnection checks remain pending. |
| E01–E03 | PASS | Tests, source/installed identity, private backup isolation and settings comparisons recorded above. |
| E04 | PENDING | Publication verification recorded separately from physical acceptance. |

Overall: automatic recovery installed and exercised; **complete regression
acceptance remains PENDING**. No claim of full boot or overnight reliability.

### User confirmation

After installation, the user answered “works” to the bundled check for normal
panel appearance, covered-camera darkening after 35 seconds, camera wake at the
requested 45% brightness and first touch. This is a positive single-cycle physical
observation, not proof of cold-boot persistence or the complete regression gate.
No runtime change followed this confirmation.
