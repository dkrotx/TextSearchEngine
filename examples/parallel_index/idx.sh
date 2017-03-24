#!/bin/bash

PREVT=

step() {
    local now=$( date +%s )
    local prefix="[$( date +%H:%M:%S )]"

    if [[ -n $PREVT ]]; then
        delta=$[ $now - $PREVT ]
        prefix="$prefix [$delta s]"
    fi


    echo "$prefix " "$@"
    PREVT=$now
}

set -e
set -o pipefail

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"/../..

IDX_TEMP_DIR=idx-temp
TSENGINE_BINDIR=$SCRIPT_DIR/build/install/TextSearchEngine/bin

rm -rf $IDX_TEMP_DIR
mkdir $IDX_TEMP_DIR

NFILES=$( find *.parsed/ -name '*.gz' | wc -l )
NPROCS=1 #$( fgrep -c 'physical id' /proc/cpuinfo )

NFILES_PER_PROC=$[ $NFILES / $NPROCS ]

step "Index $NFILES files by $NPROCS processes"
find *.parsed/ -name '*.gz' | xargs -P $NPROCS -n $NFILES_PER_PROC ./idx_part.sh $IDX_TEMP_DIR

DATADIR=data
rm -rf $DATADIR
mkdir $DATADIR

step "optimize index"
sed -e "s/%DIRECTORY%/$DATADIR/" config.tmpl >tsengine.conf
$TSENGINE_BINDIR/optimize.sh -c tsengine.conf $IDX_TEMP_DIR/*.conf

step "make dictionary"
$TSENGINE_BINDIR/make_dict.sh -c tsengine.conf

step "Index created (tsengine.conf)"
