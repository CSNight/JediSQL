package com.csnight.jedisql.commands;

import com.csnight.jedisql.AccessControlUser;
import com.csnight.jedisql.params.ClientKillParams;
import com.csnight.jedisql.params.MigrateParams;

import java.util.List;

public interface AdvancedBinaryJedisCommands {

    List<byte[]> configGet(byte[] pattern);

    byte[] configSet(byte[] parameter, byte[] value);

    String slowlogReset();

    Long slowlogLen();

    List<byte[]> slowlogGetBinary();

    List<byte[]> slowlogGetBinary(long entries);

    Long objectRefcount(byte[] key);

    byte[] objectEncoding(byte[] key);

    Long objectIdletime(byte[] key);

    String migrate(String host, int port, byte[] key, int destinationDB, int timeout);

    String migrate(String host, int port, int destinationDB, int timeout, MigrateParams params, byte[]... keys);

    String clientKill(byte[] ipPort);

    String clientKill(String ip, int port);

    Long clientKill(ClientKillParams params);

    byte[] clientGetnameBinary();

    byte[] clientListBinary();

    String clientSetname(byte[] name);

    byte[] memoryDoctorBinary();

    byte[] aclWhoAmIBinary();

    byte[] aclGenPassBinary();

    List<byte[]> aclListBinary();

    List<byte[]> aclUsersBinary();

    AccessControlUser aclGetUser(byte[] name);

    String aclSetUser(byte[] name);

    String aclSetUser(byte[] name, byte[]... keys);

    Long aclDelUser(byte[] name);

    List<byte[]> aclCatBinary();

    List<byte[]> aclCat(byte[] category);

    // TODO: Implements ACL LOAD/SAVE commands
}
