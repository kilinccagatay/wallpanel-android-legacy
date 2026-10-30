# Denver display profiles

These root helpers use Allwinner sun5i display ioctls verified on the reference
Denver tablet. They are not portable Android display controls.

`wallpanel-backlight` reads `/data/local/wallpanel-display-mode` at startup:

| File value | When Android brightness is at most 5 |
| --- | --- |
| `backlight-only` | Keep the LCD image/controller on, close its separate backlight-enable gate, and request hardware brightness 0 |
| `lcd-off` (also the default for an absent/unknown value) | Request the prior LCD-off behavior |

The current charger-powered reference uses **backlight-only**. Brightness zero
alone did not eliminate a low-light shimmer on the reference unit. Its verified
`script.fex` maps `lcd_bl_en` to AXP20 GPIO1 (`power1`), separate from LCD power
on GPIO0 (`power0`). The helper closes/opens only GPIO1 through the existing
`axp20-gpio` sysfs interface, checks that it is already an output, and preserves
the sysfs GPIO selector. Do not use this profile on another board without
verifying its wiring and pin configuration. It must not switch GPIO0.

The previous
LCD-off profile remains available for rollback. Neither profile puts Android
into suspend. Both preserve camera motion processing and cap wake brightness
at 191/255 (about 75%). WallPanel continues to own the 30-second inactivity
timer and short power-key toggle; the helper follows its brightness setting.

To select a profile on the verified rooted device, write its exact value to
the mode file and restart the backlight helper, or reboot. The mode file lives
on the data partition and survives reboot; the existing boot launcher starts
the same helper. No APK or firmware image replacement is needed.

### Helper recovery

The app's 30-second inactivity timer writes Android brightness 0. The root
backlight helper must also be alive to close the physical backlight gate.
A PID file alone is insufficient: it can remain after a process disappears.
If brightness reaches 0 but the light stays on, verify the recorded process
exists and its command matches `wallpanel-backlight` before changing app timers.

The boot launcher now remains running and checks the five installed panel
helpers every 30 seconds. It restarts a missing helper after validating its PID
and command, preserving the selected display profile and app preferences.
This is process recovery, separate from the app's inactivity timeout.

Before switching, privately back up the installed helper, boot launcher, key
layout, application preferences, and SettingsProvider database. Those backups
may contain credentials and must never be committed. Reverting the mode to
`lcd-off` and restarting the helper restores the prior display behavior without
changing the app's camera, brightness, timeout, or dashboard configuration.

The legacy LCD-off profile periodically reasserts LCD off after window/USB
events. Backlight-only changes hardware brightness only when Android's setting
changes, avoiding redundant zero writes and LCD reinitialization. Logs identify
the selected mode and requested dark state. A successful ioctl/log is
not a measurement of actual display power or backlight output; verify visually
on the target device. An image visible under external light is expected with
`backlight-only`.
