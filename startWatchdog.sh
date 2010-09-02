#!/bin/sh

# simple startskript for watchdog, needs more options

JAVA="`which java`"

CLASSPATH=""
for N in lib/*.jar; do CLASSPATH="$CLASSPATH$N:"; done
CLASSPATH=".:htroot:$CLASSPATH"

cmdline="$JAVA -Djava.awt.headless=true -classpath $CLASSPATH net.yacy.Watchdog";

$cmdline
