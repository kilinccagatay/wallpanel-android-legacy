# Denver retained HTTP error-page recovery

`device-root/wallpanel-watchdog` optionally sources
`/system/xbin/wallpanel-http-recovery`, whose source is in this directory's parent
`device-root/` folder. It checks the dashboard URL from the private root-owned
mode-600 file `/data/local/tmp/wallpanel-recovery-url` on the existing watchdog's
30-second loop. An explicit HTTP 404, or two transport failures, arms recovery.
Two later healthy probes allow one foreground dashboard restart. A five-minute
cooldown prevents repeated reloads. Existing display, camera and memory-recovery
logic is preserved; no APK update or extra daemon is involved.

Install both source files together and preserve the old watchdog for rollback.
Never install this Denver watchdog on Samsung. Restore the original watchdog and
restart only that helper to roll back; preserve all other root helpers/settings.

The matching server guard, setup instructions, tests and installed acceptance
record are maintained in the companion project's
[HoaryDash recovery guide](https://github.com/kilinccagatay/home-assistant-legacy-panels/blob/codex/office-workday-panel/docs/hoarydash-recovery.md).
A live controlled probe outage/recovery was tested. Full reboot, power-key,
physical timeout/wake, sound and long-run regression remain PENDING; the change
is not fully accepted until `regression-checklist.md` is satisfied.
