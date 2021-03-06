package com.csnight.jedisql;

import com.csnight.jedisql.commands.ProtocolCommand;
import com.csnight.jedisql.exceptions.*;
import com.csnight.jedisql.util.RedisInputStream;
import com.csnight.jedisql.util.RedisOutputStream;
import com.csnight.jedisql.util.SafeEncoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Protocol {

    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 6379;
    public static final int DEFAULT_SENTINEL_PORT = 26379;
    public static final int DEFAULT_TIMEOUT = 2000;
    public static final int DEFAULT_DATABASE = 0;
    public static final String CHARSET = "UTF-8";
    public static final byte DOLLAR_BYTE = '$';
    public static final byte ASTERISK_BYTE = '*';
    public static final byte PLUS_BYTE = '+';
    public static final byte MINUS_BYTE = '-';
    public static final byte COLON_BYTE = ':';
    public static final String SENTINEL_MASTERS = "masters";
    public static final String SENTINEL_GET_MASTER_ADDR_BY_NAME = "get-master-addr-by-name";
    public static final String SENTINEL_RESET = "reset";
    public static final String SENTINEL_SLAVES = "slaves";
    public static final String SENTINEL_FAILOVER = "failover";
    public static final String SENTINEL_MONITOR = "monitor";
    public static final String SENTINEL_REMOVE = "remove";
    public static final String SENTINEL_SET = "set";
    public static final String CLUSTER_NODES = "nodes";
    public static final String CLUSTER_MEET = "meet";
    public static final String CLUSTER_RESET = "reset";
    public static final String CLUSTER_ADDSLOTS = "addslots";
    public static final String CLUSTER_DELSLOTS = "delslots";
    public static final String CLUSTER_INFO = "info";
    public static final String CLUSTER_GETKEYSINSLOT = "getkeysinslot";
    public static final String CLUSTER_SETSLOT = "setslot";
    public static final String CLUSTER_SETSLOT_NODE = "node";
    public static final String CLUSTER_SETSLOT_MIGRATING = "migrating";
    public static final String CLUSTER_SETSLOT_IMPORTING = "importing";
    public static final String CLUSTER_SETSLOT_STABLE = "stable";
    public static final String CLUSTER_FORGET = "forget";
    public static final String CLUSTER_FLUSHSLOT = "flushslots";
    public static final String CLUSTER_KEYSLOT = "keyslot";
    public static final String CLUSTER_COUNTKEYINSLOT = "countkeysinslot";
    public static final String CLUSTER_SAVECONFIG = "saveconfig";
    public static final String CLUSTER_REPLICATE = "replicate";
    public static final String CLUSTER_SLAVES = "slaves";
    public static final String CLUSTER_FAILOVER = "failover";
    public static final String CLUSTER_SLOTS = "slots";
    public static final String PUBSUB_CHANNELS = "channels";
    public static final String PUBSUB_NUMSUB = "numsub";
    public static final String PUBSUB_NUM_PAT = "numpat";
    public static final byte[] BYTES_TRUE = toByteArray(1);
    public static final byte[] BYTES_FALSE = toByteArray(0);
    public static final byte[] BYTES_TILDE = SafeEncoder.encode("~");
    public static final byte[] POSITIVE_INFINITY_BYTES = "+inf".getBytes();
    public static final byte[] NEGATIVE_INFINITY_BYTES = "-inf".getBytes();
    private static final String ASK_PREFIX = "ASK ";
    private static final String MOVED_PREFIX = "MOVED ";
    private static final String CLUSTERDOWN_PREFIX = "CLUSTERDOWN ";
    private static final String BUSY_PREFIX = "BUSY ";
    private static final String NOSCRIPT_PREFIX = "NOSCRIPT ";
    private static final String WRONGPASS_PREFIX = "WRONGPASS";
    private static final String NOPERM_PREFIX = "NOPERM";

    private Protocol() {
        // this prevent the class from instantiation
    }

    public static void sendCommand(final RedisOutputStream os, final ProtocolCommand command,
                                   final byte[]... args) {
        sendCommand(os, command.getRaw(), args);
    }

    private static void sendCommand(final RedisOutputStream os, final byte[] command,
                                    final byte[]... args) {
        try {
            os.write(ASTERISK_BYTE);
            os.writeIntCrLf(args.length + 1);
            os.write(DOLLAR_BYTE);
            os.writeIntCrLf(command.length);
            os.write(command);
            os.writeCrLf();

            for (final byte[] arg : args) {
                os.write(DOLLAR_BYTE);
                os.writeIntCrLf(arg.length);
                os.write(arg);
                os.writeCrLf();
            }
        } catch (IOException e) {
            throw new JedisConnectionException(e);
        }
    }

    private static void processError(final RedisInputStream is) {
        String message = is.readLine();
        // TODO: I'm not sure if this is the best way to do this.
        // Maybe Read only first 5 bytes instead?
        if (message.startsWith(MOVED_PREFIX)) {
            String[] movedInfo = parseTargetHostAndSlot(message);
            throw new JedisMovedDataException(message, new HostAndPort(movedInfo[1],
                    Integer.parseInt(movedInfo[2])), Integer.parseInt(movedInfo[0]));
        } else if (message.startsWith(ASK_PREFIX)) {
            String[] askInfo = parseTargetHostAndSlot(message);
            throw new JedisAskDataException(message, new HostAndPort(askInfo[1],
                    Integer.parseInt(askInfo[2])), Integer.parseInt(askInfo[0]));
        } else if (message.startsWith(CLUSTERDOWN_PREFIX)) {
            throw new JedisClusterException(message);
        } else if (message.startsWith(BUSY_PREFIX)) {
            throw new JedisBusyException(message);
        } else if (message.startsWith(NOSCRIPT_PREFIX)) {
            throw new JedisNoScriptException(message);
        } else if (message.startsWith(WRONGPASS_PREFIX)) {
            throw new JedisAccessControlException(message);
        } else if (message.startsWith(NOPERM_PREFIX)) {
            throw new JedisAccessControlException(message);
        }
        throw new JedisDataException(message);
    }

    public static String readErrorLineIfPossible(RedisInputStream is) {
        final byte b = is.readByte();
        // if buffer contains other type of response, just ignore.
        if (b != MINUS_BYTE) {
            return null;
        }
        return is.readLine();
    }

    private static String[] parseTargetHostAndSlot(String clusterRedirectResponse) {
        String[] response = new String[3];
        String[] messageInfo = clusterRedirectResponse.split(" ");
        String[] targetHostAndPort = HostAndPort.extractParts(messageInfo[2]);
        response[0] = messageInfo[1];
        response[1] = targetHostAndPort[0];
        response[2] = targetHostAndPort[1];
        return response;
    }

    private static Object process(final RedisInputStream is) {
        final byte b = is.readByte();
        switch (b) {
            case PLUS_BYTE:
                return processStatusCodeReply(is);
            case DOLLAR_BYTE:
                return processBulkReply(is);
            case ASTERISK_BYTE:
                return processMultiBulkReply(is);
            case COLON_BYTE:
                return processInteger(is);
            case MINUS_BYTE:
                processError(is);
                return null;
            default:
                throw new JedisConnectionException("Unknown reply: " + (char) b);
        }
    }

    private static byte[] processStatusCodeReply(final RedisInputStream is) {
        return is.readLineBytes();
    }

    private static byte[] processBulkReply(final RedisInputStream is) {
        final int len = is.readIntCrLf();
        if (len == -1) {
            return null;
        }

        final byte[] read = new byte[len];
        int offset = 0;
        while (offset < len) {
            final int size = is.read(read, offset, (len - offset));
            if (size == -1) throw new JedisConnectionException(
                    "It seems like server has closed the connection.");
            offset += size;
        }

        // read 2 more bytes for the command delimiter
        is.readByte();
        is.readByte();

        return read;
    }

    private static Long processInteger(final RedisInputStream is) {
        return is.readLongCrLf();
    }

    private static List<Object> processMultiBulkReply(final RedisInputStream is) {
        final int num = is.readIntCrLf();
        if (num == -1) {
            return null;
        }
        final List<Object> ret = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            try {
                ret.add(process(is));
            } catch (JedisDataException e) {
                ret.add(e);
            }
        }
        return ret;
    }

    public static Object read(final RedisInputStream is) {
        return process(is);
    }

    public static final byte[] toByteArray(final boolean value) {
        return value ? BYTES_TRUE : BYTES_FALSE;
    }

    public static final byte[] toByteArray(final int value) {
        return SafeEncoder.encode(String.valueOf(value));
    }

    public static final byte[] toByteArray(final long value) {
        return SafeEncoder.encode(String.valueOf(value));
    }

    public static final byte[] toByteArray(final double value) {
        if (value == Double.POSITIVE_INFINITY) {
            return POSITIVE_INFINITY_BYTES;
        } else if (value == Double.NEGATIVE_INFINITY) {
            return NEGATIVE_INFINITY_BYTES;
        } else {
            return SafeEncoder.encode(String.valueOf(value));
        }
    }

    public static enum Command implements ProtocolCommand {
        ACL, APPEND, ASKING, AUTH, BGREWRITEAOF, BGSAVE, BITCOUNT, BITFIELD, BITOP, BITPOS, BLPOP,
        BRPOP, BRPOPLPUSH, BZPOPMAX, BZPOPMIN, CLIENT, CLUSTER, COMMAND,
        CONFIG, DBSIZE, DEBUG, DECR, DECRBY, DEL, DISCARD, DUMP, ECHO, EVAL, EVALSHA, EXEC, EXISTS,
        EXPIRE, EXPIREAT, FLUSHALL, FLUSHDB, GEOADD, GEODIST, GEOHASH, GEOPOS, GEORADIUS, GEORADIUSBYMEMBER,
        GEORADIUSBYMEMBER_RO, GEORADIUS_RO, GET, GETBIT, GETRANGE, GETSET, HDEL, HEXISTS, HGET, HGETALL, HINCRBY,
        HINCRBYFLOAT, HKEYS, HLEN, HMGET, HMSET, HOST$, HSCAN, HSET, HSETNX, HSTRLEN, HVALS, INCR, INCRBY, INCRBYFLOAT,
        INFO, KEYS, LASTSAVE, LATENCY, LINDEX, LINSERT, LLEN, LOLWUT, LPOP, LPUSH, LPUSHX, LRANGE, LREM, LSET, LTRIM,
        MEMORY, MGET, MIGRATE, MODULE, MONITOR, MOVE, MSET, MSETNX, MULTI, OBJECT, PERSIST, PEXPIRE,
        PEXPIREAT, PFADD, PFCOUNT, PFDEBUG, PFMERGE, PFSELFTEST, PING, POST, PSETEX, PSUBSCRIBE, PSYNC, PTTL,
        PUBLISH, PUBSUB, PUNSUBSCRIBE, QUIT, RANDOMKEY, READONLY, READWRITE, RENAME, RENAMENX, RENAMEX,
        REPLCONF, REPLICAOF, RESTORE, RESTORE_ASKING, ROLE, RPOP, RPOPLPUSH, RPUSH, RPUSHX, SADD, SAVE, SCAN,
        SCARD, SCRIPT, SDIFF, SDIFFSTORE, SELECT, SENTINEL, SET, SETBIT, SETEX, SETNX, SETRANGE, SHUTDOWN, SINTER,
        SINTERSTORE, SISMEMBER, SLAVEOF, SLOWLOG, SMEMBERS, SMOVE, SORT, SPOP, SRANDMEMBER, SREM, SSCAN, STRLEN,
        SUBSCRIBE, SUBSTR, SUNION, SUNIONSTORE, SWAPDB, SYNC, TIME, TOUCH, TTL, TYPE,
        UNLINK, UNSUBSCRIBE, UNWATCH, WAIT, WATCH,
        XACK, XADD, XCLAIM, XDEL, XGROUP, XINFO, XLEN, XPENDING, XRANGE, XREAD, XREADGROUP, XREVRANGE,
        XSETID, XTRIM, ZADD, ZCARD, ZCOUNT,
        ZINCRBY, ZINTERSTORE, ZLEXCOUNT, ZPOPMAX, ZPOPMIN, ZRANGE, ZRANGEBYLEX, ZRANGEBYSCORE, ZRANK, ZREM,
        ZREMRANGEBYLEX, ZREMRANGEBYRANK, ZREMRANGEBYSCORE, ZREVRANGE, ZREVRANGEBYLEX, ZREVRANGEBYSCORE, ZREVRANK, ZSCAN,
        ZSCORE, ZUNIONSTORE;

        private final byte[] raw;

        Command() {
            if (this.name().contains("_")) {
                raw = SafeEncoder.encode(this.name().replace("_", "-"));
            } else if (this.name().contains("$")) {
                raw = SafeEncoder.encode(this.name().replace("$", ":"));
            } else {
                raw = SafeEncoder.encode(this.name());
            }
        }

        @Override
        public byte[] getRaw() {
            return raw;
        }
    }

    public static enum Keyword {
        AGGREGATE, ALPHA, ASC, BY, DESC, GET, LIMIT, MESSAGE, NO, NOSORT, PMESSAGE, PSUBSCRIBE,
        PUNSUBSCRIBE, OK, ONE, QUEUED, SET, STORE, SUBSCRIBE, UNSUBSCRIBE, WEIGHTS, WITHSCORES,
        RESETSTAT, REWRITE, RESET, FLUSH, EXISTS, LOAD, KILL, LEN, REFCOUNT, ENCODING, IDLETIME,
        GETNAME, SETNAME, LIST, MATCH, COUNT, PING, PONG, UNLOAD, REPLACE, KEYS, PAUSE, DOCTOR,
        BLOCK, NOACK, STREAMS, KEY, CREATE, MKSTREAM, SETID, DESTROY, DELCONSUMER, MAXLEN, GROUP,
        IDLE, TIME, RETRYCOUNT, FORCE, STREAM, GROUPS, CONSUMERS,
        SETUSER, GETUSER, DELUSER, WHOAMI, CAT, GENPASS, USERS;

        public final byte[] raw;

        Keyword() {
            raw = SafeEncoder.encode(this.name().toLowerCase(Locale.ENGLISH));
        }
    }

    public static enum RediSQLCommand implements ProtocolCommand {
        CREATE_DB("REDISQL.CREATE_DB"),
        EXEC("REDISQL.EXEC"),
        EXEC_NOW("REDISQL.EXEC.NOW"),
        QUERY("REDISQL.QUERY"),
        QUERY_NOW("REDISQL.QUERY.NOW"),
        QUERY_INTO("REDISQL.QUERY.INTO"),
        QUERY_INTO_NOW("REDISQL.QUERY.INTO.NOW"),
        CREATE_STATEMENT("REDISQL.CREATE_STATEMENT"),
        CREATE_STATEMENT_NOW("REDISQL.CREATE_STATEMENT.NOW"),
        EXEC_STATEMENT("REDISQL.EXEC_STATEMENT"),
        EXEC_STATEMENT_NOW("REDISQL.EXEC_STATEMENT.NOW"),
        QUERY_STATEMENT("REDISQL.QUERY_STATEMENT"),
        QUERY_STATEMENT_NOW("REDISQL.QUERY_STATEMENT.NOW"),
        QUERY_STATEMENT_INTO("REDISQL.QUERY_STATEMENT.INTO"),
        QUERY_STATEMENT_INTO_NOW("REDISQL.QUERY_STATEMENT.INTO.NOW"),
        DELETE_STATEMENT("REDISQL.DELETE_STATEMENT"),
        DELETE_STATEMENT_NOW("REDISQL.DELETE_STATEMENT.NOW"),
        UPDATE_STATEMENT("REDISQL.UPDATE_STATEMENT"),
        UPDATE_STATEMENT_NOW("REDISQL.DELETE_STATEMENT.NOW"),
        COPY("REDISQL.COPY"),
        COPY_NOW("REDISQ.COPY.NOW"),
        STATISTICS("REDISQL.STATISTICS"),
        VERSION("REDISQL.VERSION");

        public final byte[] raw;

        RediSQLCommand(String command) {
            raw = SafeEncoder.encode(command);
        }

        @Override
        public byte[] getRaw() {
            return raw;
        }
    }
}
