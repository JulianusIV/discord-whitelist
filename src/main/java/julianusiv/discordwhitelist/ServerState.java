package julianusiv.discordwhitelist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import julianusiv.discordwhitelist.data.WhitelistEntry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class ServerState extends PersistentState {

    private static List<WhitelistEntry> whitelist = new ArrayList<WhitelistEntry>();
    private static List<Long> banlist = new ArrayList<Long>();

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        for (WhitelistEntry entry : whitelist) {
            nbt.putString("dcwl-" + entry.getDiscordId(), entry.getUuid() + ";" + entry.getUsername());
        }
        nbt.putLongArray("banlist", banlist);
        return nbt;
    }
    
    public static ServerState createFromNbt(NbtCompound nbt) {
        ServerState serverState = new ServerState();
        Set<String> keys = nbt.getKeys();
        for (String key : keys) {
            if (key.startsWith("dcwl-")) {
                String[] value = nbt.getString(key).split(";");
                whitelist.add(new WhitelistEntry(value[1], value[0], Long.parseLong(key.substring(5))));
            }
        }
        long[] bans = nbt.getLongArray("banlist");
        banlist = Arrays.stream(bans).boxed().collect(Collectors.toList());

        return serverState;
    }

    public static ServerState getServerState(MinecraftServer server) {
        PersistentStateManager psm = server.getWorld(World.OVERWORLD).getPersistentStateManager();

        return psm.getOrCreate(ServerState::createFromNbt, ServerState::new, "discord-whitelist");
    }

    public boolean whitelistUser(String username, String uuid, long discordId) {
        if (banlist.contains(discordId))
            return false;

        whitelist.add(new WhitelistEntry(username, uuid, discordId));
        this.markDirty();
        return true;
    }

    public int unwhitelistUser(long discordId) {
        List<WhitelistEntry> toRemove = new ArrayList<WhitelistEntry>();
        for (WhitelistEntry entry : whitelist) {
            if (entry.getDiscordId() == discordId)
                toRemove.add(entry);
        }

        for (WhitelistEntry entry : toRemove) {
            whitelist.remove(entry);
        }

        Whitelist.kickPlayers(toRemove, "You were kicked from the whitelist.");

        this.markDirty();

        return toRemove.size();
    }

    public int unwhitelistUser(String username) {
        long discordId = 0;
        for (WhitelistEntry entry : whitelist) {
            if (entry.getUsername().equals(username))
                discordId = entry.getDiscordId();
        }
        if (discordId != 0)
            return unwhitelistUser(discordId);
        return 0;
    }

    public int banUser(String username, boolean uuid) {
        long discordId = 0;
        for (WhitelistEntry entry : whitelist) {
            String toComp = uuid ? entry.getUuid() : entry.getUsername();
            if (toComp.equals(username))
                discordId = entry.getDiscordId();
        }
        if (discordId != 0)
            return banUser(discordId);
        return 0;
    }

    public int banUser(long discordId) {
        List<WhitelistEntry> toRemove = new ArrayList<WhitelistEntry>();
        for (WhitelistEntry entry : whitelist) {
            if (entry.getDiscordId() == discordId)
                toRemove.add(entry);
        }

        for (WhitelistEntry entry : toRemove) {
            whitelist.remove(entry);
        }

        banlist.add(discordId);

        Whitelist.kickPlayers(toRemove, "The hammer has spoken!");

        this.markDirty();

        return toRemove.size();
    }

    public void unbanUser(long discordId) {
        banlist.remove(discordId);
        this.markDirty();
    }

    public boolean isWhitelisted(String username) {
        for (WhitelistEntry entry : whitelist) {
            if (entry.getUsername().equals(username))
                return true;
        }
        return false;
    }
}
