package julianusiv.discordwhitelist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class ServerState extends PersistentState {

    private static Map<String, Long> whitelist = new HashMap<String, Long>();
    private static List<Long> banlist = new ArrayList<Long>();

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        for (Map.Entry<String, Long> entry : whitelist.entrySet()) {
            nbt.putLong("dcwl-" + entry.getKey(), entry.getValue());
        }
        nbt.putLongArray("banlist", banlist);
        return nbt;
    }
    
    public static ServerState createFromNbt(NbtCompound nbt) {
        ServerState serverState = new ServerState();
        Set<String> keys = nbt.getKeys();
        for (String key : keys) {
            if (key.startsWith("dcwl-"))
                whitelist.put(key.substring(5), nbt.getLong(key));
        }
        long[] bans = nbt.getLongArray("banlist");
        banlist = Arrays.stream(bans).boxed().collect(Collectors.toList());

        return serverState;
    }

    public static ServerState getServerState(MinecraftServer server) {
        PersistentStateManager psm = server.getWorld(World.OVERWORLD).getPersistentStateManager();

        return psm.getOrCreate(ServerState::createFromNbt, ServerState::new, "discord-whitelist");
    }

    public boolean whitelistUser(String username, long discordId) {
        if (banlist.contains(discordId))
            return false;

        whitelist.put(username, discordId);
        this.markDirty();
        return true;
    }

    public void unwhitelistUser(String username) {
        whitelist.remove(username);
        this.markDirty();
    }

    public void banUser(String username) {
        long discordId = whitelist.get(username);
        banUser(discordId);
    }

    public void banUser(long discordId) {
        List<String> toRemove = new ArrayList<String>();
        for (Map.Entry<String, Long> entry : whitelist.entrySet()) {
            if (entry.getValue() == discordId)
                toRemove.add(entry.getKey());
        }

        for (String key : toRemove) {
            whitelist.remove(key);
        }

        banlist.add(discordId);

        Whitelist.kickPlayers(toRemove);

        this.markDirty();
    }

    public void unbanUser(long discordId) {
        banlist.remove(discordId);
        this.markDirty();
    }

    public boolean isWhitelisted(String username) {
        return whitelist.containsKey(username);
    }
}
