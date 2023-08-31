package julianusiv.discordwhitelist;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import julianusiv.discordwhitelist.config.SimpleConfig;
import julianusiv.discordwhitelist.data.WhitelistEntry;
import julianusiv.discordwhitelist.discord.DiscordBot;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

public class Whitelist implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("discord-whitelist");
	public static final SimpleConfig CONFIG = SimpleConfig.of("discord-whitelist").provider((filename) -> {
		//default config
		return """
			discord.token=yourtokenhere
			discord.guild.id=0
			# discord.guild.chatchannel=0
			# discord.guild.chatthread=0
			""";
	}).request();

	private static MinecraftServer serverInstance = null;
	private static boolean sentStartupMessage = false;

	@Override
	public void onInitialize() {
		LOGGER.info("Starting up Discord-Whitelist mod!");

		//register events
		ServerWorldEvents.LOAD.register((mcServer, serverWorld) -> {
			serverInstance = mcServer;
			if (!sentStartupMessage) {
				DiscordBot.publishStartStop("Server started.");
				sentStartupMessage = true;
			}
		});
		ServerMessageEvents.CHAT_MESSAGE.register((message, player, params) -> {
			DiscordBot.publishChatMessage(message, player);
		});
		ServerMessageEvents.COMMAND_MESSAGE.register((message, source, params) -> {
			DiscordBot.publishCommandMessage(message, params);
		});
		ServerMessageEvents.GAME_MESSAGE.register((server, text, overlay) -> {
			if (text.getContent() instanceof TranslatableTextContent)
				DiscordBot.publishGameMessage(text);
		});

		//register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LiteralCommandNode<ServerCommandSource> ban = CommandManager.literal("whiteban").requires(source -> source.hasPermissionLevel(1)).build();
			ArgumentCommandNode<ServerCommandSource, EntitySelector> player = CommandManager.argument("player", EntityArgumentType.player()).executes(Whitelist::whiteban).build();
			dispatcher.getRoot().addChild(ban);
			ban.addChild(player);
		});

		//Start discord bot
		Thread t = new Thread(new DiscordBot()); 
		t.start();

		LOGGER.info("Discord-Whitelist startup complete!");
	}

	public static ServerState getServerState() {
		return ServerState.getServerState(serverInstance);
	}

	public static void announceServerShutdown() {
		serverInstance.getPlayerManager().broadcast(Text.of("Server is shutting down in 10 seconds."), false);
		try {
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException e) {
		}
		serverInstance.getPlayerManager().disconnectAllPlayers();
	}

	public static int getPlayerCount() {
		return serverInstance.getCurrentPlayerCount();
	}

	public static String[] getOnlinePlayers() {
		return serverInstance.getPlayerNames();
	}

	public static void forwardMessage(String message) {
		serverInstance.getPlayerManager().broadcast(Text.of(message), false);
	}

	public static void shutdown() {
		LOGGER.info("Shutting down!");
		while (serverInstance == null) {
		}
		serverInstance.stop(false);
	}

	public static void kickPlayers(List<WhitelistEntry> users, String message) {
		for (WhitelistEntry entry : users) {
			ServerPlayerEntity player = serverInstance.getPlayerManager().getPlayer(entry.getUsername());

			player.networkHandler.disconnect(Text.of(message));
		}
	}

	private static int whiteban(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");

		int banned = getServerState().banUser(player.getUuidAsString(), true);

		context.getSource().getPlayer().sendMessageToClient(Text.of("Successfully banned user, " + banned + " accounts were removed in total due to association with the same discord account."), false);
		return 1;
	}
}