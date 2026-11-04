# Samsung computer-off brightness gate review — 2026-11-04

Status: candidate deployed; **physical acceptance PENDING**.

A read-only device observation simultaneously showed:

- Computer endpoint state `off` with a fresh report (about nine seconds old).
- Computer-display helper `OFFLINE owned=false`.
- Android Awake / display ON, brightness 115 in both Android and the driver.
- Samsung BrowserActivityLegacy in the foreground.
- Installed helper hash matched the previous documented revision.

This proves the helper received the off state but its brightness-zero precondition
was not satisfied. Camera events are a plausible reason brightness remains high;
they are not proven by this single snapshot. The user reports locking the Mac
and pressing Esc; the office calendar disappears while the screen remains visible.

## Prepared change

Retain immediate full-off when already dimmed. After a continuously confirmed
OFFLINE interval of 30 seconds, allow full-off even if camera activity keeps
brightness above zero. The existing 10-second polling interval adds latency.
Only the foreground Samsung panel is eligible. ONLINE, GRACE, UNKNOWN, settings
and permission pages are excluded. The Mac-on wake ownership rule is unchanged.
No APK, camera threshold, preference, root launcher, Denver code or device changes
have been made. Existing unrelated dirty files are preserved.

Unit tests PASS for the 29,999/30,000ms boundary, already-dimmed behavior, all
non-offline states, panel foreground, already-asleep and unknown brightness.
Legacy Java/Dex helper build and shell syntax checks PASS. Build is local only.

## Blocker and rollback

Samsung Wi-Fi ADB became offline during the user test. The bounded read-only
recording remains on the device; retrieve it before any optional reboot. No
replacement helper has been installed. Existing runtime remains the rollback.
A later installation must first back up the deployed JAR, boot launcher, private
configuration and preferences, then verify singleton ownership and final hashes.

## Acceptance gate

D01–D08, D11–D13: PENDING physical boot, 30-second timer, full-off on real Mac lock,
return-on-unlock, repeated camera/proximity cycles, manual power/long press and
first touch. D09–D10: N/A for Samsung's intentionally absent battery badge.
D14: PENDING deployed settings comparison; no APK update is planned.
U01–U07 and H01–H10: PENDING applicable general checks; no UI or household
actuation performed. E01: PASS targeted unit/build/syntax checks. E02: PENDING
installation and served revision. E03: PASS private endpoint and logs kept local.
E04: PENDING deployed verification and publication. Overall: PENDING.

### Follow-up

The user confirmed the 2–3 minute normal-camera Mac lock + Esc test did **not**
close Samsung's screen. Targeted Wi-Fi ADB disconnect/reconnect failed afterward,
despite the TCP port accepting connections. One request was sent through the
previously configured Home Assistant Samsung restart helper; HA accepted it, but
phone-side reboot was not verified and ADB remained unavailable over a bounded
12-attempt recovery window. No repeated restart request was sent. User-assisted
restart is requested. The on-device read-only log is persistent and was not
removed; retrieving it and deploying the built fix remain PENDING.

Built candidate JAR SHA-256:
`24d6f6704675bd9b874645e1ee5d7f13bfabaa1b5ed0feb42daba38331e8748f`.
The installed JAR is still the prior version; do not claim this candidate fixed
the device before installation and the real lock/unlock test.

### Installation after user-assisted reboot

The user reports the earlier Home Assistant restart did not occur and manually
restarted Samsung. Wi-Fi ADB returned. The full 72-sample prior recording was
retrieved; 14 samples contain fresh computer `off` while the helper remains
OFFLINE / not-owned and Android Awake / ON at brightness 115.

The original helper was backed up with raw ADB pull (not terminal binary output),
SHA-256 `8343041f5704b35b88611f1dfeca3635c3055242ea5e9f08474f8cf1fe997f7d`.
The candidate JAR was atomically installed, old supervisor stopped and existing
boot launcher invoked. A caller timeout occurred during launcher invocation;
subsequent independent verification confirmed the intended final JAR hash, one
supervisor with one policy worker and ONLINE / not-owned status. Native APK was
not changed. Boot script, config, preferences and backlight script hashes remain
identical to the raw backups. No Denver changes were made.

A fresh bounded on-device recording is running for the real Mac lock/Esc and
unlock test with the camera uncovered. Physical result is PENDING. Restore only
the original JAR and restart this helper's supervisor for rollback; keep all
private settings and the other helpers unchanged. The earlier not-deployed
notes above describe the preceding blocked stage, not the current installation.
