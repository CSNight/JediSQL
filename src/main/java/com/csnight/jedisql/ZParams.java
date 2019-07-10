package com.csnight.jedisql;

import com.csnight.jedisql.util.SafeEncoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ZParams {
    public enum Aggregate {
        SUM, MIN, MAX;

        public final byte[] raw;

        Aggregate() {
            raw = SafeEncoder.encode(name());
        }
    }

    private List<byte[]> params = new ArrayList<byte[]>();

    /**
     * Set weights.
     *
     * @param weights weights.
     * @return
     */
    public ZParams weights(final double... weights) {
        params.add(Protocol.Keyword.WEIGHTS.raw);
        for (final double weight : weights) {
            params.add(Protocol.toByteArray(weight));
        }

        return this;
    }

    public Collection<byte[]> getParams() {
        return Collections.unmodifiableCollection(params);
    }

    public ZParams aggregate(final Aggregate aggregate) {
        params.add(Protocol.Keyword.AGGREGATE.raw);
        params.add(aggregate.raw);
        return this;
    }
}
