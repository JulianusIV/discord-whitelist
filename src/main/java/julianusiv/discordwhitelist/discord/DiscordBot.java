package julianusiv.discordwhitelist.discord;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;

import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;

import julianusiv.discordwhitelist.Whitelist;
import julianusiv.discordwhitelist.data.UserVerificationResult;
import julianusiv.discordwhitelist.data.WhitelistEntry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.minecraft.network.message.MessageType.Parameters;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class DiscordBot implements EventListener, Runnable {
    public static JDA jda = null;
    private static TextChannel chatChannel = null;
    private static ThreadChannel chatThread = null;

    public static void publishChatMessage(SignedMessage message, ServerPlayerEntity player) {
        MessageCreateData dcMessage = new MessageCreateBuilder()
            .addEmbeds(new EmbedBuilder()
                .setAuthor(player.getName().getString())
                .setDescription(message.getSignedContent())
                .setColor(Color.GREEN)
                .build())
            .build();

        if (chatChannel != null)
            chatChannel.sendMessage(dcMessage).queue();
        if (chatThread != null)
            chatThread.sendMessage(dcMessage).queue();
    }

    public static void publishCommandMessage(SignedMessage message, Parameters params) {
        MessageCreateData dcMessage = new MessageCreateBuilder()
            .addEmbeds(new EmbedBuilder()
                .setDescription(params.applyChatDecoration(message.getContent()).getString())
                .setColor(Color.GREEN)
                .build())
            .build();

        if (chatChannel != null)
            chatChannel.sendMessage(dcMessage).queue();
        if (chatThread != null)
            chatThread.sendMessage(dcMessage).queue();
    }

    public static void publishGameMessage(Text text) {
        MessageCreateData dcMessage = new MessageCreateBuilder()
            .addEmbeds(new EmbedBuilder()
                .setAuthor("Server", "https://lathcraft.julianusiv.de", jda.getSelfUser().getAvatarUrl())
                .setDescription(text.getString())
                .setColor(Color.GREEN)
                .build())
            .build();

        if (chatChannel != null)
            chatChannel.sendMessage(dcMessage).queue();
        if (chatThread != null)
            chatThread.sendMessage(dcMessage).queue();
    }

    public static void publishStartStop(String message) {
        MessageCreateData dcMessage = new MessageCreateBuilder()
            .addEmbeds(new EmbedBuilder()
                .setAuthor("Server", "https://lathcraft.julianusiv.de", jda.getSelfUser().getAvatarUrl())
                .setDescription(message)
                .setColor(Color.GREEN)
                .build())
            .build();

        if (chatChannel != null)
            chatChannel.sendMessage(dcMessage).queue();
        if (chatThread != null)
            chatThread.sendMessage(dcMessage).queue();
    }

    @Override
    public void run() {
        // start discordbot
        String token = Whitelist.CONFIG.getOrDefault("discord.token", null);
        if (token == null || token.equals("yourtokenhere")) {
            Whitelist.LOGGER.error("Could not find token in configuration, please supply one!");
            Whitelist.shutdown();
            return;
        }
        long chatChannel = Whitelist.CONFIG.getOrDefault("discord.guild.chatchannel", 0);
        long chatThread = Whitelist.CONFIG.getOrDefault("discord.guild.chatthread", 0);

        Collection<GatewayIntent> intents = GatewayIntent.getIntents(GatewayIntent.DEFAULT);
        if (chatChannel != 0 || chatThread != 0)
            intents.add(GatewayIntent.MESSAGE_CONTENT);

        jda = JDABuilder.createLight(token, intents)
                .setMemberCachePolicy(MemberCachePolicy.DEFAULT)
                .addEventListeners(this).build();

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        jda.getPresence().setStatus(OnlineStatus.ONLINE);
        jda.getPresence().setActivity(Activity.playing("Minecraft"));

        Whitelist.LOGGER.info("ID: " + jda.getSelfUser().getName() + " / " + jda.getSelfUser().getIdLong());

        long guildId = Whitelist.CONFIG.getOrDefault("discord.guild.id", 0);
        if (guildId == 0) {
            Whitelist.LOGGER.error("Could not find guildid in configuration, please supply one!");
            Whitelist.shutdown();
            return;
        }
        for (Guild x : jda.getGuilds()) {
            Whitelist.LOGGER.info("\tin Guild: " + x.getIdLong() + " | " + x.getName() + " / " + x.getOwnerIdLong());
            if (x.getIdLong() == guildId) {
                x.updateCommands()
                        .addCommands(Commands.slash("whitelist", "whitelist your minecraft account(s)")
                                .setGuildOnly(true)
                                .addOption(OptionType.STRING, "username", "Name of the minecraft account", true, false))
                        .addCommands(Commands.slash("whitelistbedrock", "whitelist your minecraft bedrock account(s)")
                                .setGuildOnly(true)
                                .addOption(OptionType.STRING, "username", "Name of the minecraft account", true, false))
                        .addCommands(Commands.slash("whitekick", "Kick someone from the whitelist")
                                .setGuildOnly(true)
                                .setDefaultPermissions(DefaultMemberPermissions
                                        .enabledFor(net.dv8tion.jda.api.Permission.KICK_MEMBERS))
                                .addOption(OptionType.USER, "member", "Member to whiteban", true, false))
                        .addCommands(Commands.slash("whiteban",
                                "Ban someone from whitelisting accounts, and remove all their current accounts from the whitelist")
                                .setGuildOnly(true)
                                .setDefaultPermissions(DefaultMemberPermissions
                                        .enabledFor(net.dv8tion.jda.api.Permission.KICK_MEMBERS))
                                .addOption(OptionType.USER, "member", "Member to whiteban", true, false))
                        .addCommands(Commands.slash("unban", "Unban someone from whitelisting accounts")
                                .setGuildOnly(true)
                                .setDefaultPermissions(DefaultMemberPermissions
                                        .enabledFor(net.dv8tion.jda.api.Permission.KICK_MEMBERS))
                                .addOption(OptionType.USER, "member", "Member to unban", true, false))
                        .addCommands(Commands.slash("online", "check who is online")
                                .setGuildOnly(true)
                                .addSubcommands(new SubcommandData("count", "Count online players"))
                                .addSubcommands(
                                        new SubcommandData("who", "Get a list of all currently online players")))
                        .queue();
                Whitelist.LOGGER.info("\t\tSet up slash commands for this guild");
                
                if (chatChannel != 0)
                    DiscordBot.chatChannel = x.getChannelById(TextChannel.class, chatChannel);
                if (chatThread != 0)
                    DiscordBot.chatThread = x.getThreadChannelById(chatThread);
            }
        }
    }

    @Override
    public void onEvent(GenericEvent event) {
        if (event instanceof SlashCommandInteractionEvent ctx) {
            ctx.deferReply().queue();
            String commandName = ctx.getName();
            switch (commandName) {
                case "whitelist":
                    whitelistCommand(ctx, false);
                    break;
                case "whitelistbedrock":
                    whitelistCommand(ctx, true);
                    break;
                case "whitekick":
                    whitekickCommand(ctx);
                    break;
                case "whiteban":
                    whitebanCommand(ctx);
                    break;
                case "unban":
                    unbanCommand(ctx);
                    break;
                case "online":
                    String subCmdName = ctx.getSubcommandName();
                    if (subCmdName.equals("count")) {
                        onlineCountCommand(ctx);
                        break;
                    }
                    if (subCmdName.equals("who")) {
                        onlineWhoCommand(ctx);
                        break;
                    }
                    break;
                default:
                    break;
            }
        }
        if (event instanceof ReadyEvent){
            Whitelist.LOGGER.info("API is ready!");
            return;
        }

        if (event instanceof MessageReceivedEvent ctx) {
            messageReceivedEventHandler(ctx);
            return;
        }
    }

    private void whitelistCommand(SlashCommandInteractionEvent ctx, boolean bedrock) {
        String username = ctx.getOption("username").getAsString();
        if (Whitelist.getServerState().isWhitelisted(username)) {
            ctx.getHook().sendMessage("That user is already whitelisted!").queue();
            return;
        }

        UserVerificationResult result = null;
        if (!bedrock) {
            result = verifyUserName(username);
            if (!result.isSuccess()) {
                ctx.getHook().sendMessage("There was an error validating that user.").queue();
                return;
            }
            if (!result.isValid()) {
                ctx.getHook().sendMessage("Account not found.").queue();
                return;
            }
        }

        boolean success = false;
        if (bedrock) {
            success = Whitelist.getServerState().whitelistUser(
                    username,
                    "", // use empty uuid to identify bedrock players, gotta find something better here
                    ctx.getMember().getIdLong());
        } else {
            success = Whitelist.getServerState().whitelistUser(
                    result.getEntry().getUsername(),
                    result.getEntry().getUuid(),
                    ctx.getMember().getIdLong());
        }

        if (!success) {
            ctx.getHook().sendMessage("You are banned from using this command!").queue();
            return;
        }
        ctx.getHook().sendMessage("Whitelist entry added." + (bedrock
                ? "\nYou are using a bedrock account, which means your username doesnt get verified. Hope you typed it correcrly."
                : "")).queue();
    }

    private void whitekickCommand(SlashCommandInteractionEvent ctx) {
        Member member = ctx.getOption("member").getAsMember();
        int kicked = Whitelist.getServerState().unwhitelistUser(member.getIdLong());
        ctx.getHook().sendMessage("Successfully kicked member " + member.getAsMention() + " from the whitelist, "
                + kicked + " names were removed from the whitelist.").queue();
    }

    private void whitebanCommand(SlashCommandInteractionEvent ctx) {
        Member member = ctx.getOption("member").getAsMember();
        int banned = Whitelist.getServerState().banUser(member.getIdLong());
        ctx.getHook().sendMessage("Successfully banned member " + member.getAsMention() + ", " + banned
                + " names were removed from the whitelist.").queue();
    }

    private void unbanCommand(SlashCommandInteractionEvent ctx) {
        Member member = ctx.getOption("member").getAsMember();
        Whitelist.getServerState().unbanUser(member.getIdLong());
        ctx.getHook().sendMessage("Sucessfully unbanned user " + member.getAsMention()).queue();
    }

    private void onlineCountCommand(SlashCommandInteractionEvent ctx) {
        int players = Whitelist.getPlayerCount();
        ctx.getHook().sendMessage("There are currently " + players + " players on the server.").queue();
    }

    private void onlineWhoCommand(SlashCommandInteractionEvent ctx) {
        String[] players = Whitelist.getOnlinePlayers();
        if (players.length == 0) {
            ctx.getHook().sendMessage("No players currently online.").queue();
            return;
        }
        StringBuilder playerlist = new StringBuilder("These players are currently online:\n");
        for (int i = 0; i < players.length; i++) {
            playerlist.append(i)
                    .append(". ")
                    .append(players[i])
                    .append('\n');
        }
        ctx.getHook().sendMessage(playerlist.toString()).queue();
    }

    private void messageReceivedEventHandler(MessageReceivedEvent ctx) {
        if (ctx.getAuthor().isBot())
            return;
        
        long channelId = ctx.getChannel().getIdLong();
        if (channelId != chatChannel.getIdLong() && channelId != chatThread.getIdLong())
            return;
        
        StringBuilder builder = new StringBuilder(ctx.getAuthor().getEffectiveName());
        builder.append(": ");
        builder.append(ctx.getMessage().getContentDisplay());

        Whitelist.forwardMessage(builder.toString());
    }

    private UserVerificationResult verifyUserName(String username) {
        HttpURLConnection connection = null;

        try {
            // Create connection
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);

            // Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\n');
            }

            rd.close();
            connection.disconnect();

            System.out.println("RESPONSE: " + response.toString());
            // Parse Response
            JsonStreamParser jp = new JsonStreamParser(response.toString().trim());
            JsonObject jo = jp.next().getAsJsonObject();
            // create response object and return
            if (jo.has("id")) {
                String uuid = jo.get("id").getAsString();
                String name = jo.get("name").getAsString();

                return new UserVerificationResult(
                        new WhitelistEntry(name, uuid, 0),
                        true,
                        true);
            } else {
                return new UserVerificationResult(null, false, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new UserVerificationResult(null, false, false);
        } finally {
            if (connection != null)
                connection.disconnect();
        }
    }
}
