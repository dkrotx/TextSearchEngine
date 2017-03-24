#!/bin/bash

function Err() {
  echo $1 >&2
  exit 1
}

PROTO_PATH=${1:-../../src/main/proto/plain_document.proto}
[ -f ${PROTO_PATH} ] || Err "Proto file not found: ${PROTO_PATH}"
protoc -I$(dirname $PROTO_PATH) --python_out=ppc/protogen/ $PROTO_PATH || Err "Failed to compile ${PROTO_PATH}"
