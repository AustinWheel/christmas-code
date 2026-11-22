#!/bin/sh
set -eu
cd "$(dirname "$0")"
# Prefer an explicit runtime; otherwise use IDEA's bundled runtime on this Mac.
if [ -n "${CHRISTMAS_JAVA_HOME:-}" ]; then
    export JAVA_HOME="$CHRISTMAS_JAVA_HOME"
elif [ -d "/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home" ]; then
    export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home"
fi
# The Gradle build also works without a local IDE (downloads its pinned baseline).
if [ -d "/Applications/IntelliJ IDEA.app" ]; then
    exec ./gradlew -PideaPath="/Applications/IntelliJ IDEA.app" "$@"
fi
exec ./gradlew "$@"
