# Samsung landscape lock and DiagMonAgent — 2026-11-01

## Scope and rollback

Samsung SM-J510FN / Android 6 only. Denver's APK, settings and root helpers were
not changed. The previous Samsung APK was copied on-device before installation;
preferences and initial rotation settings were backed up privately.

The system initially had auto-rotation enabled and natural/portrait rotation.
After setting auto-rotation off and rotation 1, the active panel still requested
portrait. The Samsung package now filters orientation requests inside the browser
activity, including later embedded-component requests. Other package profiles
retain the requested orientation. This does not alter camera settings, brightness,
power keys, navigation lock, dashboard mappings or authentication.

Development Samsung Build 14 includes this guard. The model/API-guarded
[`samsung-orientation-lock.sh`](../device-root/samsung-orientation-lock.sh) is
installed as a one-shot boot service named `92-samsung-orientation-lock.sh`.
It waits at most two minutes for boot completion, writes persistent Android
rotation settings and exits; it is not another polling daemon.

DiagMonAgent was identified as `com.sec.android.diagmonagent`, version 4.1.15,
in the system's `priv-app` directory. User 0 now has `enabled=3` (disabled-user)
and the package is stopped. A normal shell disable was rejected because this
package uses the system UID; the existing authorized root access applied the
package-specific change. The APK/system partition was not removed or rewritten.

To undo the diagnostic-package change, run `pm enable --user 0
com.sec.android.diagmonagent` under root. To undo the orientation configuration,
remove only the new boot script and restore the privately recorded Android
rotation settings; the previous APK can be installed back without clearing data.
Do not copy these changes to Denver or disable unrelated system packages.

## Acceptance record

Actual reboot/power-cycle persistence is PENDING. The batteryless phone has not
been rebooted as part of this change. A successful build or application restart
is not a cold-boot test. Full device acceptance remains PENDING until every
applicable item in the mandatory checklist has physical evidence.

| ID | Status | Evidence / remaining check |
| --- | --- | --- |
| D01 | PENDING | Application-start orientation is tested separately; actual cold boot still pending. |
| D02 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| D03 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| D04 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| D05 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| D06 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| D07 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| D08 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| D09 | N/A | The office phone does not configure a native battery overlay. |
| D10 | N/A | The office phone does not configure a native battery overlay. |
| D11 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| D12 | PENDING | No reboot performed; boot-service setup is not a reboot persistence test. |
| D13 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| D14 | PASS | Build 14 retained all user preferences; only Chromium internal resource version changed. |
| U01 | N/A | Samsung uses the separate office layout rather than the Denver Home reference. |
| U02 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| U03 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| U04 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| U05 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| U06 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| U07 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| H01 | N/A | Samsung uses the separate office layout rather than the Denver Home reference. |
| H02 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| H03 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| H04 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| H05 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| H06 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| H07 | N/A | Robot controls are absent from this office profile. |
| H08 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| H09 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| H10 | PENDING | Not repeated on this installed revision; previous behavior is not full acceptance. |
| E01 | PASS | Clean Samsung flavor debug build passed under JDK 8 with offline dependencies; shell syntax check passed. |
| E02 | PASS | Installed Samsung Build 14; two force-stop/relaunch cycles each returned SurfaceOrientation=1 and app orientation=0. Actual frame is 1280×720. |
| E03 | PASS | Only the new guard/script/guide are selected; private configuration and unrelated working-tree changes excluded. |
| E04 | PASS | Only the intended guard, boot script and this guide were committed/pushed; remaining device checks explicitly PENDING. |

The two application-restart tests are **not** actual phone reboots.
DiagMonAgent remained disabled-user/stopped after both. Home and settings opened
in landscape; the wider complete physical-feature gate remains PENDING.

![Actual Samsung landscape panel, meeting title redacted](images/samsung-landscape-locked.png)

Work-title pixels were covered with a flat neutral label before publication.
