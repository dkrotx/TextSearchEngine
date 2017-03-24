#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"/../..

TSENGINE_BINDIR=$SCRIPT_DIR/build/install/TextSearchEngine/bin

function search() {
  echo $1
  echo $1 | $TSENGINE_BINDIR/search.sh -c tsengine.conf
}

search "cloudera"
search "palace"
search "wikipedia"
search "kindle"
