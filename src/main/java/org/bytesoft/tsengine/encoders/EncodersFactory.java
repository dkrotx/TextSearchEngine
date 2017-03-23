package org.bytesoft.tsengine.encoders;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class EncodersFactory {

    public enum EncodingMethods {
        VAR_BYTE, SIMPLE9, FIBONACCI, ELIAS_GAMMA
    }

    private static HashMap<String, EncodingMethods> known_methods = new HashMap<>();
    private EncodingMethods encoder;
    private EncodingMethods decoder;

    static {
        known_methods.put("varbyte", EncodingMethods.VAR_BYTE);
        known_methods.put("simple9", EncodingMethods.SIMPLE9);
        known_methods.put("fibonacci", EncodingMethods.FIBONACCI);
        known_methods.put("elias_gamma", EncodingMethods.ELIAS_GAMMA);
    }

    public static EncodingMethods GetEncoderByName(String method) {
        return known_methods.get(method.toLowerCase());
    }

    public static String GetEncoderNameByID(EncodingMethods method) {
        for (Map.Entry<String, EncodingMethods> entry: known_methods.entrySet()) {
            if (method == entry.getValue())
                return entry.getKey();
        }

        return null;
    }

    public void SetCurrentEncoder(EncodingMethods e) {
        encoder = e;
    }

    public void SetCurrentDecoder(EncodingMethods d) {
        decoder = d;
    }

    public EncodingMethods GetCurrentEncoder() {
        return encoder;
    }

    public EncodingMethods GetCurrentDecoder() {
        return decoder;
    }

    public String GetCurrentEncoderName() {
        return GetEncoderNameByID(GetCurrentEncoder());
    }

    public String GetCurrentDecoderName() {
        return GetEncoderNameByID(GetCurrentDecoder());
    }

    public IntCompressor MakeEncoder() {
        switch(encoder) {
            case VAR_BYTE: return new VarByteEncoder();
            case SIMPLE9: return new Simple9Encoder();
            case FIBONACCI: return new FibonacciEncoder();
            case ELIAS_GAMMA: return new EliasGammaEncoder();
        }

        return null;
    }

    public IntDecompressor MakeDecoder(ByteBuffer block) {
        switch(decoder) {
            case VAR_BYTE: return new VarByteDecoder(block);
            case SIMPLE9: return new Simple9Decoder(block);
            case FIBONACCI: return new FibonacciDecoder(block);
            case ELIAS_GAMMA: return new EliasGammaDecoder(block);
        }

        return null;
    }
}
