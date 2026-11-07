# Keep Samsung asleep through connection errors and false camera motion

Status: Build 21 and matching helper installed; controlled outage PASS; physical/general acceptance PENDING.

A Samsung screen that was fully asleep could later reopen while the Mac remained
locked. Two independent wake paths were reproduced on the real device:

- The previous root helper woke its owned screen after two HTTP/network errors.
  A boolean-only fixture reproduced OFFLINE / owned / Asleep / OFF followed by
  UNKNOWN / not-owned / Awake / ON. Real Home Assistant state was not modified.
- Removing that root wake alone was insufficient: during the next fixture run,
  WallPanel logged camera motion and acquired its screen wake lock despite the
  covered lens. The candidate recorded `external_wake_observed`; the test failed
  and the prior helper/config were automatically restored.

These controlled observations establish actual wake paths. The user's original
nighttime event cannot be attributed retrospectively because Android was rebooted
before those transient logs could be read.

## Current fix

The root policy wakes only for a fresh ONLINE report. UNKNOWN, GRACE and repeated
HTTP failures retain owned sleep. Physical/manual wake is still available.

Samsung Build 21 reads the short-lived root-owned System setting
`wallpanel_samsung_computer_sleep_lease`. While the helper owns full sleep, it
renews an elapsed-time `issued:expiry` lease every polling cycle. It clears the
lease before Mac-driven wake and on normal cleanup. The native guards prevent
screen-wake acquisitions and positive brightness changes while the display is
asleep. Physical power uses Android's original path; brightness changes remain
allowed once Android is interactive. No key mapping, sensor sensitivity or camera
frame rate was changed.

The lease expires after 30 seconds without a working helper, rejects malformed,
future/old-boot and excessively long values, and is read only by the Samsung
package. Denver returns before consulting it. It contains no credentials or
company data and needs no new permission. The candidate APK's signing certificate
and manifest permission set match the installed predecessor.

Transition/action diagnostics are bounded to 64 KiB plus one rotated file. Logs
contain timestamps, booleans, brightness and action names, not request URLs,
credentials, meeting names or camera images. `external_wake_observed` distinguishes
an outside wake from a helper-issued wake but does not guess its source.

## Validation and rollback

PASS: pure lease/presence/wake policy tests; Samsung-only package check; lease
expiry, reboot/future and invalid inputs; legacy Java/Dex helper compilation;
Samsung APK build using compatible JDK 8. A Java 11 build first failed on generated
annotation compatibility; JDK 8 rebuilt all tasks successfully.

The prior exact APK, helper JAR, private config, boot script, backlight script and
preferences are retained in private rollback storage. The original running helper
SHA-256 was `24d6f6704675bd9b874645e1ee5d7f13bfabaa1b5ed0feb42daba38331e8748f`.
Do not publish the private fixture configuration, endpoint, preferences or logs.

This native patch is applied to the existing local Samsung profile without
staging its unrelated pre-existing changes. The guard hooks are independent of
the separate proximity/Smart Stay implementation. No Denver APK is installed.

For rollback, install the exact previous signed APK with data preserved, restore
the original helper JAR and live config, clear the lease setting, and restart only
the Samsung computer-display supervisor. Keep other root helpers and settings.

Final installation hashes, controlled outage duration, live Mac lock/unlock,
physical power, first touch, navigation permission and complete
[regression acceptance](regression-checklist.md) must be recorded after deployment.
Full boot/overnight/general acceptance remains PENDING until actually observed.

## Final controlled installation — 2026-11-05

- Samsung APK versionCode 8421 / `0.8.4 Build 21 Samsung`.
  SHA-256 `229b3ed968f20e116e40dcc2260c4b9bad827dc678aa7d7dd4aa71f98e3bc398`.
- Root JAR SHA-256
  `e1b1225c6788394bd9ddf6c057532a3352b5f2ea096041954215052e801a4b5b`.
- Final root lease writer uses the device's working `settings put system` path,
  bounded by a four-second subprocess abort. The direct root-context provider
  write was rejected on this Android image; that invalid test was aborted, not
  reported as a successful gate test. Empty ONLINE leases are cleared once,
  avoiding a CLI process on every ordinary online poll.
- Final fixture: OFFLINE became owned / Asleep / Display OFF. A valid numeric
  elapsed lease was present. During HTTP503, UNKNOWN stayed owned / Asleep / OFF
  throughout **127 seconds** of samples. A fresh ON report woke the device to
  115/255 and cleared the lease. Actual Mac entity state was never changed.
- The original private live config was then restored and verified byte-identical.
  Boot launcher and backlight helper hashes are also unchanged. All user
  preferences are unchanged; only Chromium's `ResourceExtractor.Version` cache
  timestamp changed after the APK update. Manifest permissions and signer match.
- The temporary local fixture HTTP server was stopped after restoration. The
  final installed 1280 × 720 screenshot retains the existing landscape layout;
  no CSS, icon padding, card selection insets or optical offsets were changed.
  Private screenshots and endpoint/config files are not published.

| Gate | Status | Evidence / limits |
| --- | --- | --- |
| D01 | PENDING | Final cold boot not performed. |
| D02–D05 | PENDING physical | Controlled full-off/outage/ON telemetry PASS; real camera, dim-room, repeated cycles and light observation pending. |
| D06–D08 | PENDING | Physical power, original long-power menu and first touch need user confirmation. |
| D09–D10 | N/A | Samsung intentionally has no battery badge; Denver was not updated. |
| D11 | PENDING | Selected worker was restarted and reports live ONLINE; every configured helper/recovery not retested. |
| D12–D13 | PENDING | Cold boot, extended and overnight reliability not verified. |
| D14 | PASS targeted | Data-preserving signed APK update; user preferences, live config and other helper files preserved. |
| U01 | N/A | Samsung office profile has its own configured tabs. |
| U02–U03 | PASS targeted | Actual installed landscape screenshot retains readable fields and existing layout. |
| U04–U07 | PENDING | Navigation, idle return, sound, gestures and dialogs not physically repeated. |
| U08 | PASS targeted | No UI spacing change; installed cards and selected indicators retain balanced existing spacing. |
| H01, H07–H08 | N/A | Denver-only home arrangement, robot and countdown excluded from this office profile. |
| H02–H06, H09–H11 | PENDING | No live lamp, meeting or printer actuation; existing sources/config unchanged. |
| E01 | PASS | Wake policy and lease tests, JDK8 Samsung build, Java/Dex helper build and shell syntax checks pass. |
| E02 | PASS targeted | APK version and helper hash verified on Samsung, live config restored. |
| E03 | PASS | Only new guard hooks/files and root changes staged; unrelated dirty source and all private data excluded. |
| E04 | PENDING | Documentation/source publication does not close the physical/general gate. |

Overall: controlled reproduction and targeted fix PASS; full acceptance PENDING.
An overnight issue is not declared resolved based on a two-minute test alone.


## User follow-up — 2026-11-05

- PASS targeted: user repeated the real Mac lock → Esc → wait → unlock cycle
  twice and confirmed Samsung woke after unlocking on both attempts. User
  estimates wake latency at slightly more than five seconds; this is not a
  measured upper bound. The helper polls about every ten seconds, in addition
  to upstream report and request latency, so wake is not instantaneous.
- The reply confirms wake on both attempts but does not separately confirm
  complete physical darkness during the locked interval. That part remains
  PENDING for the final installed revision.
- User reports granting the existing navigation Accessibility permission.
  Permission grant alone does not verify key interception, first touch, sound,
  or landscape; those physical checks remain PENDING.
- No polling, APK, runtime setting or permission was changed for this follow-up.
  Full regression and overnight acceptance remain PENDING.

## Samsung automatic-dim camera settling — 2026-11-07

Build 22 (8422) adds a two-second camera-motion settling period immediately
after the Samsung backlight timer dims the display. The camera callback checks
an elapsed-time deadline shared with the timer. Touch, proximity, the manual
power grace period, and the Mac display-ownership gate retain their existing
paths. Denver does not arm this deadline.

Build 21 evidence: roughly 31-second cycles, with a camera event about 0.7
seconds after dim and brightness returning to 115/255. The Mac helper showed
ONLINE without a corresponding wake request. The user covered only the camera
lens for 90 seconds and reported that the screen dimmed and stayed dark.
An exposure transition is a plausible trigger; no camera frames were collected.

Build 22 was installed over Build 21 with the same signer. Installed version
and landscape panel focus were verified. Only Chromium's resource extraction
timestamp differed in the preferences snapshot. The original Build 21 APK is
retained privately for rollback, and the computer-display JAR hash matches the
previous deployment. The office HTML was not redeployed.

| Gate | Status | Evidence or limit |
| --- | --- | --- |
| D01, D06–D08, D12–D13 | PENDING | Cold boot, physical keys, first touch, extended and overnight use have not been repeated. |
| D02–D04 | PASS targeted | Both brightness readings reached zero, stayed there for about 39 seconds, and returned to 115 when the user intentionally moved a hand in front of the lens. Three-cycle and dim-room checks remain pending. |
| D05 | PASS targeted | The repeat test held both brightness readings at zero for at least 137 seconds with the lens uncovered. Earlier shorter wakes could not be classified because movement was uncertain. |
| D09–D10 | N/A | Samsung office profile has no battery badge. |
| D11 | PENDING | Live Mac helper reports ONLINE; all helper supervision paths have not been retested. |
| D14 | PASS targeted | Signed in-place installation, saved preferences and landscape focus checked. |
| U01 | N/A | Samsung uses the office layout. |
| U02–U07 | PENDING | Dashboard content, sound and gestures were not all physically exercised. |
| H01, H07–H08 | N/A | Denver-only arrangement, robot and countdown are absent from the office profile. |
| H02–H06, H09–H10 | PENDING | Live household and dashboard controls were not exercised. |
| E01 | PASS | Offline JDK 8 Samsung debug build, both existing computer wake policy tests and staged diff check passed. |
| E02 | PASS targeted | Build 22 confirmed on the Samsung, with camera wake confirmed by the user. |
| E03 | PASS | Only the intended service and build hunks plus this record are staged. |
| E04 | PENDING | The complete physical and general acceptance gate remains open. |

Overall: targeted camera wake and two-minute dark hold verified; complete
regression remains PENDING.
