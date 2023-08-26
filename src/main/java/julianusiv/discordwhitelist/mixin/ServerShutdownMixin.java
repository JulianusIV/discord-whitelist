package julianusiv.discordwhitelist.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public class ServerShutdownMixin {
    @Inject(at = @At("HEAD"), method = "shutdown")
    private void beforeServerShutdown(CallbackInfo info) {
        //shutdown discord bot
    }
}
