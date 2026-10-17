#!/bin/sh
#
# Copyright 2015 the original author or authors.
#
GRADLE_OPTS="${GRADLE_OPTS:-"-Xmx64m -Dorg.gradle.daemon=false"}"
APP_HOME=$(dirname "$0")
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
exec java $GRADLE_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
