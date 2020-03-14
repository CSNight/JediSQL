package com.csnight.jedisql;

import com.csnight.jedisql.exceptions.JedisException;
import com.csnight.jedisql.exceptions.JedisNoReachableClusterNodeException;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.util.List;
import java.util.Set;

public class JedisSlotBasedConnectionHandler extends JedisClusterConnectionHandler {

    public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes,
                                           final GenericObjectPoolConfig poolConfig, int timeout) {
        this(nodes, poolConfig, timeout, timeout);
    }

    public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes,
                                           final GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout) {
        super(nodes, poolConfig, connectionTimeout, soTimeout, null);
    }

    public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout, String password) {
        super(nodes, poolConfig, connectionTimeout, soTimeout, password);
    }

    public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout, String password, String clientName) {
        super(nodes, poolConfig, connectionTimeout, soTimeout, password, clientName);
    }

    public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout,
                                           String user, String password, String clientName) {
        super(nodes, poolConfig, connectionTimeout, soTimeout, user, password, clientName);
    }

    public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout, String password, String clientName,
                                           boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters, HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap portMap) {
        super(nodes, poolConfig, connectionTimeout, soTimeout, password, clientName, ssl, sslSocketFactory, sslParameters, hostnameVerifier, portMap);
    }

    public JedisSlotBasedConnectionHandler(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout,
                                           String user, String password, String clientName, boolean ssl, SSLSocketFactory sslSocketFactory,
                                           SSLParameters sslParameters, HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap portMap) {
        super(nodes, poolConfig, connectionTimeout, soTimeout, user, password, clientName, ssl, sslSocketFactory, sslParameters, hostnameVerifier, portMap);
    }

    @Override
    public JediSQL getConnection() {
        // In antirez's redis-rb-cluster implementation,
        // getRandomConnection always return valid connection (able to
        // ping-pong)
        // or exception if all connections are invalid

        List<JedisPool> pools = cache.getShuffledNodesPool();

        for (JedisPool pool : pools) {
            JediSQL jediSQL = null;
            try {
                jediSQL = pool.getResource();

                if (jediSQL == null) {
                    continue;
                }

                String result = jediSQL.ping();

                if (result.equalsIgnoreCase("pong")) return jediSQL;

                jediSQL.close();
            } catch (JedisException ex) {
                if (jediSQL != null) {
                    jediSQL.close();
                }
            }
        }

        throw new JedisNoReachableClusterNodeException("No reachable node in cluster");
    }

    @Override
    public JediSQL getConnectionFromSlot(int slot) {
        JedisPool connectionPool = cache.getSlotPool(slot);
        if (connectionPool != null) {
            // It can't guaranteed to get valid connection because of node
            // assignment
            return connectionPool.getResource();
        } else {
            renewSlotCache(); //It's abnormal situation for cluster mode, that we have just nothing for slot, try to rediscover state
            connectionPool = cache.getSlotPool(slot);
            if (connectionPool != null) {
                return connectionPool.getResource();
            } else {
                //no choice, fallback to new connection to random node
                return getConnection();
            }
        }
    }
}
