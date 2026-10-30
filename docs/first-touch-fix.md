# Jelly Bean first-touch loss with rooted SystemBar hiding

On the tested Android 4.1.1 tablet, the touch driver reported the initial tap,
but Android registered a `hidden nav` / `TYPE_HIDDEN_NAV_CONSUMER` input window.
The activity repeatedly requested `0x707` system UI flags, including
`SYSTEM_UI_FLAG_HIDE_NAVIGATION`. A tap cleared these flags to `0x700` for about
one second before hiding was reapplied. The next tap could then reach the app.

This matches Jelly Bean’s [HideNavInputEventReceiver policy](https://android.googlesource.com/platform/frameworks/base/+/android-4.1.1_r1/policy/src/com/android/internal/policy/impl/PhoneWindowManager.java):
hiding navigation installs an input consumer, and a pointer-down temporarily
forces the controls visible. Dashboard JavaScript cannot recover an event that
Android consumes before it reaches the activity.

## Build 8 behavior

For API 16–18 **with both installed root SystemUI helper files**, WallPanel uses
stable layout/fullscreen flags (`0x704`), leaving actual bar visibility to the
existing root helper. It does not request `HIDE_NAVIGATION` or low-profile mode,
and it skips the legacy touch/focus rehide loop for this path. Helper detection
is cached per activity so it does not stat filesystem paths on every touch.

The battery badge still explicitly shows Android controls and hides them after
12 seconds. The root helper, camera profile, power-key behavior, display timeout,
dashboard, permissions, and native battery badge are unchanged.

API 19+ keeps immersive-sticky mode. API 16–18 without the helper keeps the earlier
fallback; this change is not a portable substitute for root SystemBar control.
No clock/date/audio migration is included in this build.

## Validation and installation

Build with the legacy Java 8 toolchain and Android 27/build-tools 28.0.2:

```sh
bash gradlew assembleProdDebug --offline
```

Verify the APK signature matches the installed application, then use an in-place
update (`adb -s DEVICE_SERIAL install -r ...`). Do not uninstall or clear app data.
Build 8 is version code `8408`; keep the previous APK available for rollback.

After starting BrowserActivityLegacy, check:

- the installed version is Build 8;
- shared preference fingerprints remain unchanged;
- `dumpsys window windows` reports `0x704` on the rooted Jelly Bean path;
- `dumpsys input` has no `hidden nav` input window;
- an isolated tap after waiting reaches the dashboard;
- the battery long press can still show controls, then return to kiosk mode;
- display timeout and camera wake still operate.

Use placeholder device identifiers in shared commands and keep preference files,
raw device logs, and private network addresses out of the repository.

## Observed result on the test tablet

The in-place Build 8 update installed successfully with the matching signing
certificate. The application’s shared-preference fingerprint was unchanged.
After launch, and again after the battery-menu test, system UI flags were `0x704`
and the `hidden nav` input consumer was absent. The camera service still reported
the WallPanel process as its client.

The user confirmed that an isolated first tap after waiting worked and that the
battery long press still showed Android controls and returned to kiosk mode.
This result is for the tested rooted API 16 ROM, not a guarantee for every device.
