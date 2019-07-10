package com.csnight.jedisql;

public interface JedisClusterHostAndPortMap {
    HostAndPort getSSLHostAndPort(String host, int port);
}
