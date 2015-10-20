#!/bin/bash -e

cd "$( dirname $0 )/build/classes/main"

MAIN_CLASS=$1
shift

CLSPATH=/usr/share/java/gnu-getopt.jar:/home/dkrot/javalibs/boilerpipe-1.2.0/boilerpipe-1.2.0.jar:/home/dkrot/javalibs/boilerpipe-1.2.0/lib/*:.
JAVA_HOME=/opt/java/jdk-latest

/opt/java/jdk-latest/bin/java -cp "$CLSPATH" "$MAIN_CLASS" "$@"
