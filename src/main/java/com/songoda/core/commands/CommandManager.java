package com.songoda.core.commands;

import com.songoda.core.compatibility.ServerProject;
import com.songoda.core.compatibility.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;

    private final HashMap<String, SimpleNestedCommand> commands = new HashMap<>();
    private final String msg_noConsole = ChatColor.RED + "You must be a player to use this command.";
    private final String msg_noPerms = ChatColor.RED + "You do not have permission to do that.";
    private final String msg_noCommand = ChatColor.GRAY + "The command you entered does not exist or is spelt incorrectly.";
    private final List<String> msg_syntaxError = Arrays.asList(
            ChatColor.RED + "Invalid Syntax!",
            ChatColor.GRAY + "The valid syntax is: " + ChatColor.GOLD + "%syntax%" + ChatColor.GRAY + "."
    );

    private final boolean allowLooseCommands = false;

    public CommandManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static void registerCommandDynamically(Plugin plugin, String command, CommandExecutor executor, TabCompleter tabManager) {
        try {
            // Retrieve the SimpleCommandMap from the server
            Class<?> clazzCraftServer = Bukkit.getServer().getClass();
            Object craftServer = clazzCraftServer.cast(Bukkit.getServer());
            SimpleCommandMap commandMap = (SimpleCommandMap) craftServer.getClass()
                    .getDeclaredMethod("getCommandMap").invoke(craftServer);

            // Construct a new Command object
            Constructor<PluginCommand> constructorPluginCommand = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructorPluginCommand.setAccessible(true);
            PluginCommand commandObject = constructorPluginCommand.newInstance(command, plugin);

            // If we're on Paper 1.8, we need to register timings (spigot creates timings on init, paper creates it on register)
            // later versions of paper create timings if needed when the command is executed
            if (ServerProject.isServer(ServerProject.PAPER, ServerProject.TACO) && ServerVersion.isServerVersionBelow(ServerVersion.V1_9)) {
                Class<?> clazz = Class.forName("co.aikar.timings.TimingsManager");
                Method method = clazz.getMethod("getCommandTiming", String.class, Command.class);
                Field field = PluginCommand.class.getField("timings");

                field.set(commandObject, method.invoke(null, plugin.getName().toLowerCase(), commandObject));
            }

            // Set command action
            commandObject.setExecutor(executor);

            // Set tab complete
            commandObject.setTabCompleter(tabManager);

            // Register the command
            Field fieldKnownCommands = SimpleCommandMap.class.getDeclaredField("knownCommands");
            fieldKnownCommands.setAccessible(true);
            Map<String, Command> knownCommands = (Map<String, Command>) fieldKnownCommands.get(commandMap);
            knownCommands.put(command, commandObject);
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
        }
    }

    public MainCommand addMainCommand(String command) {
        MainCommand nested = new MainCommand(plugin, command);
        commands.put(command.toLowerCase(), nested.nestedCommands);

        PluginCommand pcmd = plugin.getCommand(command);
        if (pcmd != null) {
            pcmd.setExecutor(this);
            pcmd.setTabCompleter(this);
        } else {
            plugin.getLogger().warning("Failed to register command: /" + command);
        }

        return nested;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        // grab the specific command that's being called
        SimpleNestedCommand nested = commands.get(command.getName().toLowerCase());

        if (nested != null) {
            // check to see if we're trying to call a sub-command
            if (args.length != 0 && !nested.children.isEmpty()) {
                String subCmd = getSubCommand(nested, args);

                if (subCmd != null) {
                    // we have a subcommand to use!
                    AbstractCommand sub = nested.children.get(subCmd);

                    // adjust the arguments to match - BREAKING!!
                    int i = subCmd.indexOf(' ') == -1 ? 1 : 2;
                    String[] newArgs = new String[args.length - i];
                    System.arraycopy(args, i, newArgs, 0, newArgs.length);

                    // now process the command
                    processRequirements(sub, commandSender, newArgs);
                    return true;
                }
            }

            // if we've gotten this far, then just use the command we have
            if (nested.parent != null) {
                processRequirements(nested.parent, commandSender, args);
                return true;
            }
        }

        commandSender.sendMessage(msg_noCommand);
        return true;
    }

    private String getSubCommand(SimpleNestedCommand nested, String[] args) {
        String cmd = args[0].toLowerCase();
        if (nested.children.containsKey(cmd)) {
            return cmd;
        }

        String match = null;
        // support for mulit-argument subcommands
        if (args.length >= 2 && nested.children.keySet().stream().anyMatch(k -> k.indexOf(' ') != -1)) {
            for (int len = args.length; len > 1; --len) {
                String cmd2 = String.join(" ", Arrays.copyOf(args, len)).toLowerCase();
                if (nested.children.containsKey(cmd2)) {
                    return cmd2;
                }
            }
        }

        // if we don't have a subcommand, should we search for one?
        if (allowLooseCommands) {
            // do a "closest match"
            int count = 0;
            for (String c : nested.children.keySet()) {
                if (c.startsWith(cmd)) {
                    match = c;
                    if (++count > 1) {
                        // there can only be one!
                        match = null;
                        break;
                    }
                }
            }
        }

        return match;
    }

    private void processRequirements(AbstractCommand command, CommandSender sender, String[] args) {
        if (!(sender instanceof Player) && command.isNoConsole()) {
            sender.sendMessage(msg_noConsole);
            return;
        }

        if (command.getPermissionNode() == null || sender.hasPermission(command.getPermissionNode())) {
            AbstractCommand.ReturnType returnType = command.runCommand(sender, args);

            if (returnType == AbstractCommand.ReturnType.NEEDS_PLAYER) {
                sender.sendMessage(msg_noConsole);
                return;
            }

            if (returnType == AbstractCommand.ReturnType.SYNTAX_ERROR) {
                for (String s : msg_syntaxError) {
                    sender.sendMessage(s.replace("%syntax%", command.getSyntax()));
                }
            }

            return;
        }

        sender.sendMessage(msg_noPerms);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // grab the specific command that's being called
        SimpleNestedCommand nested = commands.get(command.getName().toLowerCase());

        if (nested != null) {
            if (args.length == 0 || nested.children.isEmpty()) {
                return nested.parent != null ? nested.parent.onTab(sender, args) : null;
            }

            // check for each sub-command that they have access to
            final boolean op = sender.isOp();
            final boolean console = !(sender instanceof Player);
            if (args.length == 1) {
                // suggest sub-commands that this user has access to
                final String arg = args[0].toLowerCase();
                return nested.children.entrySet().stream()
                        .filter(e -> !console || !e.getValue().isNoConsole())
                        .filter(e -> e.getKey().startsWith(arg))
                        .filter(e -> op || e.getValue().getPermissionNode() == null || sender.hasPermission(e.getValue().getPermissionNode()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
            }

            // more than one arg, let's check to see if we have a command here
            String subCmd = getSubCommand(nested, args);
            AbstractCommand sub;
            if (subCmd != null &&
                    (sub = nested.children.get(subCmd)) != null &&
                    (!console || !sub.isNoConsole()) &&
                    (op || sub.getPermissionNode() == null || sender.hasPermission(sub.getPermissionNode()))) {
                // adjust the arguments to match - BREAKING!!
                int i = subCmd.indexOf(' ') == -1 ? 1 : 2;
                String[] newArgs = new String[args.length - i];
                System.arraycopy(args, i, newArgs, 0, newArgs.length);

                // we're good to go!
                return fetchList(sub, newArgs, sender);
            }
        }

        return Collections.emptyList();
    }

    private List<String> fetchList(AbstractCommand abstractCommand, String[] args, CommandSender sender) {
        List<String> list = abstractCommand.onTab(sender, args);

        if (args.length != 0) {
            String str = args[args.length - 1];

            if (list != null && str != null && str.length() >= 1) {
                try {
                    list.removeIf(s -> !s.toLowerCase().startsWith(str.toLowerCase()));
                } catch (UnsupportedOperationException ignore) {
                }
            }
        }

        return list;
    }

    /*
    private class DCommand extends PluginCommand {

        protected DCommand(@NotNull String name, @NotNull Plugin owner) {
            super(name, owner);
        }
    } */
}
