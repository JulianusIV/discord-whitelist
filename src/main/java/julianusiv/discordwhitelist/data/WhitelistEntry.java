package julianusiv.discordwhitelist.data;

public class WhitelistEntry {
    public WhitelistEntry(String username, String uuid, long discordId) {
        this.username = username;
        this.uuid = uuid;
        this.discordId = discordId;
    }

    private String username;
    private String uuid;
    private long discordId;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getDiscordId() {
        return discordId;
    }
    
    public void setDiscordId(long discordId) {
        this.discordId = discordId;
    }
}
