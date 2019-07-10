package com.csnight.jedisql;

import com.csnight.jedisql.util.SafeEncoder;

public enum GeoUnit {
    M, KM, MI, FT;

    public final byte[] raw;

    GeoUnit() {
        raw = SafeEncoder.encode(this.name().toLowerCase());
    }
}
