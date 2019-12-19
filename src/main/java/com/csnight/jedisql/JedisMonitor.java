package com.csnight.jedisql;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class JedisMonitor {
    protected Client client;
    private AtomicBoolean broken = new AtomicBoolean(false);

    public void setBroken(boolean broken) {
        this.broken.set(broken);
    }

    public void proceed(Client client) {
        this.client = client;
        this.client.setTimeoutInfinite();
        while (client.isConnected() && !broken.get()) {
            String command = client.getBulkReply();
            onCommand(command);
        }
        onCommand("Cancel Monitor Process");
    }

    public abstract void onCommand(String command);
}