package com.csnight.jedisql;

import com.csnight.jedisql.exceptions.JedisConnectionException;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.io.Closeable;
import java.util.Map;
import java.util.Set;

public abstract class JedisClusterConnectionHandler implements Closeable {
    protected final JedisClusterInfoCache cache;

    public JedisClusterConnectionHandler(Set<HostAndPort> nodes,
                                         final GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout, String password) {
        this(nodes, poolConfig, connectionTimeout, soTimeout, password, null);
    }

    public JedisClusterConnectionHandler(Set<HostAndPort> nodes,
                                         final GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout, String password, String clientName) {
        this(nodes, poolConfig, connectionTimeout, soTimeout, password, clientName, false, null, null, null, null);
    }

    public JedisClusterConnectionHandler(Set<HostAndPort> nodes, final GenericObjectPoolConfig poolConfig,
                                         int connectionTimeout, int soTimeout, String user, String password, String clientName) {
        this(nodes, poolConfig, connectionTimeout, soTimeout, user, password, clientName, false, null, null, null, null);
    }

    public JedisClusterConnectionHandler(Set<HostAndPort> nodes,
                                         final GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout, String password, String clientName,
                                         boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
                                         HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap portMap) {
        this.cache = new JedisClusterInfoCache(poolConfig, connectionTimeout, soTimeout, password, clientName,
                ssl, sslSocketFactory, sslParameters, hostnameVerifier, portMap);
        initializeSlotsCache(nodes, connectionTimeout, soTimeout, password, clientName, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
    }

    public JedisClusterConnectionHandler(Set<HostAndPort> nodes,
                                         final GenericObjectPoolConfig poolConfig, int connectionTimeout, int soTimeout,
                                         String user, String password, String clientName, boolean ssl, SSLSocketFactory sslSocketFactory,
                                         SSLParameters sslParameters, HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap portMap) {
        this.cache = new JedisClusterInfoCache(poolConfig, connectionTimeout, soTimeout, user, password, clientName,
                ssl, sslSocketFactory, sslParameters, hostnameVerifier, portMap);
        initializeSlotsCache(nodes, connectionTimeout, soTimeout, user, password, clientName, ssl, sslSocketFactory, sslParameters, hostnameVerifier);

    }

    abstract JediSQL getConnection();

    abstract JediSQL getConnectionFromSlot(int slot);

    public JediSQL getConnectionFromNode(HostAndPort node) {
        return cache.setupNodeIfNotExist(node).getResource();
    }

    public Map<String, JedisPool> getNodes() {
        return cache.getNodes();
    }

    private void initializeSlotsCache(Set<HostAndPort> startNodes,
                                      int connectionTimeout, int soTimeout, String password, String clientName,
                                      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters, HostnameVerifier hostnameVerifier) {

        initializeSlotsCache(startNodes, connectionTimeout, soTimeout, null, password, clientName,
                ssl, sslSocketFactory, sslParameters, hostnameVerifier);
    }

    private void initializeSlotsCache(Set<HostAndPort> startNodes,
                                      int connectionTimeout, int soTimeout, String user, String password, String clientName,
                                      boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters, HostnameVerifier hostnameVerifier) {
        for (HostAndPort hostAndPort : startNodes) {
            JediSQL jedis = null;
            try {
                jedis = new JediSQL(hostAndPort.getHost(), hostAndPort.getPort(), connectionTimeout, soTimeout, ssl, sslSocketFactory, sslParameters, hostnameVerifier);
                if (user != null) {
                    jedis.auth(user, password);
                } else if (password != null) {
                    jedis.auth(password);
                }
                if (clientName != null) {
                    jedis.clientSetname(clientName);
                }
                cache.discoverClusterNodesAndSlots(jedis);
                break;
            } catch (JedisConnectionException e) {
                // try next nodes
            } finally {
                if (jedis != null) {
                    jedis.close();
                }
            }
        }
    }

    public void renewSlotCache() {
        cache.renewClusterSlots(null);
    }

    public void renewSlotCache(JediSQL jediSQL) {
        cache.renewClusterSlots(jediSQL);
    }

    @Override
    public void close() {
        cache.reset();
    }
}
