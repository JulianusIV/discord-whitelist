package julianusiv.discordwhitelist;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class Whitelist implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("discord-whitelist");
	private static MinecraftServer serverInstance;

	@Override
	public void onInitialize() {
		LOGGER.info("Starting up Discord-Whitelist mod!");

		ServerWorldEvents.LOAD.register((mcServer, serverWorld) -> {
			serverInstance = mcServer;
		});

		//register ban command 

		//Start discord bot
		//Thread t = new Thread(); t.start();

		LOGGER.info("Discord-Whitelist startup complete!");
	}

	public static ServerState getServerState() {
		return ServerState.getServerState(serverInstance);
	}

	public static void kickPlayers(List<String> names) {
		for (String name : names) {
			ServerPlayerEntity player = serverInstance.getPlayerManager().getPlayer(name);

			player.remove(RemovalReason.valueOf("The hammer has spoken!"));
		}
	}
}