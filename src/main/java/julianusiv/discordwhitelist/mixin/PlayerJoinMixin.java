package julianusiv.discordwhitelist.mixin;

import java.net.SocketAddress;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.GameProfile;

import julianusiv.discordwhitelist.Whitelist;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;

@Mixin(PlayerManager.class)
public class PlayerJoinMixin {
	@Inject(at = @At("HEAD"), cancellable = true, method = "checkCanJoin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/text/Text;")
	private void checkCanJoin(SocketAddress sockAddr, GameProfile profile, CallbackInfoReturnable<Text> info) {
		boolean isWhitelisted = Whitelist.getServerState().isWhitelisted(profile.getName());
		if (!isWhitelisted)
			info.setReturnValue(Text.of("You are not whitelisted, BOZO"));
	}
}