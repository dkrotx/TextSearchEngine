#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

pushd ${SCRIPT_DIR}

DOCUMENT_LIST=${1:-"documents.list"}
OUTPUT_GZ=${2:-"../parallel_index/text.parsed/documents.binary.gz"}
./download.sh "https://www.amazon.com/gp/product/B00ZV9PXP2" $DOCUMENT_LIST
./download.sh "https://en.wikipedia.org/wiki/United_Nations_Scientific_Committee_on_the_Effects_of_Atomic_Radiation" $DOCUMENT_LIST
./download.sh "https://en.wikipedia.org/wiki/Palace_of_Westminster" $DOCUMENT_LIST
./download.sh "https://blog.cloudera.com/blog/2016/12/hdfs-datanode-scanners-and-disk-checker-explained/" $DOCUMENT_LIST

./python_proto.sh

./convert.sh ${DOCUMENT_LIST} ${OUTPUT_GZ}

popd
