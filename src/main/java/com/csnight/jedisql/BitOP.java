package com.csnight.jedisql;

import com.csnight.jedisql.util.SafeEncoder;

public enum BitOP {
    AND, OR, XOR, NOT;

    public final byte[] raw;

    private BitOP() {
        this.raw = SafeEncoder.encode(name());
    }
}
