package com.csnight.jedisql;

import com.csnight.jedisql.util.Pool;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class JedisPoolAbstract extends Pool<JediSQL> {

    public JedisPoolAbstract() {
        super();
    }

    public JedisPoolAbstract(GenericObjectPoolConfig poolConfig, PooledObjectFactory<JediSQL> factory) {
        super(poolConfig, factory);
    }

    @Override
    protected void returnBrokenResource(JediSQL resource) {
        super.returnBrokenResource(resource);
    }

    @Override
    protected void returnResource(JediSQL resource) {
        super.returnResource(resource);
    }
}
