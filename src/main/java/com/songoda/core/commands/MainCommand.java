package com.songoda.core.commands;

import com.songoda.core.chat.ChatMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MainCommand extends AbstractCommand {
    protected final SimpleNestedCommand nestedCommands;
    final String command;
    String header = null;
    String description;
    boolean sortHelp = false;

    public MainCommand(Plugin plugin, String command) {
        super(CommandType.CONSOLE_OK, command);

        this.command = command;
        this.description = "Shows the command help page for /" + command;
        this.nestedCommands = new SimpleNestedCommand(this);
    }

    public MainCommand addSubCommands(AbstractCommand... commands) {
        nestedCommands.addSubCommands(commands);
        return this;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        sender.sendMessage("");

        if (header != null) {
            sender.sendMessage(header);
        }

        if (nestedCommands != null) {
            List<String> commands = nestedCommands.children.values().stream().distinct().map(c -> c.getCommands().get(0)).collect(Collectors.toList());

            if (sortHelp) {
                Collections.sort(commands);
            }

            boolean isPlayer = sender instanceof Player;
            sender.sendMessage(ChatColor.DARK_GRAY + "- " + ChatColor.YELLOW + getSyntax() + ChatColor.GRAY + " - " + getDescription());

            for (String cmdStr : commands) {
                final AbstractCommand cmd = nestedCommands.children.get(cmdStr);
                if (cmd == null) continue;
                if (!isPlayer) {
                    sender.sendMessage(ChatColor.DARK_GRAY + "- " + ChatColor.YELLOW + cmd.getSyntax() + ChatColor.GRAY + " - " + cmd.getDescription());
                } else if (cmd.getPermissionNode() == null || sender.hasPermission(cmd.getPermissionNode())) {
                    ChatMessage chatMessage = new ChatMessage();
                    final String c = "/" + command + " ";
                    chatMessage.addMessage(ChatColor.DARK_GRAY + "- ")
                            .addPromptCommand(ChatColor.YELLOW + c + cmd.getSyntax(), ChatColor.YELLOW + c + cmdStr, c + cmdStr)
                            .addMessage(ChatColor.GRAY + " - " + cmd.getDescription());
                    chatMessage.sendTo(sender);
                }
            }
        }

        sender.sendMessage("");

        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        // don't need to worry about tab for a root command - handled by the manager
        return null;
    }

    @Override
    public String getPermissionNode() {
        // permissions for a root command should be handled in the plugin.yml
        return null;
    }

    @Override
    public String getSyntax() {
        return "/" + command;
    }

    @Override
    public String getDescription() {
        return description;
    }

}
