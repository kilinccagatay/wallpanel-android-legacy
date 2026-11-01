# Samsung native long-press crash — 2026-11-01

Samsung development Build 15 reproduced a native SIGSEGV in the Crosswalk
Chrome_InProcRe renderer when holding a bottom navigation item for 1.5 seconds.
The application process died. This is not a Java exception that can be caught
in a dashboard callback. Private logs contain the original evidence and are not
published.

The Samsung package now consumes Crosswalk's native long-click callback with
`setOnLongClickListener { true }`. Crosswalk's own
[source](https://github.com/crosswalk-project/crosswalk/blob/master/runtime/android/core_internal/src/org/xwalk/core/internal/XWalkViewInternal.java)
identifies this callback as the way to disable its copy/paste action bar.
The custom DOM touch events/timers are not consumed. Denver packages do not
apply this guard. No layout, camera, orientation, power or root-helper change
is part of this fix.

Installed candidate: development Samsung Build 16.

Device checks: three 1.5-second bottom-tab holds preserved the same application
process. Further calendar, clock and weather holds also preserved that process;
no new fatal native/Java entry appeared in the filtered log. The user separately
confirmed that long press no longer crashed. Holding the room light opened its
custom brightness dialog at 0%; X closed it, without changing the light.
SurfaceOrientation remained 1. The only preference difference was Chromium
internal ResourceExtractor version metadata. No household service was actuated. Rollback: the previous
Build 15 APK is preserved on-device, with a private preferences backup before
installation. Do not clear app data to roll back.

## Mandatory gate

| Check | Status | Evidence / remaining work |
| --- | --- | --- |
| D01 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| D02 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| D03 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| D04 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| D05 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| D06 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| D07 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| D08 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| D09 | N/A | Samsung profile has no battery overlay. |
| D10 | N/A | Samsung profile has no battery overlay. |
| D11 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| D12 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| D13 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| D14 | PASS | Before/after preferences match except Chromium internal ResourceExtractor version; Samsung profile/auth/camera settings retained. |
| U01 | N/A | Samsung office profile has a different home layout and no robot/all-house countdown. |
| U02 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| U03 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| U04 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| U05 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| U06 | PENDING | Native tab/text holds no longer crash; custom lamp hold opens once. All timing modes, cancellation and lamp actuation not fully repeated. |
| U07 | PENDING | Custom lamp dialog opened and X closed it. Physical sound and all dialog variants not repeated. |
| H01 | N/A | Samsung office profile has a different home layout and no robot/all-house countdown. |
| H02 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| H03 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| H04 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| H05 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| H06 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| H07 | N/A | Samsung office profile has a different home layout and no robot/all-house countdown. |
| H08 | N/A | Samsung office profile has a different home layout and no robot/all-house countdown. |
| H09 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| H10 | PENDING | Not repeated on the final candidate; physical verification remains required. |
| E01 | PASS | assembleProdSamsungDebug --offline -Pkotlin.incremental=false passed under JDK 8. |
| E02 | PASS | Installed Build 16 verified by PackageManager; native holds and custom lamp dialog tested on the actual Samsung. |
| E03 | PASS | Only this long-click guard and review selected; unrelated dirty files and private logs excluded. |
| E04 | PASS | Only Samsung long-click guard and this review committed/pushed; wider hardware checks remain explicitly pending. |

Overall: PENDING until the complete applicable installed-device gate passes.
