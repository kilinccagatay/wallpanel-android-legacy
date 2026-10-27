#!/bin/sh

set -eu

if [ -z "${ANDROID_HOME:-}" ]; then
    echo "ANDROID_HOME must point to an Android SDK." >&2
    exit 1
fi

PLATFORM_JAR=$(find "$ANDROID_HOME/platforms" -name android.jar | sort | head -1)
DX=$(find "$ANDROID_HOME/build-tools" -name dx -type f | sort | tail -1)

if [ -z "$PLATFORM_JAR" ] || [ -z "$DX" ]; then
    echo "An Android platform and build-tools version containing dx are required." >&2
    exit 1
fi

OUTPUT_DIR=${1:-build-artifacts/systemui-helper}
CLASSES_DIR="$OUTPUT_DIR/classes"
OUTPUT_JAR="$OUTPUT_DIR/wallpanel-systemui.jar"

mkdir -p "$CLASSES_DIR"
javac -source 7 -target 7 -classpath "$PLATFORM_JAR" \
    -d "$CLASSES_DIR" device-root/SystemUiControl.java
"$DX" --dex --output="$OUTPUT_JAR" "$CLASSES_DIR"

echo "$OUTPUT_JAR"
