package me.agnes.trDiscordSync.configuration;

import com.bentahsin.configuration.annotation.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigHeader({
        "#################################################################################",
        "#                                                                               #",
        "#                        Agnes Account Linking System                           #",
        "#           Advanced Configuration for Minecraft & Discord Integration          #",
        "#                                                                               #",
        "#################################################################################",
        "",
        "Need help? Contact support or check documentation.",
        ""
})
@ConfigVersion(2)
@Backup(enabled = true, onFailure = true, onMigration = true)
public class MainConfig {

    @Comment({
            "----------------------------------------------------------------",
            " DYNAMIC COMMAND SYSTEM",
            "----------------------------------------------------------------",
            "Configure the primary command and aliases for the plugin.",
            "You can add multiple aliases by separating them with '|'.",
            "Example: 'link|sync|accountlink'"
    })
    public CommandSection commands = new CommandSection();

    @Comment({
            "----------------------------------------------------------------",
            " DISCORD BOT SETTINGS",
            "----------------------------------------------------------------",
            "Your Discord Bot Token from the Discord Developer Portal.",
            "MAKE SURE the bot has 'Server Members Intent' and 'Message Content Intent' enabled."
    })
    @Validate(notNull = true)
    public String token = "DISCORD_BOT_TOKEN";

    @Comment({
            "The ID of the Discord Server (Guild) where the bot will operate."
    })
    @ConfigPath("guild-id")
    public String guildId = "";

    @Comment({
            "Status messages displayed on the Discord Bot's presence.",
            "Supports placeholders: {online}, {max_players}, {server_version}"
    })
    @ConfigPath("status-messages")
    public List<String> statusMessages = Arrays.asList(
            "ServerName.net",
            "Linking accounts...",
            "{online} Players online");

    @Comment({
            "----------------------------------------------------------------",
            " LOGGING SYSTEM",
            "----------------------------------------------------------------",
            "The channel ID where the bot will log linking activities and reports."
    })
    @ConfigPath("log-channel-id")
    public String logChannelId = "log-channel-id";

    @Comment("Enable or disable the logging system to the log-channel.")
    @ConfigPath("log-system")
    public Boolean logSystem = true;

    @Comment({
            "----------------------------------------------------------------",
            " ROLE SYSTEM",
            "----------------------------------------------------------------",
            "Admin Role ID: Users with this role can manage the bot (e.g., reports, unlinking)."
    })
    @ConfigPath("admin-role-id")
    public String adminRoleId = "admin-role-id";

    @Comment("Verified Role ID: Given automatically to users after successful account linking.")
    @ConfigPath("verified-role-id")
    public String verifiedRoleId = "verified-role-id";

    @Comment({
            "Role Synchronization:",
            "Map Minecraft LuckPerms groups to Discord Role IDs.",
            "Format: <luckperms_group>: <discord_role_id>",
            "Example:",
            "  vip: '123456789012345678'",
            "  staff: '987654321098765432'"
    })
    public Map<String, String> roles = new HashMap<String, String>() {
        {
            put("vip", "");
            put("vipplus", "");
            put("mvip", "");
            put("mvipplus", "");
        }
    };

    @Comment({
            "----------------------------------------------------------------",
            " INFORMATION PANEL",
            "----------------------------------------------------------------",
            "Status of the info panel message: 'sent' or 'not_sent'.",
            "If set to 'not_sent', the bot will try to send a new message and update the config."
    })
    @ConfigPath("information-message")
    public String informationMessage = "not_sent";

    @Comment("The ID of the channel where the information/linking panel will be displayed.")
    @ConfigPath("information-channel-id")
    public String informationChannelId = "information-channel-id";

    @Comment({
            "----------------------------------------------------------------",
            " ONE-TIME REWARDS",
            "----------------------------------------------------------------",
            "Commands to execute in console when a player links their account for THE FIRST TIME."
    })
    public List<String> rewards = Arrays.asList(
            "give %player% minecraft:diamond 5",
            "say %player% just linked their account and earned rewards!",
            "experience add %player% 100 points");

    @Comment({
            "----------------------------------------------------------------",
            " DAILY REWARDS SYSTEM",
            "----------------------------------------------------------------",
            "Cooldown between daily reward claims in milliseconds (example: 86400000 = 24 hours)."
    })
    @ConfigPath("reward-cooldown")
    @Validate(min = 0)
    public long rewardCooldown = 86400000L;

    @Comment("Commands to execute when a player claims their daily reward.")
    @ConfigPath("daily-rewards")
    public List<String> dailyRewards = Arrays.asList(
            "give %player% diamond 1",
            "xp %player% add 100");

    @Comment({
            "----------------------------------------------------------------",
            " BOOSTER SYSTEM",
            "----------------------------------------------------------------",
            "The channel ID where the Booster Panel will be displayed."
    })
    @ConfigPath("booster-channel-id")
    public String boosterChannelId = "booster-channel-id";

    @Comment("The cooldown between booster reward claims in SECONDS (example: 86400 = 24 hours).")
    @ConfigPath("booster-role-time")
    public long boosterRoleTime = 86400L;

    @Comment("The Discord Role ID that grants access to booster rewards.")
    @ConfigPath("booster-role-id")
    public String boosterRoleId = "booster-role-id";

    @Comment({
            "Booster Reward Packages:",
            "Configure different rewards or messages for boosters."
    })
    @ConfigPath("booster-rewards")
    public Map<String, RewardItem> boosterRewards = new HashMap<String, RewardItem>() {
        {
        }
    };

    public static class RewardItem {
        public String name;
        public List<String> commands;

        public RewardItem() {
        }

        public RewardItem(String name, List<String> commands) {
            this.name = name;
            this.commands = commands;
        }
    }

    @Comment({
            "----------------------------------------------------------------",
            " GENERAL SETTINGS",
            "----------------------------------------------------------------",
            "Preferred language for system messages.",
            "Available: en, tr, es, fr, de, zh"
    })
    public String lang = "en";

    public static class CommandSection {
        public String main = "link|sync|accountlink";
        public SubCommandSection subs = new SubCommandSection();

        public static class SubCommandSection {
            public String esle = "link|code";
            public String onayla = "confirm|approve";
            public String iptal = "cancel|stop";
            public String kaldir = "unlink|remove";
            public String ikifa = "2fa|security";
            public String liste = "list|all";
            public String sifirla = "reset|clear";
            public String odul = "reward|claim";
            public String yenile = "reload|refresh";
        }
    }
}