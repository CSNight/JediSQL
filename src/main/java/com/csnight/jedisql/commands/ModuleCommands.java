package com.csnight.jedisql.commands;

import com.csnight.jedisql.Module;

import java.util.List;

public interface ModuleCommands {
    String moduleLoad(String path);

    String moduleUnload(String name);

    List<Module> moduleList();
}
