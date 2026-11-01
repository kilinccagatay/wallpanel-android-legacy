# Samsung computer-aware screen power

Optional, rooted **SM-J510FN / Android 6 (API 23)** profile. Never install these
helpers on Denver or copy Denver's Allwinner display controls to Samsung.
This adds no APK, firmware, accessibility permission or Mac application update.

## Behavior

The helper reads an existing Home Assistant entity's `last_report_epoch`
attribute. [MacCallIndicator](https://github.com/kilinccagatay/mac-call-indicator)
reports regularly while running and authorized. The meeting boolean is ignored:
“not in a meeting” does not turn off the screen. A fresh heartbeat means reachable;
loss of reports can mean sleep, shutdown, app exit, lost permission or network
trouble, so this is not proof of a shutdown.

- Up to 40 seconds old: normal existing backlight-only behavior.
- 41–119 seconds old: grace period; do not introduce full screen-off.
- At least 120 seconds old: when WallPanel's own 30-second idle timeout has set
  Android brightness to zero and the panel is foreground, send Android's SLEEP
  key event. Android reports Asleep / Display Power OFF.
- A fresh heartbeat wakes a screen put to sleep by this helper. Manual sleep is
  not claimed or undone. Manual/proximity wakes are allowed; the existing idle
  timeout still owns the next dark transition.
- Missing/future/invalid data is unknown. Two consecutive network failures restore
  a helper-owned sleeping screen to normal mode instead of leaving it stuck.

A partial CPU wake lock is held only while the helper owns full screen-off, so
Wi-Fi/heartbeat polling can wake the display. This is full **display** off, not a
claim that the entire phone/CPU is in deep suspend. The camera policy is unchanged.
The root process adds approximately 48 MB RSS on the reference phone, with a
10-second read interval and bounded HTTP connection/read timeouts.

## Build and install

Set `ANDROID_HOME` to an SDK containing a platform `android.jar` and build-tools
with `dx`; use JDK 8 in PATH. Then run:

```sh
sh device-root/build-samsung-computer-display.sh
```

Privately back up current preferences, helpers and rotation settings. Install the
resulting `samsung-computer-display.jar` under `/data/local/tmp/`. Create a **private,
mode-600** `/data/local/tmp/samsung-computer-display.json`:

```json
{
  "url": "http://HOME_ASSISTANT_HOST:4567/api/proxy/api/states/binary_sensor.YOUR_MAC_HEARTBEAT",
  "stale_seconds": 120
}
```

Use your existing trusted read-only endpoint, not credentials in a URL. This
reference endpoint is supplied by the existing HoaryDash setup; the helper does
not create an API, remove authentication or expose Home Assistant externally.
The response must be a HA entity object with `attributes.last_report_epoch`.
No hostname, private mapping or credential belongs in Git.

Install `samsung-computer-display.sh` as
`/data/adb/service.d/88-samsung-computer-display.sh` with mode 755. It backgrounds
its worker, waits at most two minutes for boot, and restarts a stopped worker after
10 seconds. PID/command checks and a local socket prevent duplicate workers.
Existing 89 orientation, 90 Wi-Fi ADB and 91 backlight helpers remain intact.
Status is in `/data/local/tmp/samsung-computer-display.status`; configuration,
logs and ownership files are private device data. The helper never records
meeting names, participants, credentials or report URLs in its own status.

## Tests and rollback

The pure policy test covers fresh/grace/stale/missing/future reports:

```sh
mkdir -p build-artifacts/computer-policy-tests
javac -source 7 -target 7 -d build-artifacts/computer-policy-tests device-root/ComputerDisplayPolicy.java tests/root/ComputerDisplayPolicyTest.java
java -cp build-artifacts/computer-policy-tests ComputerDisplayPolicyTest
```

For controlled device tests, `--test-seconds N` limits execution to 10–180 seconds
and restores a helper-owned sleeping display on normal exit. Use a separate test
endpoint; never falsify production meeting state or interrupt a real call.

To roll back, remove the 88 boot entry, stop its recorded launcher first (so it
cannot restart its worker), then stop the matching SamsungComputerDisplay process.
Send `input keyevent 224` under existing root access to restore the display,
remove the ownership marker, and restore/delete the private configuration as
appropriate. Do not modify power-key layouts or remove the other helpers.
A killed worker releases its Binder CPU wake lock; the ownership marker allows
its supervised replacement to restore a helper-owned screen later.

## Acceptance — 2026-11-02

Native Samsung Build 16 and all preferences stayed unchanged. Direct 20-second
screen-off tests reached Asleep / OFF; the user confirmed close/reopen. In a
separate fixture cycle, OFFLINE / owned / Asleep / OFF was recorded at 51.5s.
A fresh test heartbeat was then written; ONLINE / not-owned / Awake / ON followed
at 63.4s, before the 180-second test limit. The user confirmed automatic reopening.
The fixture meeting value deliberately stayed `off` in both phases. Real HA/Mac
meeting state was never modified. The final configuration uses the existing
live heartbeat and reports ONLINE. Actual Mac sleep/shutdown and a real phone
reboot remain separate pending checks.

Hashes of the existing 89/90/91 helpers and app preferences match before/after.
The launcher was invoked twice without duplicating its worker. The helper runs
independently after the installation ADB connection closes. A stopped online
worker was replaced by the supervisor within 13 seconds, with exactly one new
worker and ONLINE state restored.

| Check | Status | Evidence / remaining work |
| --- | --- | --- |
| D01 | PENDING | Not repeated on the final installation; no full general-feature acceptance claim. |
| D02 | PENDING | Existing timeout unchanged. Fixture reached full OFF after idle; exact 30-second origin and live Mac-off run pending. |
| D03 | PENDING | Backlight-only applies while Mac online; offline mode intentionally uses Android full display-off. Normal mode physical repetition pending. |
| D04 | PENDING | Fixture heartbeat auto-wake and physical reopening confirmed once. Three camera/proximity/dim-room cycles pending. |
| D05 | PENDING | Not repeated on the final installation; no full general-feature acceptance claim. |
| D06 | FAIL | Manual power-off can wake immediately with the camera uncovered. User confirmed at least 5 seconds off with the camera covered and explicitly deferred this issue as TODO while requesting Mac-aware mode enabled. |
| D07 | PENDING | Not repeated on the final installation; no full general-feature acceptance claim. |
| D08 | PENDING | Not repeated on the final installation; no full general-feature acceptance claim. |
| D09 | N/A | Samsung has no battery overlay; Denver is untouched. |
| D10 | N/A | Samsung has no battery overlay; Denver is untouched. |
| D11 | PENDING | Singleton, independent ADB lifetime and 13-second worker replacement confirmed. Recovery of every other configured helper not repeated. |
| D12 | PENDING | Boot entry installed; actual batteryless-phone reboot not performed. |
| D13 | PENDING | Not repeated on the final installation; no full general-feature acceptance claim. |
| D14 | PASS | No APK/firmware update; preferences and existing helper hashes unchanged. |
| U01 | N/A | Office profile omits Denver home/robot/all-house countdown. |
| U02 | PENDING | Not repeated on the final installation; no full general-feature acceptance claim. |
| U03 | PENDING | Not repeated on the final installation; no full general-feature acceptance claim. |
| U04 | PENDING | Not repeated on the final installation; no full general-feature acceptance claim. |
| U05 | PENDING | Not repeated on the final installation; no full general-feature acceptance claim. |
| U06 | PENDING | Not repeated on the final installation; no full general-feature acceptance claim. |
| U07 | PENDING | Not repeated on the final installation; no full general-feature acceptance claim. |
| H01 | N/A | Office profile omits Denver home/robot/all-house countdown. |
| H02 | PENDING | Not repeated on the final installation; no full general-feature acceptance claim. |
| H03 | PENDING | Not repeated on the final installation; no full general-feature acceptance claim. |
| H04 | PENDING | Not repeated on the final installation; no full general-feature acceptance claim. |
| H05 | PENDING | Not repeated on the final installation; no full general-feature acceptance claim. |
| H06 | PENDING | Not repeated on the final installation; no full general-feature acceptance claim. |
| H07 | N/A | Office profile omits Denver home/robot/all-house countdown. |
| H08 | N/A | Office profile omits Denver home/robot/all-house countdown. |
| H09 | PENDING | Not repeated on the final installation; no full general-feature acceptance claim. |
| H10 | PENDING | Not repeated on the final installation; no full general-feature acceptance claim. |
| E01 | PASS | JDK 8 build, pure lease tests and both shell syntax checks passed. |
| E02 | PASS | Verified Samsung-only worker and boot entry installed; live heartbeat reports ONLINE. |
| E03 | PASS | Only intended generic source/tests/guide staged; private endpoint and unrelated dirty files excluded. |
| E04 | PASS | Intended source and guide committed/pushed; remaining hardware checks explicitly pending. |

Overall: PENDING until the full applicable installed-device gate passes.

## Explicitly deferred manual power behavior

The user requested enabling the Mac-aware feature while keeping immediate
camera wake after a short power press as a separate TODO. Power logs identified
WallPanel's application wake lock; with the camera covered and the hand away
from the proximity sensor, the user confirmed at least five seconds off.
This supports a camera/automatic-wake interaction, not a direct helper wake
command. No sensitivity, key layout, APK or sensor setting was changed to mask it.
See [the focused TODO and its acceptance criteria](samsung-display-todo.md).
The full acceptance gate remains FAIL/PENDING; this is not a clean general
regression pass. Real Mac sleep/shutdown and phone cold-boot persistence remain
unverified despite the successful isolated heartbeat test.
