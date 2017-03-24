#!/bin/bash

set -e 
set -o pipefail

if [[ $# -lt 2 ]]; then
    echo "Usage: $( basename $0 ) PARENT file1 [...]" >&2
    exit 64
fi

CONFIG_TMPL=$( readlink -f $( dirname $0 ) )/config.tmpl
PARTS_DIR=$1
shift

OUT_DIR=$( mktemp -d --tmpdir=$PARTS_DIR data-XXXXXX )
TMP_CONF=$PARTS_DIR/${OUT_DIR##*data-}.conf

OUT_DIR_NAME=$( basename $OUT_DIR )
sed -e "s/%DIRECTORY%/$OUT_DIR_NAME/" $CONFIG_TMPL >$TMP_CONF
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"/../..

$SCRIPT_DIR/build/install/TextSearchEngine/bin/index.sh -t -c $TMP_CONF "$@"
