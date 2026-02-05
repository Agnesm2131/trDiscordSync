package me.agnes.trDiscordSync;

import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.PaperCommandManager;
import com.bentahsin.benthpapimanager.BenthPAPIManager;
import com.bentahsin.configuration.Configuration;
import me.agnes.trDiscordSync.commands.EsleCommandACF;
import me.agnes.trDiscordSync.configuration.MainConfig;
import me.agnes.trDiscordSync.data.DatabaseManager;
import me.agnes.trDiscordSync.discord.DiscordBot;
import me.agnes.trDiscordSync.data.EslestirmeManager;
import me.agnes.trDiscordSync.listener.PlayerLoginListener;
import me.agnes.trDiscordSync.placeholders.PlayerPlaceholders;
import me.agnes.trDiscordSync.placeholders.ServerPlaceholders;
import me.agnes.trDiscordSync.util.LuckPermsUtil;
import me.agnes.trDiscordSync.util.MessageUtil;
import me.agnes.trDiscordSync.configuration.GuiConfig;
import me.agnes.trDiscordSync.util.SchedulerUtil;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class trDiscordSync extends JavaPlugin {

    private static trDiscordSync instance;
    private Configuration configManager;
    private MainConfig mainConfig;
    private GuiConfig guiConfig;
    private DiscordBot discordBot;
    private LuckPerms luckPerms;
    private BenthPAPIManager papiMgr;
    private LuckPermsUtil luckPermsUtil;
    private me.agnes.trDiscordSync.menu.ProfileMenu profileMenu;

    private File rewardsDataFile;
    private FileConfiguration rewardsDataConfig;

    @Override
    public void onEnable() {
        instance = this;

        this.configManager = new Configuration(this);
        this.mainConfig = new MainConfig();
        this.guiConfig = new GuiConfig();
        this.configManager.init(mainConfig, "config.yml");
        this.configManager.init(guiConfig, "gui.yml");

        DatabaseManager.init();

        try {
            this.luckPerms = LuckPermsProvider.get();
            getLogger().info("[trDiscordSync] LuckPerms API successfully linked.");
            if (this.luckPerms != null) {
                this.luckPermsUtil = new LuckPermsUtil(this.luckPerms, getLogger());
            }
        } catch (IllegalStateException e) {
            getLogger()
                    .warning("[trDiscordSync] LuckPerms API not found! Rank features will be disabled.");
            getLogger().severe(e.getMessage());
            this.luckPerms = null;
            this.luckPermsUtil = null;
        }

        String token = getMainConfig().token;

        if (token.equals("DISCORD_BOT_TOKEN")) {
            getLogger().severe("---------------------------------------------------");
            getLogger().severe("ERROR: Discord Bot Token is not set!");
            getLogger().severe("Please edit config.yml and restart the server.");
            getLogger().severe("Disabling plugin...");
            getLogger().severe("---------------------------------------------------");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.discordBot = new DiscordBot(token);

        MessageUtil.load();
        MessageUtil.setLang(mainConfig.lang);

        this.discordBot.start();

        PaperCommandManager commandManager = new PaperCommandManager(this);

        commandManager.getCommandReplacements().addReplacement("main_cmd", mainConfig.commands.main);
        commandManager.getCommandReplacements().addReplacement("sub_esle", mainConfig.commands.subs.esle);
        commandManager.getCommandReplacements().addReplacement("sub_onayla", mainConfig.commands.subs.onayla);
        commandManager.getCommandReplacements().addReplacement("sub_iptal", mainConfig.commands.subs.iptal);
        commandManager.getCommandReplacements().addReplacement("sub_kaldir", mainConfig.commands.subs.kaldir);
        commandManager.getCommandReplacements().addReplacement("sub_2fa", mainConfig.commands.subs.ikifa);
        commandManager.getCommandReplacements().addReplacement("sub_liste", mainConfig.commands.subs.liste);
        commandManager.getCommandReplacements().addReplacement("sub_sifirla", mainConfig.commands.subs.sifirla);
        commandManager.getCommandReplacements().addReplacement("sub_odul", mainConfig.commands.subs.odul);
        commandManager.getCommandReplacements().addReplacement("sub_yenile", mainConfig.commands.subs.yenile);

        commandManager.getCommandContexts().registerContext(BukkitCommandIssuer.class, c -> {
            return commandManager.getCommandIssuer(c.getSender());
        });

        commandManager.getCommandCompletions().registerAsyncCompletion("@players", c -> {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        });

        commandManager.registerCommand(new EsleCommandACF(this));

        EslestirmeManager.init();

        this.profileMenu = new me.agnes.trDiscordSync.menu.ProfileMenu(this);
        getServer().getPluginManager().registerEvents(new PlayerLoginListener(discordBot), this);
        getServer().getPluginManager().registerEvents(new me.agnes.trDiscordSync.listener.MenuListener(this), this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                this.papiMgr = BenthPAPIManager.create(this)
                        .withInjectable(DiscordBot.class, this.discordBot)
                        .withInjectable(LuckPerms.class, this.luckPerms)
                        .withDebugMode()
                        .register(
                                PlayerPlaceholders.class,
                                ServerPlaceholders.class);
                getLogger().info("PlaceholderAPI support successfully enabled.");
            } catch (Exception e) {
                getLogger().severe("An error occurred while starting BenthPAPIManager!");
                getLogger().warning(e.getMessage());
            }
        } else {
            getLogger().warning("PlaceholderAPI not found, placeholders could not be loaded.");
        }

        createRewardsDataFile();
        createReadmeFile();
        getLogger().info("Data Files Loaded!");

        getLogger().info("[trDiscordSync] Plugin successfully loaded!");
    }

    @Override
    public void onDisable() {
        if (this.papiMgr != null) {
            try {
                this.papiMgr.unregisterAll();
            } catch (Exception ignored) {
            }
        }
        if (discordBot != null)
            discordBot.shutdown();
        getLogger().info("[trDiscordSync] Plugin disabled!");
    }

    public static trDiscordSync getInstance() {
        return instance;
    }

    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public LuckPermsUtil getLuckPermsUtil() {
        return luckPermsUtil;
    }

    public GuiConfig getGuiConfig() {
        return guiConfig;
    }

    public me.agnes.trDiscordSync.menu.ProfileMenu getProfileMenu() {
        return profileMenu;
    }

    private void createRewardsDataFile() {
        rewardsDataFile = new File(getDataFolder(), "rewards-data.yml");
        if (!rewardsDataFile.exists()) {
            boolean ignored = rewardsDataFile.getParentFile().mkdirs();
            saveResource("rewards-data.yml", false);
        }
        rewardsDataConfig = YamlConfiguration.loadConfiguration(rewardsDataFile);
    }

    public FileConfiguration getRewardsDataConfig() {
        return rewardsDataConfig;
    }

    public void saveRewardsDataConfig() {
        try {
            rewardsDataConfig.save(rewardsDataFile);
        } catch (IOException e) {
            getLogger().severe(e.getMessage());
        }
    }

    /**
     * Oyuncunun günlük ödülünü kontrol eder ve verir.
     * Sonuç Discord üzerinden InteractionHook ile bildirilir.
     *
     * @param playerUUID Ödül kontrolü yapılacak oyuncunun UUID'si
     * @param hook       Discord etkileşim kancası (Cevap vermek için)
     */
    public void handleRewardCheck(UUID playerUUID, InteractionHook hook) {
        SchedulerUtil.runSync(() -> {

            if (playerUUID == null) {
                hook.sendMessage(MessageUtil.stripColors(MessageUtil.getMessage("odul-sistemi.eslesmemis")))
                        .setEphemeral(true).queue();
                return;
            }

            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null || !player.isOnline()) {
                hook.sendMessage(MessageUtil.stripColors(MessageUtil.getMessage("odul-sistemi.oyuncu-cevrimdisi")))
                        .setEphemeral(true).queue();
                return;
            }

            FileConfiguration rewardsData = getRewardsDataConfig();
            String path = playerUUID + ".lastClaim";

            long lastClaim = rewardsData.getLong(path, 0);
            long cooldown = getMainConfig().rewardCooldown;
            long now = System.currentTimeMillis();
            long timeDiff = now - lastClaim;

            if (timeDiff < cooldown) {
                long remainingMillis = cooldown - timeDiff;
                long hours = remainingMillis / 3600000;
                long minutes = (remainingMillis % 3600000) / 60000;
                long seconds = (remainingMillis % 60000) / 1000;

                Map<String, String> timeVars = new HashMap<>();
                timeVars.put("hours", String.format("%02d", hours));
                timeVars.put("minutes", String.format("%02d", minutes));
                timeVars.put("seconds", String.format("%02d", seconds));

                String timeString = MessageUtil.getMessage("odul-sistemi.zaman-formati", timeVars);

                Map<String, String> msgVars = new HashMap<>();
                msgVars.put("time", timeString);

                hook.sendMessage(
                        MessageUtil.stripColors(MessageUtil.getMessage("odul-sistemi.bekleme-suresi", msgVars)))
                        .setEphemeral(true).queue();
                return;
            }

            List<String> rewardCommands = getMainConfig().dailyRewards;
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            for (String cmd : rewardCommands) {
                Bukkit.dispatchCommand(console, cmd.replace("%player%", player.getName()));
            }

            rewardsData.set(path, now);
            SchedulerUtil.runAsync(this::saveRewardsDataConfig);

            hook.sendMessage(MessageUtil.stripColors(MessageUtil.getMessage("odul-sistemi.basarili")))
                    .setEphemeral(true).queue();
        });
    }

    public void odulVer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            getLogger().info("Delivering reward to: " + player.getName());
            player.sendMessage(MessageUtil.getMessage("reward-message"));

            for (String cmd : getMainConfig().rewards) {
                String command = cmd.replace("%player%", player.getName());
                getLogger().info("Executing command: " + command);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }
    }

    public MainConfig getMainConfig() {
        return mainConfig;
    }

    public Configuration getConfigManager() {
        return configManager;
    }

    @Override
    public void reloadConfig() {
        if (configManager != null) {
            configManager.reload(mainConfig, "config.yml");
        }
    }

    private void createReadmeFile() {
        File file = new File(getDataFolder(), "README.txt");
        if (file.exists())
            return;

        try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
            writer.println("# AgnAccountLinking - Advanced Minecraft & Discord Integration");
            writer.println("");
            writer.println("Need help? Follow these steps to set up your plugin:");
            writer.println("");
            writer.println("1. Create a Discord Bot at https://discord.com/developers/applications");
            writer.println("2. Enable ALL Privileged Gateway Intents (Presence, Members, Content)");
            writer.println("3. Copy your Bot Token and paste it into config.yml");
            writer.println(
                    "4. Invite the bot using OAuth2 URL Generator (bot + applications.commands + Administrator)");
            writer.println("5. Fill in the guild-id, log-channel-id, and information-channel-id in config.yml");
            writer.println("6. Restart the server or use /link reload");
            writer.println("");
            writer.println("Commands:");
            writer.println("- /link link: Generate a link code");
            writer.println("- /link confirm: Confirm the link in Minecraft");
            writer.println("- /link list: (Admin) List all linked accounts");
            writer.println("");
            writer.println("Thank you for using Agnes Account Linking!");
            writer.flush();
        } catch (java.io.IOException e) {
            getLogger().warning("Could not create README.txt file: " + e.getMessage());
        }
    }
}
