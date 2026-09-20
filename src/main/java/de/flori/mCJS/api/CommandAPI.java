package de.flori.mCJS.api;

import org.bukkit.command.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CommandAPI extends BaseAPI {
    private final APIHelper apiHelper;
    private final Set<Command> dynamicCommands = ConcurrentHashMap.newKeySet();

    public CommandAPI(JavaPlugin plugin, APIHelper apiHelper) {
        super(plugin);
        this.apiHelper = apiHelper;
    }

    public void registerCommand(String name, String description, String usage, Object executor) {
        debug("Registering command: " + name + " (with description and usage)");
        registerCommand(name, description, usage, executor, null);
    }

    public void registerCommand(String name, Object executor) {
        debug("Registering simple command: " + name);
        registerCommand(name, "", "/" + name, executor, null);
    }

    public void registerCommand(String name, String description, String usage, Object executor, Object tabCompleter) {
        registerCommandInternal(name, description, usage, executor);

        if (tabCompleter != null) {
            try {
                PluginCommand command = plugin.getCommand(name);
                if (command == null) {

                    try {
                        CommandMap commandMap = getCommandMap();
                        Command cmd = commandMap.getCommand(name);
                        if (cmd instanceof PluginCommand) {
                            command = (PluginCommand) cmd;
                        }
                    } catch (Exception e) {

                    }
                }

                if (command != null && tabCompleter instanceof Function) {
                    command.setTabCompleter((sender, cmd, alias, args) -> {
                        try {
                            org.mozilla.javascript.Context rhinoContext = org.mozilla.javascript.Context.enter();
                            try {
                                rhinoContext.setOptimizationLevel(-1);
                                rhinoContext.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                                Function func = (Function) tabCompleter;
                                Scriptable scope = apiHelper.getScope();
                                Object result = func.call(rhinoContext, scope, scope, new Object[]{sender, args});

                                if (result instanceof java.util.List) {
                                    @SuppressWarnings("unchecked")
                                    java.util.List<String> list = (java.util.List<String>) result;
                                    return list;
                                } else if (result instanceof Object[]) {
                                    return java.util.Arrays.asList((String[]) result);
                                } else if (result instanceof Scriptable) {
                                    Scriptable array = (Scriptable) result;
                                    java.util.List<String> completions = new java.util.ArrayList<>();
                                    Object length = array.get("length", array);
                                    if (length instanceof Number) {
                                        int len = ((Number) length).intValue();
                                        for (int i = 0; i < len; i++) {
                                            Object item = array.get(i, array);
                                            if (item != null) {
                                                completions.add(item.toString());
                                            }
                                        }
                                    }
                                    return completions;
                                }
                            } finally {
                                org.mozilla.javascript.Context.exit();
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error in tab completer for command '" + name + "': " + e.getMessage());
                        }
                        return null;
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to set tab completer for command '" + name + "': " + e.getMessage());
            }
        }
    }

    private void registerCommandInternal(String name, String description, String usage, Object executor) {
        try {

            PluginCommand command = plugin.getCommand(name);

            if (command == null) {

                try {
                    CommandMap commandMap = getCommandMap();

                    Command dynamicCommand = new Command(name) {
                        @Override
                        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                            try {
                                if (executor instanceof Function && apiHelper.getScope() != null) {
                                    org.mozilla.javascript.Context rhinoContext = org.mozilla.javascript.Context.enter();
                                    try {
                                        rhinoContext.setOptimizationLevel(-1);
                                        rhinoContext.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                                        Function func = (Function) executor;
                                        Scriptable scope = apiHelper.getScope();

                                        Scriptable jsArgs = rhinoContext.newArray(scope, args.length);
                                        for (int i = 0; i < args.length; i++) {

                                            jsArgs.put(i, jsArgs, args[i] != null ? args[i].toString() : "");
                                        }
                                        Object result = func.call(rhinoContext, scope, scope, new Object[]{sender, jsArgs});
                                        return result instanceof Boolean ? (Boolean) result : true;
                                    } finally {
                                        org.mozilla.javascript.Context.exit();
                                    }
                                }
                                return true;
                            } catch (Exception e) {
                                plugin.getLogger().severe("Error executing JS command '" + name + "': " + e.getMessage());
                                e.printStackTrace();
                                return false;
                            }
                        }
                    };

                    dynamicCommand.setDescription(description != null ? description : "");
                    dynamicCommand.setUsage(usage != null ? usage : "/" + name);

                    commandMap.register(name, plugin.getName().toLowerCase(), dynamicCommand);
                    dynamicCommands.add(dynamicCommand);
                    plugin.getLogger().info("Dynamically registered command: /" + name);
                    return;
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to register command '" + name + "' dynamically: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            }

            command.setExecutor((sender, cmd, label, args) -> {
                try {
                    if (executor instanceof Function && apiHelper.getScope() != null) {
                        org.mozilla.javascript.Context rhinoContext = org.mozilla.javascript.Context.enter();
                        try {
                            rhinoContext.setOptimizationLevel(-1);
                            rhinoContext.setLanguageVersion(org.mozilla.javascript.Context.VERSION_ES6);
                            Function func = (Function) executor;
                            Scriptable scope = apiHelper.getScope();

                            Scriptable jsArgs = rhinoContext.newArray(scope, args.length);
                            for (int i = 0; i < args.length; i++) {

                                jsArgs.put(i, jsArgs, args[i] != null ? args[i].toString() : "");
                            }
                            Object result = func.call(rhinoContext, scope, scope, new Object[]{sender, jsArgs});
                            return result instanceof Boolean ? (Boolean) result : true;
                        } finally {
                            org.mozilla.javascript.Context.exit();
                        }
                    }
                    return true;
                } catch (Exception e) {
                    plugin.getLogger().severe("Error executing JS command '" + name + "': " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            });

            if (description != null && !description.isEmpty()) {
                command.setDescription(description);
            }
            if (usage != null && !usage.isEmpty()) {
                command.setUsage(usage);
            }

            plugin.getLogger().info("Registered JS command: /" + name);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register command '" + name + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void unregisterCommands() {
        if (dynamicCommands.isEmpty()) return;
        try {
            CommandMap commandMap = getCommandMap();
            for (Command command : dynamicCommands) {
                command.unregister(commandMap);
                commandMap.getKnownCommands().values().removeIf(registered -> registered == command);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to unregister JS commands: " + e.getMessage());
        } finally {
            dynamicCommands.clear();
        }
    }

    private CommandMap getCommandMap() throws ReflectiveOperationException {
        Class<?> serverClass = plugin.getServer().getClass();
        while (serverClass != null) {
            try {
                Field commandMapField = serverClass.getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                return (CommandMap) commandMapField.get(plugin.getServer());
            } catch (NoSuchFieldException ignored) {
                serverClass = serverClass.getSuperclass();
            }
        }
        throw new NoSuchFieldException("commandMap");
    }
}
