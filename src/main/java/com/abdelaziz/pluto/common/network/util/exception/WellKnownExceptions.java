package com.abdelaziz.pluto.common.network.util.exception;

import com.abdelaziz.pluto.common.network.util.exception.QuietDecoderException;

public enum WellKnownExceptions {
    ;

    public static final QuietDecoderException BAD_LENGTH_CACHED = new QuietDecoderException("Bad packet length");
    public static final QuietDecoderException VARINT_BIG_CACHED = new QuietDecoderException("VarInt too big");
}
