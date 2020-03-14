package com.csnight.jedisql.commands;

import com.csnight.jedisql.AccessControlUser;
import com.csnight.jedisql.params.ClientKillParams;
import com.csnight.jedisql.params.MigrateParams;
import com.csnight.jedisql.util.Slowlog;

import java.util.List;

public interface AdvancedJedisCommands {
    List<String> configGet(String pattern);

    String configSet(String parameter, String value);

    String slowlogReset();

    Long slowlogLen();

    List<Slowlog> slowlogGet();

    List<Slowlog> slowlogGet(long entries);

    Long objectRefcount(String key);

    String objectEncoding(String key);

    Long objectIdletime(String key);

    String migrate(String host, int port, String key, int destinationDB, int timeout);

    String migrate(String host, int port, int destinationDB, int timeout, MigrateParams params, String... keys);

    String clientKill(String ipPort);

    String clientKill(String ip, int port);

    Long clientKill(ClientKillParams params);

    String clientGetname();

    String clientList();

    String clientSetname(String name);

    String memoryDoctor();

    String aclWhoAmI();

    String aclGenPass();

    List<String> aclList();

    List<String> aclUsers();

    AccessControlUser aclGetUser(String name);

    String aclSetUser(String name);

    String aclSetUser(String name, String... keys);

    Long aclDelUser(String name);

    List<String> aclCat();

    List<String> aclCat(String category);

    // TODO: Implements ACL LOAD/SAVE commands
}
