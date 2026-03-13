package com.rajoki.injuryplugin.commands;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

public class InjuryTestCommand extends CommandBase {

    // /injury just to make sure it's working/activated

    public InjuryTestCommand() {
        super("injury", "Tests if the injury system is working.");
        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP
        HytaleLogger.forEnclosingClass().atInfo().log("InjuryTestCommand constructor called!");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        ctx.sendMessage(Message.raw("Injuries are on!"));
    }
}