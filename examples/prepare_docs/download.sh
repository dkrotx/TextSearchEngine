#!/bin/bash

DOC=$(wget -O- -q $1 | base64 -w0 );
echo "{ \"url\": \"$1\", \"download_time\":$(date +%s), \"content\":\"$DOC\"}" >> $2; 

