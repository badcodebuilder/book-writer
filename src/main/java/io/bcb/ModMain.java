package io.bcb;

import io.bcb.commands.BookCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;

/**
 * ModMain
 */
public class ModMain implements ModInitializer{

    @Override
    public void onInitialize() {
        BookCommand.register(ClientCommandManager.DISPATCHER);

        System.out.println("hello, world");
        System.out.println("Welcome to book-writer");
    }

}