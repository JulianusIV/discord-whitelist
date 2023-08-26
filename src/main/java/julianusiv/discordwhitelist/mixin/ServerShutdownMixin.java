package julianusiv.discordwhitelist.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import julianusiv.discordwhitelist.Whitelist;
import julianusiv.discordwhitelist.discord.DiscordBot;
import net.dv8tion.jda.api.OnlineStatus;
import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public class ServerShutdownMixin {
    @Inject(at = @At("HEAD"), method = "shutdown")
    private void beforeServerShutdown(CallbackInfo info) {
        if (DiscordBot.jda != null) {
            Whitelist.LOGGER.info("Shutting down discord bot.");
            DiscordBot.jda.getPresence().setStatus(OnlineStatus.OFFLINE);
            DiscordBot.jda.shutdown();
            Whitelist.LOGGER.info("Discord-Whitelist shutdown complete.");
        }
    }
}
