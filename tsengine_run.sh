#!/bin/bash -e

MAIN_CLASS=$1
shift

PROJECT_CLASS_DIR="$( dirname $0 )/build/classes/main"
CLSPATH=/usr/share/java/gnu-getopt.jar:/home/dkrot/javalibs/boilerpipe-1.2.0/boilerpipe-1.2.0.jar:/home/dkrot/work/lemmatizer_jni/lemmatizer_native.jar:/home/dkrot/javalibs/boilerpipe-1.2.0/lib/*:/home/dkrot/.m2/repository/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar:/home/dkrot/.m2/repository/com/google/protobuf/protobuf-java/2.6.1/protobuf-java-2.6.1.jar:.
JAVA_HOME=/opt/java/jdk-latest

env LD_LIBRARY_PATH=/home/dkrot/work/lemmatizer_jni:$LD_LIBRARY_PATH \
    /opt/java/jdk-latest/bin/java -cp "$CLSPATH:$PROJECT_CLASS_DIR" "$MAIN_CLASS" "$@"
