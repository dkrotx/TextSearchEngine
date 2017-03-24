# http://code.google.com/p/protobuf-json/

from google.protobuf.descriptor import FieldDescriptor as FieldD


class ParseError(Exception):
    pass


def json2pb(pb, js):
    """ convert JSON string to google.protobuf.descriptor instance """
    for field in pb.DESCRIPTOR.fields:
        if field.name not in js:
            continue
        if field.type == FieldD.TYPE_MESSAGE:
            pass
        elif field.type in _js2ftype:
            ftype = _js2ftype[field.type]
        else:
            raise ParseError(
                "Field %s.%s of type '%d' is not supported" % (pb.__class__.__name__, field.name, field.type, ))
        value = js[field.name]
        if field.label == FieldD.LABEL_REPEATED:
            pb_value = getattr(pb, field.name, None)
            for v in value:
                if field.type == FieldD.TYPE_MESSAGE:
                    json2pb(pb_value.add(), v)
                else:
                    pb_value.append(ftype(v))
        else:
            if field.type == FieldD.TYPE_MESSAGE:
                json2pb(getattr(pb, field.name, None), value)
            else:
                setattr(pb, field.name, ftype(value))
    for field in pb.DESCRIPTOR.extensions:
        if field.name + ':ext' not in js:
            continue
        if field.type == FieldD.TYPE_MESSAGE:
            pass
        elif field.type in _js2ftype:
            ftype = _js2ftype[field.type]
        else:
            raise ParseError(
                "Field %s.%s of type '%d' is not supported" % (pb.__class__.__name__, field.name, field.type, ))
        value = js[field.name + ':ext']
        if field.label == FieldD.LABEL_REPEATED:
            pb_value = pb.Extensions[field]
            for v in value:
                if field.type == FieldD.TYPE_MESSAGE:
                    json2pb(pb_value.add(), v)
                else:
                    pb_value.append(ftype(v))
        else:
            if field.type == FieldD.TYPE_MESSAGE:
                json2pb(pb.Extensions[field], value)
            else:
                pb.Extensions[field] = ftype(value)
    return pb


def pb2json(pb):
    """ convert google.protobuf.descriptor instance to JSON string """
    js = {}
    # fields = pb.DESCRIPTOR.fields #all fields
    fields = pb.ListFields()  # only filled (including extensions)
    for field, value in fields:
        if field.type == FieldD.TYPE_MESSAGE:
            ftype = pb2json
        elif field.type in _ftype2js:
            ftype = _ftype2js[field.type]
        else:
            raise ParseError(
                "Field %s.%s of type '%d' is not supported" % (pb.__class__.__name__, field.name, field.type, ))
        if field.label == FieldD.LABEL_REPEATED:
            js_value = []
            for v in value:
                js_value.append(ftype(v))
        else:
            js_value = ftype(value)
        if field.is_extension:
            js[field.name + ':ext'] = js_value
        else:
            js[field.name] = js_value
    return js


_ftype2js = {
    FieldD.TYPE_DOUBLE: float,
    FieldD.TYPE_FLOAT: float,
    FieldD.TYPE_INT64: long,
    FieldD.TYPE_UINT64: long,
    FieldD.TYPE_INT32: int,
    FieldD.TYPE_FIXED64: float,
    FieldD.TYPE_FIXED32: float,
    FieldD.TYPE_BOOL: bool,
    FieldD.TYPE_STRING: unicode,
    # FD.TYPE_MESSAGE: pb2json,		#handled specially
    FieldD.TYPE_BYTES: lambda x: x.encode('string_escape'),
    FieldD.TYPE_UINT32: int,
    FieldD.TYPE_ENUM: int,
    FieldD.TYPE_SFIXED32: float,
    FieldD.TYPE_SFIXED64: float,
    FieldD.TYPE_SINT32: int,
    FieldD.TYPE_SINT64: long,
}

_js2ftype = {
    FieldD.TYPE_DOUBLE: float,
    FieldD.TYPE_FLOAT: float,
    FieldD.TYPE_INT64: long,
    FieldD.TYPE_UINT64: long,
    FieldD.TYPE_INT32: int,
    FieldD.TYPE_FIXED64: float,
    FieldD.TYPE_FIXED32: float,
    FieldD.TYPE_BOOL: bool,
    FieldD.TYPE_STRING: unicode,
    # FD.TYPE_MESSAGE: json2pb,	#handled specially
    FieldD.TYPE_BYTES: lambda x: x.decode('string_escape'),
    FieldD.TYPE_UINT32: int,
    FieldD.TYPE_ENUM: int,
    FieldD.TYPE_SFIXED32: float,
    FieldD.TYPE_SFIXED64: float,
    FieldD.TYPE_SINT32: int,
    FieldD.TYPE_SINT64: long,
}