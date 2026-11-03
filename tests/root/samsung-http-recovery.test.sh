#!/bin/sh
set -eu
set -- --library
. ./device-root/samsung-http-recovery.sh
NOW=0 CAN_RELOAD=yes HTTP_CODE=200
recovery_policy; [ "$ACTION" = none ]
HTTP_CODE=000; NOW=30; recovery_policy; [ "$RECOVERY_PENDING" = 0 ]
HTTP_CODE=404; NOW=60; recovery_policy; [ "$RECOVERY_PENDING" = 1 ]
HTTP_CODE=200; NOW=90; recovery_policy; [ "$ACTION" = none ]
CAN_RELOAD=no; NOW=120; recovery_policy; [ "$ACTION" = none ]
CAN_RELOAD=yes; NOW=150; recovery_policy; [ "$ACTION" = reload ]
RECOVERY_PENDING=0; LAST_RECOVERY=$NOW
HTTP_CODE=000; NOW=180; recovery_policy; [ "$RECOVERY_PENDING" = 0 ]
NOW=210; recovery_policy; [ "$RECOVERY_PENDING" = 1 ]
HTTP_CODE=200; NOW=240; recovery_policy; NOW=270; recovery_policy
[ "$ACTION" = none ]
NOW=450; recovery_policy; [ "$ACTION" = reload ]
echo 'Samsung HTTP recovery policy PASS (404, transport, asleep/foreign focus, cooldown)'
