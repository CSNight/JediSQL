package com.csnight.jedisql;

import com.csnight.jedisql.exceptions.JedisConnectionException;
import com.csnight.jedisql.exceptions.JedisException;
import com.csnight.jedisql.util.SafeEncoder;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class JedisClusterInfoCache {
    private final Map<String, JedisPool> nodes = new HashMap<String, JedisPool>();
    private final Map<Integer, JedisPool> slots = new HashMap<Integer, JedisPool>();

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock r = rwl.readLock();
    private final Lock w = rwl.writeLock();
    private volatile boolean rediscovering;
    private final GenericObjectPoolConfig poolConfig;

    private int connectionTimeout;
    private int soTimeout;
    private String password;
    private String clientName;

    private boolean ssl;
    private SSLSocketFactory sslSocketFactory;
    private SSLParameters sslParameters;
    private HostnameVerifier hostnameVerifier;
    private JedisClusterHostAndPortMap hostAndPortMap;

    private static final int MASTER_NODE_INDEX = 2;

    public JedisClusterInfoCache(final GenericObjectPoolConfig poolConfig, int timeout) {
        this(poolConfig, timeout, timeout, null, null);
    }

    public JedisClusterInfoCache(final GenericObjectPoolConfig poolConfig,
                                 final int connectionTimeout, final int soTimeout, final String password, final String clientName) {
        this(poolConfig, connectionTimeout, soTimeout, password, clientName, false, null, null, null, null);
    }

    public JedisClusterInfoCache(final GenericObjectPoolConfig poolConfig,
                                 final int connectionTimeout, final int soTimeout, final String password, final String clientName,
                                 boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters,
                                 HostnameVerifier hostnameVerifier, JedisClusterHostAndPortMap hostAndPortMap) {
        this.poolConfig = poolConfig;
        this.connectionTimeout = connectionTimeout;
        this.soTimeout = soTimeout;
        this.password = password;
        this.clientName = clientName;
        this.ssl = ssl;
        this.sslSocketFactory = sslSocketFactory;
        this.sslParameters = sslParameters;
        this.hostnameVerifier = hostnameVerifier;
        this.hostAndPortMap = hostAndPortMap;
    }

    public void discoverClusterNodesAndSlots(JediSQL jediSQL) {
        w.lock();

        try {
            reset();
            List<Object> slots = jediSQL.clusterSlots();

            for (Object slotInfoObj : slots) {
                List<Object> slotInfo = (List<Object>) slotInfoObj;

                if (slotInfo.size() <= MASTER_NODE_INDEX) {
                    continue;
                }

                List<Integer> slotNums = getAssignedSlotArray(slotInfo);

                // hostInfos
                int size = slotInfo.size();
                for (int i = MASTER_NODE_INDEX; i < size; i++) {
                    List<Object> hostInfos = (List<Object>) slotInfo.get(i);
                    if (hostInfos.size() <= 0) {
                        continue;
                    }

                    HostAndPort targetNode = generateHostAndPort(hostInfos);
                    setupNodeIfNotExist(targetNode);
                    if (i == MASTER_NODE_INDEX) {
                        assignSlotsToNode(slotNums, targetNode);
                    }
                }
            }
        } finally {
            w.unlock();
        }
    }

    public void renewClusterSlots(JediSQL jediSQL) {
        //If rediscovering is already in process - no need to start one more same rediscovering, just return
        if (!rediscovering) {
            try {
                w.lock();
                if (!rediscovering) {
                    rediscovering = true;

                    try {
                        if (jediSQL != null) {
                            try {
                                discoverClusterSlots(jediSQL);
                                return;
                            } catch (JedisException e) {
                                //try nodes from all pools
                            }
                        }

                        for (JedisPool jp : getShuffledNodesPool()) {
                            JediSQL j = null;
                            try {
                                j = jp.getResource();
                                discoverClusterSlots(j);
                                return;
                            } catch (JedisConnectionException e) {
                                // try next nodes
                            } finally {
                                if (j != null) {
                                    j.close();
                                }
                            }
                        }
                    } finally {
                        rediscovering = false;
                    }
                }
            } finally {
                w.unlock();
            }
        }
    }

    private void discoverClusterSlots(JediSQL jediSQL) {
        List<Object> slots = jediSQL.clusterSlots();
        this.slots.clear();

        for (Object slotInfoObj : slots) {
            List<Object> slotInfo = (List<Object>) slotInfoObj;

            if (slotInfo.size() <= MASTER_NODE_INDEX) {
                continue;
            }

            List<Integer> slotNums = getAssignedSlotArray(slotInfo);

            // hostInfos
            List<Object> hostInfos = (List<Object>) slotInfo.get(MASTER_NODE_INDEX);
            if (hostInfos.isEmpty()) {
                continue;
            }

            // at this time, we just use master, discard slave information
            HostAndPort targetNode = generateHostAndPort(hostInfos);
            assignSlotsToNode(slotNums, targetNode);
        }
    }

    private HostAndPort generateHostAndPort(List<Object> hostInfos) {
        String host = SafeEncoder.encode((byte[]) hostInfos.get(0));
        int port = ((Long) hostInfos.get(1)).intValue();
        if (ssl && hostAndPortMap != null) {
            HostAndPort hostAndPort = hostAndPortMap.getSSLHostAndPort(host, port);
            if (hostAndPort != null) {
                return hostAndPort;
            }
        }
        return new HostAndPort(host, port);
    }

    public JedisPool setupNodeIfNotExist(HostAndPort node) {
        w.lock();
        try {
            String nodeKey = getNodeKey(node);
            JedisPool existingPool = nodes.get(nodeKey);
            if (existingPool != null) return existingPool;

            JedisPool nodePool = new JedisPool(poolConfig, node.getHost(), node.getPort(),
                    connectionTimeout, soTimeout, password, 0, clientName,
                    ssl, sslSocketFactory, sslParameters, hostnameVerifier);
            nodes.put(nodeKey, nodePool);
            return nodePool;
        } finally {
            w.unlock();
        }
    }

    public void assignSlotToNode(int slot, HostAndPort targetNode) {
        w.lock();
        try {
            JedisPool targetPool = setupNodeIfNotExist(targetNode);
            slots.put(slot, targetPool);
        } finally {
            w.unlock();
        }
    }

    public void assignSlotsToNode(List<Integer> targetSlots, HostAndPort targetNode) {
        w.lock();
        try {
            JedisPool targetPool = setupNodeIfNotExist(targetNode);
            for (Integer slot : targetSlots) {
                slots.put(slot, targetPool);
            }
        } finally {
            w.unlock();
        }
    }

    public JedisPool getNode(String nodeKey) {
        r.lock();
        try {
            return nodes.get(nodeKey);
        } finally {
            r.unlock();
        }
    }

    public JedisPool getSlotPool(int slot) {
        r.lock();
        try {
            return slots.get(slot);
        } finally {
            r.unlock();
        }
    }

    public Map<String, JedisPool> getNodes() {
        r.lock();
        try {
            return new HashMap<String, JedisPool>(nodes);
        } finally {
            r.unlock();
        }
    }

    public List<JedisPool> getShuffledNodesPool() {
        r.lock();
        try {
            List<JedisPool> pools = new ArrayList<JedisPool>(nodes.values());
            Collections.shuffle(pools);
            return pools;
        } finally {
            r.unlock();
        }
    }

    /**
     * Clear discovered nodes collections and gently release allocated resources
     */
    public void reset() {
        w.lock();
        try {
            for (JedisPool pool : nodes.values()) {
                try {
                    if (pool != null) {
                        pool.destroy();
                    }
                } catch (Exception e) {
                    // pass
                }
            }
            nodes.clear();
            slots.clear();
        } finally {
            w.unlock();
        }
    }

    public static String getNodeKey(HostAndPort hnp) {
        return hnp.getHost() + ":" + hnp.getPort();
    }

    public static String getNodeKey(Client client) {
        return client.getHost() + ":" + client.getPort();
    }

    public static String getNodeKey(JediSQL jediSQL) {
        return getNodeKey(jediSQL.getClient());
    }

    private List<Integer> getAssignedSlotArray(List<Object> slotInfo) {
        List<Integer> slotNums = new ArrayList<Integer>();
        for (int slot = ((Long) slotInfo.get(0)).intValue(); slot <= ((Long) slotInfo.get(1))
                .intValue(); slot++) {
            slotNums.add(slot);
        }
        return slotNums;
    }
}
