package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.annotation.CapabilityMarker;
import edu.zsc.ai.plugin.enums.CapabilityEnum;
import edu.zsc.ai.plugin.exception.PluginException;
import edu.zsc.ai.plugin.model.command.*;

/**
 * Command executor capability.
 * Execute commands on their respective data sources.
 *
 * @param <T> the command request type
 * @param <R> the command result type
 */
@CapabilityMarker(CapabilityEnum.COMMAND_EXECUTOR)
public interface CommandExecutor<T extends CommandRequest, R extends CommandResult> {

    /**
     * Execute a command.
     *
     * @param command the command to execute
     * @return the execution result
     * @throws PluginException if execution fails
     */
    R executeCommand(T command) throws PluginException;
}