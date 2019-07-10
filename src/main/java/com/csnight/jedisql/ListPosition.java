package com.csnight.jedisql;

import com.csnight.jedisql.util.SafeEncoder;

public enum ListPosition {
    BEFORE, AFTER;
    public final byte[] raw;

    private ListPosition() {
        raw = SafeEncoder.encode(name());
    }
}
