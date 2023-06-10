package com.songoda.core.commands;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class AbstractCommand {
    private final CommandType _cmdType;
    private final List<String> _handledCommands = new ArrayList<>();

    protected AbstractCommand(CommandType type, String... command) {
        this._handledCommands.addAll(Arrays.asList(command));
        this._cmdType = type;
    }

    protected AbstractCommand(CommandType type, boolean hasArgs, String... command) {
        this._handledCommands.addAll(Arrays.asList(command));
        this._cmdType = type;
    }

    @Deprecated
    protected AbstractCommand(boolean noConsole, String... command) {
        this._handledCommands.addAll(Arrays.asList(command));
        this._cmdType = noConsole ? CommandType.PLAYER_ONLY : CommandType.CONSOLE_OK;
    }

    @Deprecated
    protected AbstractCommand(boolean noConsole, boolean hasArgs, String... command) {
        this._handledCommands.addAll(Arrays.asList(command));
        this._cmdType = noConsole ? CommandType.PLAYER_ONLY : CommandType.CONSOLE_OK;
    }

    public final List<String> getCommands() {
        return Collections.unmodifiableList(_handledCommands);
    }

    protected abstract ReturnType runCommand(CommandSender sender, String... args);

    protected abstract List<String> onTab(CommandSender sender, String... args);

    public abstract String getPermissionNode();

    public abstract String getSyntax();

    public abstract String getDescription();

    public boolean isNoConsole() {
        return _cmdType == CommandType.PLAYER_ONLY;
    }

    public enum ReturnType {SUCCESS, NEEDS_PLAYER, FAILURE, SYNTAX_ERROR}

    public enum CommandType {PLAYER_ONLY, CONSOLE_OK}
}

