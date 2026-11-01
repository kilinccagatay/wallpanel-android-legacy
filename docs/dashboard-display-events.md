# Dashboard display-dark notification

The service emits `panel-display-dark` in the active dashboard after it writes
brightness zero, and on forwarded SCREEN_OFF notifications. Backlight-only
operation does not necessarily hide the browser document, so `visibilitychange`
alone cannot reset a dashboard selection reliably.

```js
window.addEventListener('panel-display-dark', function () {
  // Restore the default local UI selection; do not change household devices.
}, false);
```

This event does not change brightness, timeout, motion/face detection, power keys,
root helpers or navigation policy. A brightness write that throws a permission exception does not emit the
zero-brightness event. Real browser hiding also permits a visibility fallback. Old dashboards
can ignore the event; dashboards can also reset on document hiding as fallback.

A Samsung-only development Build 13 was compiled and installed for the three-day
calendar review. Denver's installed APK was not changed. Preserve the prior APK
and preferences for rollback. Build success and a UI reset event do not establish
physical dark/wake or full-device acceptance; use every row of
[the mandatory checklist](regression-checklist.md). The panel repository contains
the scoped three-day calendar evidence and outstanding physical tests.
