package com.csnight.jedisql;

import com.csnight.jedisql.Protocol.Keyword;
import com.csnight.jedisql.util.SafeEncoder;

import java.nio.ByteBuffer;
import java.util.*;

import static com.csnight.jedisql.Protocol.Keyword.COUNT;
import static com.csnight.jedisql.Protocol.Keyword.MATCH;

public class ScanParams {

    private final Map<Keyword, ByteBuffer> params = new EnumMap<Keyword, ByteBuffer>(Keyword.class);

    public final static String SCAN_POINTER_START = String.valueOf(0);
    public final static byte[] SCAN_POINTER_START_BINARY = SafeEncoder.encode(SCAN_POINTER_START);

    public ScanParams match(final byte[] pattern) {
        params.put(MATCH, ByteBuffer.wrap(pattern));
        return this;
    }

    /**
     * @param pattern
     * @return
     * @see <a href="https://redis.io/commands/scan#the-match-option">MATCH option in Redis documentation</a>
     */
    public ScanParams match(final String pattern) {
        params.put(MATCH, ByteBuffer.wrap(SafeEncoder.encode(pattern)));
        return this;
    }

    /**
     * @param count
     * @return
     * @see <a href="https://redis.io/commands/scan#the-count-option">COUNT option in Redis documentation</a>
     */
    public ScanParams count(final Integer count) {
        params.put(COUNT, ByteBuffer.wrap(Protocol.toByteArray(count)));
        return this;
    }

    public Collection<byte[]> getParams() {
        List<byte[]> paramsList = new ArrayList<byte[]>(params.size());
        for (Map.Entry<Keyword, ByteBuffer> param : params.entrySet()) {
            paramsList.add(param.getKey().raw);
            paramsList.add(param.getValue().array());
        }
        return Collections.unmodifiableCollection(paramsList);
    }

    byte[] binaryMatch() {
        if (params.containsKey(MATCH)) {
            return params.get(MATCH).array();
        } else {
            return null;
        }
    }

    String match() {
        if (params.containsKey(MATCH)) {
            return new String(params.get(MATCH).array());
        } else {
            return null;
        }
    }

    Integer count() {
        if (params.containsKey(COUNT)) {
            return params.get(COUNT).getInt();
        } else {
            return null;
        }
    }
}
