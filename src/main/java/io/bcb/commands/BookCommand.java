package io.bcb.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.bcb.utils.BookGenerator;
import io.bcb.utils.parser.Parser;
import io.bcb.utils.parser.TXTParser;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class BookCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> cmdBookList = ClientCommandManager.literal("list")
            .executes(BookCommand::bookList);

        LiteralArgumentBuilder<FabricClientCommandSource> cmdBookWrite = ClientCommandManager.literal("write")
            .then(ClientCommandManager.argument("file", StringArgumentType.string())
                .suggests((context, builder) -> {
                    File[] files = getFiles(context.getSource().getClient().runDirectory);
                    if (files == null) {
                        context.getSource().sendError(new LiteralText("Can neither find '.minecraft/bookwriter/' nor create it."));
                        return builder.buildFuture();
                    }

                    for (File file : files) {
                        builder.suggest(file.getName());
                    }
                    return builder.buildFuture();
                })
                .executes(BookCommand::bookWrite));

        LiteralArgumentBuilder<FabricClientCommandSource> cmdBookRandomWrite = ClientCommandManager.literal("random")
            .executes((context) -> {
                return 0;
            });

        dispatcher.register(ClientCommandManager.literal("book")
            .then(cmdBookList)
            .then(cmdBookWrite)
            .then(cmdBookRandomWrite));
    }

    private static File[] getFiles(File minecraftRunDir) {
        File fileDir = new File(minecraftRunDir, "bookwriter");
        if (!fileDir.exists() && !fileDir.mkdir()) {
            return null;
        }
        return fileDir.listFiles((dir, name) -> {
            return name.endsWith(".txt");
        });
    }

    private static int bookList(CommandContext<FabricClientCommandSource> context) {
        File[] files = getFiles(context.getSource().getClient().runDirectory);
        if (files == null) {
            context.getSource().sendError(new LiteralText("Can neither find '.minecraft/bookwriter/' nor create it."));
            return 1;
        }

        context.getSource().sendFeedback(new LiteralText(String.format("%d files found", files.length)));
        for (File file : files) {
            // XXX: maybe it need an entire string
            context.getSource().sendFeedback(new LiteralText(file.getName()));
        }
        return 0;
    }

    private static int bookWrite(CommandContext<FabricClientCommandSource> context) {
        byte[] sizes = new byte[65536];
        try {
            context.getSource().getClient().getResourceManager().getResource(
                new Identifier("minecraft", "font/glyph_sizes.bin")
            ).getInputStream().read(sizes);
        } catch (IOException e) {
            context.getSource().sendError(new LiteralText("ResourceError: Cannot get font infomation"));
            e.printStackTrace();
            return -1;
        }

        String filename = StringArgumentType.getString(context, "file");
        if (filename == null) {
            context.getSource().sendError(new LiteralText("ArgumentError: Filename must be specified"));
            return 1;
        }

        File bookFile = new File(context.getSource().getClient().runDirectory, String.format("bookwriter/%s", filename));
        Parser parser = new TXTParser();
        BookGenerator bookGen = new BookGenerator(bookFile, parser, sizes);

        int slot = 0;
        int bookUsedCount = 0;
        for (ItemStack itemStack : context.getSource().getPlayer().inventory.main) {
            if (isWritableBook(itemStack)) {
                if (!bookGen.getIsBookEnd()) {
                    List<String> book = bookGen.generate();
                    if (book == null) {
                        context.getSource().sendError(new LiteralText("FileError: cannot read file properly"));
                        break;
                    }

                    NbtList pages = new NbtList();
                    book.stream().map(NbtString::of).forEach(pages::add);
                    itemStack.putSubTag("pages", pages);

                    context.getSource().getClient().getNetworkHandler().sendPacket(
                        new BookUpdateC2SPacket(itemStack, false, slot)
                    );
                    ++bookUsedCount;
                } else {
                    context.getSource().sendFeedback(new LiteralText(String.format("%d books used", bookUsedCount)));
                    break;
                }
            }
            ++slot;
        }
        return 0;
    }

    private static boolean isWritableBook(ItemStack itemStack) {
        final Identifier bookID = Registry.ITEM.getId(Items.WRITABLE_BOOK);
        return Registry.ITEM.getId(itemStack.getItem()).equals(bookID);
    }
}
