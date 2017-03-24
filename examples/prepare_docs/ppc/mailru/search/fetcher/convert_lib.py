#!/usr/bin/env python2.7

import argparse
import json
import sys
import struct
from google.protobuf import text_format
from google.protobuf.message import DecodeError
from google.protobuf.internal import encoder
from google.protobuf.internal import decoder

import json_convert


def open_file_read(filename_in):
    if filename_in == '-':
        return sys.stdin
    return open(filename_in, "rb")


def open_file_write(filename_out):
    if filename_out == '-':
        return sys.stdout
    return open(filename_out, "wb")


def text_to_protobuf(line, message, as_json):
    url = message()

    if as_json:
        js = json.loads(line)
        url = json_convert.json2pb(url, js)
    else:
        url.Clear()
        text_format.Merge(line, url)

    result_url = url.SerializeToString()

    delimiter = encoder._VarintBytes(len(result_url))

    return delimiter, result_url


def protobuf_to_text(protobuf_bytes, message, as_json):
    url = message()
    url.ParseFromString(protobuf_bytes)

    if as_json:
        return json.dumps(json_convert.pb2json(url))
    else:
        return text_format.MessageToString(url, True, True)


def convert_protobuf_file(filename_in, filename_out, message, no_length, as_json):
    file_in = open_file_read(filename_in)
    file_out = open_file_write(filename_out)

    try:
        while True:
            if not no_length:
                readbytes = file_in.read(4)
                if len(readbytes) != 4:
                    # hope, there is zero, otherwise file is corrupt
                    break

                length = decoder._DecodeVarint32(readbytes)

                readbytes = file_in.read(length)
                if len(readbytes) != length:
                    # corrupt file?
                    break
            else:
                readbytes = file_in.read()
                if len(readbytes) == 0:
                    # we done here
                    break
            file_out.write(protobuf_to_text(readbytes, message, as_json))
            file_out.write('\n')

    finally:
        file_in.close()
        file_out.close()


def get_class(kls):
    parts = kls.split('.')
    module = ".".join(parts[:-1])
    m = __import__(module)
    for comp in parts[1:]:
        m = getattr(m, comp)
    return m


def convert_text_file(filename_in, filename_out, message, no_length, as_json):
    file_in = open_file_read(filename_in)
    file_out = open_file_write(filename_out)

    try:
        for line in file_in:
            delimiter, result_url = text_to_protobuf(line, message, as_json)

            if not no_length:
                file_out.write(delimiter)
            file_out.write(result_url)

    finally:
        file_in.close()
        file_out.close()


def main():
    parser = argparse.ArgumentParser(description="Convert protobuf from binary to text format and back",
                                     formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('-i', '--input', help="input filename", default='-')
    parser.add_argument('-o', '--output', help="output filename", default='-')
    parser.add_argument('-r', '--reverse', action='store_true', help="convert protobuf to text")
    parser.add_argument('-l', '--no-length', action='store_true',
                        help="do not prepend protobuf by its length - useful only if you need one message")
    parser.add_argument('-p', '--proto-file', default="protogen.fetch_list_pb2",
                        help="path to protobuf description (w/o '.py' suffix)")
    parser.add_argument('-m', '--message', default="FetchUrl",
                        help="message to take from protobuf description")
    parser.add_argument('--json', action='store_true', help="use JSON as text format")
    args = parser.parse_args()

    try:
        message = get_class(args.proto_file + "." + args.message)
    except ImportError:
        sys.stderr.write("Failed to import " + args.message + " from proto-file " + args.proto_file + "\n")
        return -1

    try:
        if args.reverse:
            convert_protobuf_file(args.input, args.output, message, args.no_length, args.json)
        else:
            convert_text_file(args.input, args.output, message, args.no_length, args.json)
    except KeyboardInterrupt:
        sys.stderr.write("Interrupted by keyboard\n")
        return -1
    except DecodeError as err:
        sys.stderr.write("Failed to convert - check if input is correct. Error: '" + err.message + "'\n")
        return -2


if __name__ == '__main__':
    main()
