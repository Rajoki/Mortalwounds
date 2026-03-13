package com.rajoki.injuryplugin.commands;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadLocalRandom;

public class RandomHitCommandTest extends CommandBase {

    // /randomhit rolls a random number 1-100, for testing purposes.

    public RandomHitCommandTest() {
        super("randomhit", "Rolls a random number between 1 and 100");
        this.setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        int randomNumber = ThreadLocalRandom.current().nextInt(1, 101);
        ctx.sendMessage(Message.raw("Random number: " + randomNumber));
    }
}