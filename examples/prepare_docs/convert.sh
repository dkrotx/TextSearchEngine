#!/bin/bash

set -o pipefail

function Err() {
  echo $1 > &2
  exit 1
}

INPUT_FILE=${1:-documents.list}
OUTPUT_FILE=${2:-documents.binary.gz}

[ -f ${INPUT_FILE} ] || Err "Input file not found: ${INPUT_FILE}"

rm -f ${OUTPUT_FILE} || Err "Failed to remove output file: ${OUTPUT_FILE}"

./ppc/convert.py -i ${INPUT_FILE} -p protogen.plain_document_pb2 -m PlainDocument --json | gzip > ${OUTPUT_FILE}
