package com.csnight.jedisql;

import java.util.ArrayList;
import java.util.List;

public class AccessControlUser {

    private final List<String> flags = new ArrayList<String>();
    private final List<String> keys = new ArrayList<String>();
    private final List<String> passwords = new ArrayList<String>();
    private String commands;

    public AccessControlUser() {
    }

    public void addFlag(String flag) {
        flags.add(flag);
    }

    public List<String> getFlags() {
        return flags;
    }

    public void addKey(String key) {
        keys.add(key);
    }

    public List<String> getKeys() {
        return keys;
    }

    public void addPassword(String password) {
        passwords.add(password);
    }

    public List<String> getPassword() {
        return passwords;
    }

    public String getCommands() {
        return commands;
    }

    public void setCommands(String commands) {
        this.commands = commands;
    }

    @Override
    public String toString() {
        return "AccessControlUser{" + "flags=" + flags + ", keys=" + keys + ", passwords=" + passwords
                + ", commands='" + commands + '\'' + '}';
    }
}