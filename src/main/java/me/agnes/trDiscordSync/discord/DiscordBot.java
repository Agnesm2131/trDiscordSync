package me.agnes.trDiscordSync.discord;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.agnes.trDiscordSync.util.MessageUtil;
import me.agnes.trDiscordSync.util.SchedulerUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import java.util.*;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import me.agnes.trDiscordSync.trDiscordSync;
import me.agnes.trDiscordSync.configuration.MainConfig;
import me.agnes.trDiscordSync.data.EslestirmeManager;
import me.agnes.trDiscordSync.data.BoosterManager;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import java.awt.Color;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscordBot extends ListenerAdapter {

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("kod_gir_modal")) {
            String kod = Objects.requireNonNull(event.getValue("kod")).getAsString();
            UUID uuid = EslestirmeManager.koduKontrolEt(kod);

            if (uuid == null) {
                event.reply(MessageUtil.stripColors(MessageUtil.getMessage("discord-modal-cevap.gecersiz-kod")))
                        .setEphemeral(true).queue();
                return;
            }

            if (EslestirmeManager.discordZatenEslesmis(event.getUser().getId())) {
                event.reply(MessageUtil.stripColors(MessageUtil.getMessage("discord-modal-cevap.discord-zaten-esli")))
                        .setEphemeral(true).queue();
                return;
            }

            boolean basarili = EslestirmeManager.eslestir(uuid, event.getUser().getId());

            if (basarili) {
                event.reply(MessageUtil.stripColors(MessageUtil.getMessage("discord-modal-cevap.basarili-yonlendirme")))
                        .setEphemeral(true).queue();
            } else {
                event.reply(MessageUtil.stripColors(MessageUtil.getMessage("discord-modal-cevap.basarisiz")))
                        .setEphemeral(true).queue();
            }
        }
    }

    private static final long ESLE_COOLDOWN_SECONDS = 60; // 1 dakika
    private static final long REPORT_COOLDOWN_SECONDS = 300; // 5 dakika

    private final Logger logger;
    private final String token;
    private JDA jda;

    // Cache'ler
    private final Cache<String, Long> esleCooldowns;
    private final Cache<String, Long> reportCooldowns;

    private String parsePlaceholders(String mesaj) {
        int aktifKullanici = Bukkit.getOnlinePlayers().size();
        return mesaj.replace("{online}", String.valueOf(aktifKullanici));
    }

    public DiscordBot(String token) {
        this.logger = trDiscordSync.getInstance().getLogger();
        this.token = token;
        this.esleCooldowns = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build();
        this.reportCooldowns = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();

    }

    private boolean isSnowflake(String id) {
        if (id == null || id.isEmpty() || id.length() < 17 || id.length() > 20)
            return false;
        try {
            Long.parseLong(id);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void sendBoosterPanel() {
        MainConfig config = trDiscordSync.getInstance().getMainConfig();
        if (!isSnowflake(config.boosterChannelId)) {
            logger.info("[DiscordBot] Booster channel ID is invalid or default, skipping booster panel.");
            return;
        }
        TextChannel kanal = jda.getTextChannelById(config.boosterChannelId);
        if (kanal == null)
            return;

        String base = "information-messages.booster-message";

        StringBuilder sb = new StringBuilder();
        config.boosterRewards.values()
                .forEach(r -> sb.append("• ").append(r.name).append("\n"));

        Map<String, String> footerVars = new HashMap<>();
        footerVars.put("server", kanal.getGuild().getName());

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(MessageUtil.getMessage(base + ".title"))
                .setColor(new Color(255, 115, 250))
                .setThumbnail(kanal.getGuild().getIconUrl())
                .setDescription(MessageUtil.getMessage(base + ".description"))
                .addField(
                        MessageUtil.getMessage(base + ".rewards-title"),
                        sb.length() > 0
                                ? sb.toString()
                                : MessageUtil.getMessage(base + ".rewards-empty"),
                        false)
                .setFooter(
                        MessageUtil.getMessage(base + ".footer", footerVars))
                .setTimestamp(java.time.Instant.now());

        kanal.getHistory().retrievePast(10).queue(msgs -> {
            if (!msgs.isEmpty())
                kanal.purgeMessages(msgs);

            kanal.sendMessageEmbeds(eb.build())
                    .setActionRow(
                            Button.primary(
                                    "booster_odul_al",
                                    MessageUtil.getMessage(base + ".button-claim")),
                            Button.secondary(
                                    "booster_durum_bak",
                                    MessageUtil.getMessage(base + ".button-status")))
                    .queue();
        });
    }

    public void start() {
        try {
            jda = JDABuilder.createDefault(token)
                    .setActivity(net.dv8tion.jda.api.entities.Activity
                            .playing(MessageUtil.getMessage("discord-bot-status-starting")))
                    .addEventListeners(this)
                    .build()
                    .awaitReady();

            // Commands
            jda.upsertCommand("link", "Link your Minecraft account with your Discord account")
                    .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "code",
                            "Enter your linking code",
                            true)
                    .queue();
            jda.upsertCommand("report", "Report a player to the server staff")
                    .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "player",
                            "Name of the player to report", true)
                    .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "reason",
                            "Reason for the report",
                            true)
                    .queue();
            jda.upsertCommand("query", "Query in-game information for a Discord member")
                    .addOption(
                            net.dv8tion.jda.api.interactions.commands.OptionType.USER,
                            "user",
                            "Discord member to query",
                            true)
                    .queue();
            jda.upsertCommand("info", "Get information about a user")
                    .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.USER, "user",
                            "User to get information about", true)
                    .queue();

            sendBoosterPanel();

            // Durum mesajları
            final List<String> durumlar = trDiscordSync.getInstance().getMainConfig().statusMessages;
            SchedulerUtil.runTimerSync(() -> {
                if (jda == null || jda.getStatus() != JDA.Status.CONNECTED)
                    return;
                if (durumlar == null || durumlar.isEmpty())
                    return;

                int index = (int) ((System.currentTimeMillis() / 5000) % durumlar.size());
                String mesaj = durumlar.get(index);
                String finalMesaj = parsePlaceholders(mesaj);
                net.dv8tion.jda.api.entities.Activity activity = net.dv8tion.jda.api.entities.Activity
                        .playing(finalMesaj);
                jda.getPresence().setActivity(activity);
            }, 0L, 100L);

            String infoMessageStatus = trDiscordSync.getInstance().getMainConfig().informationMessage;
            if ("not_sent".equalsIgnoreCase(infoMessageStatus)) {
                String kanalId = trDiscordSync.getInstance().getMainConfig().informationChannelId;
                if (!isSnowflake(kanalId)) {
                    logger.info("[DiscordBot] Information channel ID is invalid or default, skipping info panel.");
                } else {
                    TextChannel kanal = jda.getTextChannelById(kanalId);
                    if (kanal != null) {
                        Guild guild = kanal.getGuild();
                        String sunucuIkonURL = guild.getIconUrl();

                        String base = "information-messages.eslestirme-message";

                        EmbedBuilder embed = new EmbedBuilder()
                                .setTitle(MessageUtil.getMessage(base + ".title"))
                                .setColor(new Color(0x2F3136))
                                .setThumbnail(sunucuIkonURL)
                                .setDescription(MessageUtil.getMessage(base + ".description"))

                                .addField(
                                        MessageUtil.getMessage(base + ".how-title"),
                                        MessageUtil.getMessage(base + ".how-text"),
                                        false)

                                .addField(
                                        MessageUtil.getMessage(base + ".advantages-title"),
                                        MessageUtil.getMessage(base + ".advantages-text"),
                                        false)

                                .setFooter(MessageUtil.getMessage(base + ".footer"));

                        kanal.sendMessageEmbeds(embed.build())
                                .setActionRow(
                                        Button.secondary("hesap_durumu",
                                                MessageUtil.getMessage(base + ".button-status")),
                                        Button.success("eslestir",
                                                MessageUtil.getMessage(base + ".button-link")),
                                        Button.danger("eslesmeyi_kaldir",
                                                MessageUtil.getMessage(base + ".button-unlink")),
                                        Button.secondary("odul-kontrol",
                                                MessageUtil.getMessage(base + ".button-reward")))
                                .queue();

                        trDiscordSync.getInstance().getMainConfig().informationMessage = "sent";
                        trDiscordSync.getInstance().getConfigManager().save(trDiscordSync.getInstance().getMainConfig(),
                                "config.yml");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        MainConfig config = trDiscordSync.getInstance().getMainConfig();
        String discordId = event.getUser().getId();

        if (id.equals("booster_odul_al")) {
            BoosterManager.handleBoosterClaim(event);
            return;
        }

        if (id.equals("booster_durum_bak")) {
            BoosterManager.handleBoosterStatus(event);
            return;
        }

        switch (id) {
            case "hesap_durumu":
                handleHesapDurumu(event);
                break;
            case "eslestir":
                Modal modal = Modal.create("kod_gir_modal", "Enter Linking Code")
                        .addActionRows(
                                ActionRow.of(TextInput.create("kod_alanı", "Linking Code", TextInputStyle.SHORT)
                                        .setPlaceholder("Ex: X7Y2-Z9")
                                        .setRequired(true)
                                        .build()))
                        .build();
                event.replyModal(modal).queue();
                break;
            case "eslesmeyi_kaldir":
                handleEslesmeyiKaldir(event);
                break;
            case "odul-kontrol":
                final UUID playerUUIDCheck = EslestirmeManager.getUUIDByDiscordId(discordId);
                if (playerUUIDCheck == null) {
                    event.reply(MessageUtil.stripColors(MessageUtil.getMessage("discord-odul-butonu.hesap-bulunamadi")))
                            .setEphemeral(true).queue();
                    break;
                }
                event.reply(MessageUtil.stripColors(MessageUtil.getMessage("discord-odul-butonu.kontrol-ediliyor")))
                        .setEphemeral(true)
                        .queue(hook -> trDiscordSync.getInstance().handleRewardCheck(playerUUIDCheck, hook));
                break;
        }

        // 2FA Confirm
        if (id.startsWith("2fa_confirm_")) {
            try {
                String[] parts = id.split("_", 4);
                if (parts.length < 4) {
                    event.reply("Buton ID'si hatalı: " + id).setEphemeral(true).queue();
                    return;
                }
                UUID playerUUID = UUID.fromString(parts[2]);
                String newIP = parts[3];
                EslestirmeManager.setKayitliIP(playerUUID, newIP);
                event.reply(MessageUtil.getMessage("discord-2fa-confirm-reply")).setEphemeral(true).queue();
                event.getMessage().editMessage(MessageUtil.getMessage("discord-2fa-confirm-message-edit"))
                        .setComponents().queue();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "2FA onayı sırasında bir hata oluştu:", e);
            }
            return;
        }

        // 2FA Deny
        if (id.startsWith("2fa_deny_")) {
            try {
                event.reply(MessageUtil.getMessage("discord-2fa-deny-reply")).setEphemeral(true).queue();
                event.getMessage().editMessage(MessageUtil.getMessage("discord-2fa-deny-message-edit")).setComponents()
                        .queue();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "2FA reddi sırasında bir hata oluştu:", e);
            }
            return;
        }

        // Rapor Kontrol
        if (id.startsWith("report_kontrol_")) {
            try {
                String[] parts = id.split("_");
                if (parts.length < 4) {
                    event.reply(MessageUtil.getMessage("discord-button-invalid-id")).setEphemeral(true).queue();
                    return;
                }
                String raporlananDiscordId = parts[2];
                UUID uuidTarget = UUID.fromString(parts[3]);
                String yetkiliRolId = config.adminRoleId;

                if (yetkiliRolId == null || Objects.requireNonNull(event.getMember()).getRoles()
                        .stream().noneMatch(role -> role.getId().equals(yetkiliRolId))) {
                    event.reply(MessageUtil.getMessage("discord-button-no-permission")).setEphemeral(true).queue();
                    return;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(uuidTarget);
                Player player = Bukkit.getPlayer(target.getUniqueId());
                if (player != null) {
                    player.kickPlayer(MessageUtil.getMessage("discord-button-control-kick-reason"));
                }

                Map<String, String> vars = new HashMap<>();
                vars.put("player", target.getName());
                vars.put("discordId", raporlananDiscordId);
                event.reply(MessageUtil.getMessage("discord-button-control-reply", vars)).setEphemeral(true).queue();

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Rapor kontrolü hatası:", e);
            }
            return;
        }

        // Rapor Ban
        if (id.startsWith("report_ban_")) {
            try {
                String[] parts = id.split("_");
                UUID uuidTarget = UUID.fromString(parts[3]);
                String yetkiliRolId = config.adminRoleId;

                if (yetkiliRolId == null || Objects.requireNonNull(event.getMember()).getRoles()
                        .stream().noneMatch(role -> role.getId().equals(yetkiliRolId))) {
                    event.reply(MessageUtil.getMessage("discord-button-no-permission")).setEphemeral(true).queue();
                    return;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(uuidTarget);
                SchedulerUtil.runSync(() -> {
                    Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(target.getName(),
                            MessageUtil.getMessage("discord-button-ban-reason"), null, event.getUser().getAsTag());
                    Player p = Bukkit.getPlayer(target.getUniqueId());
                    if (p != null)
                        p.kickPlayer(MessageUtil.getMessage("discord-button-ban-kick-reason"));
                });

                Map<String, String> vars = new HashMap<>();
                vars.put("player", target.getName());
                event.reply(MessageUtil.getMessage("discord-button-ban-reply", vars)).setEphemeral(true).queue();

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Rapor ban hatası:", e);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    // Eşle Komutu
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        try {
            if (event.getName().equals("link")) {
                String userId = event.getUser().getId();
                if (isUserOnCooldown(userId, esleCooldowns, ESLE_COOLDOWN_SECONDS, event)) {
                    return;
                }

                String kod = Objects.requireNonNull(event.getOption("code")).getAsString().toUpperCase();

                event.deferReply(true).queue();

                SchedulerUtil.runAsync(() -> {
                    UUID uuid = EslestirmeManager.koduKontrolEt(kod);
                    if (uuid == null) {
                        event.getHook()
                                .sendMessage(MessageUtil.stripColors(MessageUtil.getMessage("discord-invalid-code")))
                                .queue();
                        return;
                    }

                    if (EslestirmeManager.eslesmeVar(uuid)) {
                        event.getHook().sendMessage(MessageUtil.getMessage("discord-already-linked-mc")).queue();
                        return;
                    }

                    if (EslestirmeManager.discordZatenEslesmis(event.getUser().getId())) {
                        event.getHook().sendMessage(MessageUtil.getMessage("discord-already-linked-discord")).queue();
                        return;
                    }

                    boolean basarili = EslestirmeManager.eslestir(uuid, event.getUser().getId());
                    if (!basarili) {
                        event.getHook().sendMessage(MessageUtil.getMessage("discord-generic-error")).queue();
                        return;
                    }

                    setUserCooldown(userId, esleCooldowns);
                    sendEslestirmeEmbed(uuid, event.getUser().getId());

                    event.getHook().sendMessage(MessageUtil.getMessage("discord-success")).queue();
                });
            }

            else if (event.getName().equals("query")) {

                Member member = event.getMember();
                String adminRoleId = trDiscordSync.getInstance().getMainConfig().adminRoleId;

                // Admin check
                if (member == null || adminRoleId == null ||
                        member.getRoles().stream().noneMatch(r -> r.getId().equals(adminRoleId))) {

                    event.reply("❌ You do not have permission to use this command.")
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                User targetUser = Objects.requireNonNull(event.getOption("user")).getAsUser();
                String discordId = targetUser.getId();

                event.deferReply(false).queue(); // PUBLIC EMBED

                SchedulerUtil.runAsync(() -> {

                    UUID uuid = EslestirmeManager.getUUIDByDiscordId(discordId);

                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("🔍 Account Linking Query")
                            .setColor(Color.CYAN)
                            .setThumbnail(targetUser.getEffectiveAvatarUrl())
                            .addField("💬 Discord",
                                    targetUser.getAsMention(),
                                    false);

                    if (uuid == null) {
                        embed.addField("🔗 Link Status", "❌ Not Linked", false)
                                .setFooter("Queried by: " + event.getUser().getAsTag())
                                .setTimestamp(java.time.Instant.now());

                        event.getHook().sendMessageEmbeds(embed.build()).queue();
                        return;
                    }

                    String playerName = Bukkit.getOfflinePlayer(uuid).getName();
                    boolean ikiFA = EslestirmeManager.isIkiFAOpen(uuid);
                    String ip = EslestirmeManager.getKayitliIP(uuid);
                    long eslesmeTarihi = EslestirmeManager.getEslesmeTarihi(uuid);

                    String minotarUrl = playerName != null
                            ? "https://minotar.net/helm/" + playerName + "/100.png"
                            : null;

                    if (minotarUrl != null) {
                        embed.setThumbnail(minotarUrl);
                    }

                    embed.addField("🎮 Player Name",
                            playerName != null ? "**" + playerName + "**" : "Unknown",
                            false)
                            .addField("🆔 UUID",
                                    uuid.toString(),
                                    false)
                            .addField("🔗 Link Status",
                                    "✅ Linked",
                                    false)
                            .addField("🔐 2FA",
                                    ikiFA ? "On ✅" : "Off ❌",
                                    false)
                            .addField("🌐 Registered IP",
                                    ip != null ? "||" + ip + "||" : "None",
                                    false);

                    if (eslesmeTarihi > 0) {
                        embed.addField("📅 Link Date",
                                "<t:" + (eslesmeTarihi / 1000) + ":F>",
                                false);
                    }

                    embed.setFooter("Queried by: " + event.getUser().getAsTag())
                            .setTimestamp(java.time.Instant.now());

                    event.getHook().sendMessageEmbeds(embed.build()).queue();
                });
            }

            else if (event.getName().equals("report")) {
                String userId = event.getUser().getId();

                if (isUserOnCooldown(userId, reportCooldowns, REPORT_COOLDOWN_SECONDS, event)) {
                    return;
                }

                String raporlananOyuncu = Objects.requireNonNull(event.getOption("player")).getAsString();
                String sebep = Objects.requireNonNull(event.getOption("reason")).getAsString();
                String raporlayanKullanici = Objects.requireNonNull(event.getUser()).getAsTag();

                String logKanalId = trDiscordSync.getInstance().getMainConfig().logChannelId;
                if (!isSnowflake(logKanalId)) {
                    event.reply(MessageUtil.getMessage("discord-report-channel-not-set")).setEphemeral(true).queue();
                    return;
                }

                TextChannel logKanali = jda.getTextChannelById(logKanalId);
                if (logKanali == null) {
                    event.reply(MessageUtil.getMessage("discord-report-channel-not-found")).setEphemeral(true).queue();
                    return;
                }

                Map<String, String> reportVars = new HashMap<>();
                reportVars.put("reporter", raporlayanKullanici);
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle(MessageUtil.getMessage("discord-report-embed-title"))
                        .setColor(Color.RED)
                        .addField(MessageUtil.getMessage("discord-report-embed-field-player"), raporlananOyuncu, false)
                        .addField(MessageUtil.getMessage("discord-report-embed-field-reason"), sebep, false)
                        .setFooter(MessageUtil.getMessage("discord-report-embed-footer", reportVars));

                logKanali.sendMessageEmbeds(embed.build()).queue();

                setUserCooldown(userId, reportCooldowns);

                event.reply(MessageUtil.getMessage("discord-report-success")).setEphemeral(true).queue();
            } else if (event.getName().equals("info")) {
                net.dv8tion.jda.api.entities.User targetUser = Objects.requireNonNull(event.getOption("user"))
                        .getAsUser();
                String discordId = targetUser.getId();

                UUID playerUUID = EslestirmeManager.getUUIDByDiscordId(discordId);

                if (playerUUID == null) {
                    event.reply(MessageUtil.getMessage("discord-info-reply-no-match")).setEphemeral(true).queue();
                } else {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
                    Map<String, String> vars = new HashMap<>();
                    vars.put("player", player.getName() != null ? player.getName() : "Unknown");
                    event.reply(MessageUtil.getMessage("discord-info-reply-success", vars)).setEphemeral(true).queue();
                }
            }
        } catch (Exception e) {
            logger.severe("An error occurred while processing slash command: " + event.getName());
            logger.severe(e.getMessage());
        }
    }

    public void changeNickname(String discordId, String newNickname) {
        String guildId = trDiscordSync.getInstance().getMainConfig().guildId;
        if (!isSnowflake(guildId)) {
            logger.warning("Guild ID geçersiz veya ayarlanmamış.");
            return;
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild == null)
            return;

        guild.retrieveMemberById(discordId).queue(member -> {
            Member self = guild.getSelfMember();

            if (!self.canInteract(member)) {
                logger.warning("Nickname değiştirilemedi (rol yetkisi yok): " + discordId);
                return;
            }

            if (newNickname.equals(member.getEffectiveName())) {
                return;
            }

            guild.modifyNickname(member, newNickname).queue();
        }, error -> {
            logger.warning("Kullanıcı bulunamadı: " + error.getMessage());
        });
    }

    public void addRoleToMember(String discordId, String roleId) {
        String guildId = trDiscordSync.getInstance().getMainConfig().guildId;
        if (!isSnowflake(guildId)) {
            System.out.println("AgnHesapEşle: Guild ID geçersiz veya ayarlanmamış.");
            return;
        }
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            System.out.println("Guild bulunamadı: " + guildId);
            return;
        }

        guild.retrieveMemberById(discordId).queue(member -> {
            if (!isSnowflake(roleId)) {
                System.out.println("Rol ID geçersiz: " + roleId);
                return;
            }
            Role role = guild.getRoleById(roleId);
            if (role == null) {
                System.out.println("Rol bulunamadı: " + roleId);
                return;
            }

            System.out.println("Rol bulunuyor, ekleniyor: " + role.getName());
            guild.addRoleToMember(member, role).queue(
                    success -> System.out.println("Rol başarıyla verildi: " + role.getName()),
                    error -> System.out.println("Rol verilirken hata: " + error.getMessage()));
        }, error -> System.out.println("Üye bulunamadı veya hata oluştu: " + error.getMessage()));
    }

    // 2FA Gönderme işlevi
    public void send2FAConfirmationMessage(UUID playerUUID, String playerName, String newIpAddress) {
        String discordId = EslestirmeManager.getDiscordId(playerUUID);
        if (discordId == null) {
            logger.warning("2FA mesajı gönderilemedi: " + playerUUID + " için Discord ID bulunamadı.");
            return;
        }
        if (jda == null)
            return;

        jda.retrieveUserById(discordId).queue(user -> {
            if (user == null)
                return;

            Map<String, String> vars = new HashMap<>();
            vars.put("player", playerName);
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(MessageUtil.getMessage("discord-2fa-embed-title"))
                    .setDescription(MessageUtil.getMessage("discord-2fa-embed-description", vars))
                    .addField(MessageUtil.getMessage("discord-2fa-embed-field-ip"), "||" + newIpAddress + "||", false)
                    .setColor(Color.ORANGE)
                    .setFooter(MessageUtil.getMessage("discord-2fa-embed-footer"));

            String confirmId = "2fa_confirm_" + playerUUID.toString() + "_" + newIpAddress;
            String denyId = "2fa_deny_" + playerUUID;

            Button confirmButton = Button.success(confirmId, MessageUtil.getMessage("discord-2fa-button-confirm"));
            Button denyButton = Button.danger(denyId, MessageUtil.getMessage("discord-2fa-button-deny"));

            user.openPrivateChannel().queue(channel -> {
                channel.sendMessageEmbeds(embed.build()).setActionRow(confirmButton, denyButton).queue();
            });
        }, throwable -> {
            logger.warning("2FA onayı için " + discordId + " ID'li kullanıcıya DM gönderilemedi.");
        });
    }

    // Cooldown
    private boolean isUserOnCooldown(String userId, Cache<String, Long> cooldowns, long cooldownTimeSeconds,
            net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent event) {
        Long lastUsed = cooldowns.getIfPresent(userId);

        if (lastUsed != null) {
            long secondsSinceLastUse = (System.currentTimeMillis() - lastUsed) / 1000;

            if (secondsSinceLastUse < cooldownTimeSeconds) {
                long timeLeft = cooldownTimeSeconds - secondsSinceLastUse;

                Map<String, String> vars = new HashMap<>();
                vars.put("timeLeft", String.valueOf(timeLeft));
                event.reply(MessageUtil.getMessage("discord-cooldown-message", vars)).setEphemeral(true).queue();

                return true;
            }
        }

        return false;
    }

    private void setUserCooldown(String userId, Cache<String, Long> cooldowns) {
        cooldowns.put(userId, System.currentTimeMillis());
    }

    public void shutdown() {
        if (jda != null)
            jda.shutdownNow();
    }

    public JDA getJda() {
        return jda;
    }

    private void handleHesapDurumu(ButtonInteractionEvent event) {
        String discordId = event.getUser().getId();
        UUID uuid = EslestirmeManager.getUUIDByDiscordId(discordId);

        if (uuid == null) {
            event.reply(MessageUtil.stripColors(
                    MessageUtil.getMessage("discord-hesap-durumu.eslesmemis"))).setEphemeral(true).queue();
            return;
        }

        String playerName = Bukkit.getOfflinePlayer(uuid).getName();
        boolean is2FA = EslestirmeManager.isIkiFAOpen(uuid);

        long eslesmeMillis = EslestirmeManager.getEslesmeTarihi(uuid);
        long days = 0;
        if (eslesmeMillis > 0) {
            days = (System.currentTimeMillis() - eslesmeMillis) / (1000L * 60 * 60 * 24);
        }

        Map<String, String> vars = new HashMap<>();
        vars.put("days", String.valueOf(days));

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(MessageUtil.stripColors(MessageUtil.getMessage("discord-hesap-durumu.embed-baslik")))
                .setColor(Color.CYAN)
                .addField(
                        MessageUtil.stripColors(MessageUtil.getMessage("discord-hesap-durumu.field-oyuncu")),
                        playerName != null ? playerName : "Bilinmiyor",
                        false)
                .addField(
                        MessageUtil.stripColors(MessageUtil.getMessage("discord-hesap-durumu.field-2fa")),
                        is2FA
                                ? MessageUtil.stripColors(MessageUtil.getMessage("discord-hesap-durumu.acik"))
                                : MessageUtil.stripColors(MessageUtil.getMessage("discord-hesap-durumu.kapali")),
                        false)
                .addField(
                        MessageUtil.stripColors(MessageUtil.getMessage("discord-hesap-durumu.field-tarih")),
                        MessageUtil.stripColors(
                                MessageUtil.getMessage("discord-hesap-durumu.field-tarih-deger", vars)),
                        false)
                .setFooter(MessageUtil.stripColors(MessageUtil.getMessage("discord-hesap-durumu.footer")));

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    // Eşleşme Kaldırma Butonu İşlevi
    private void handleEslesmeyiKaldir(ButtonInteractionEvent event) {
        String discordId = event.getUser().getId();
        UUID uuid = EslestirmeManager.getUUIDByDiscordId(discordId);

        if (uuid == null) {
            event.reply(MessageUtil.stripColors(MessageUtil.getMessage("discord-kaldir-butonu.eslesmemis")))
                    .setEphemeral(true).queue();
            return;
        }

        EslestirmeManager.kaldirEslesme(uuid);

        event.reply(MessageUtil.stripColors(MessageUtil.getMessage("discord-kaldir-butonu.basarili")))
                .setEphemeral(true).queue();
    }

    // Eşleştirme Log Gönderme
    public void sendEslestirmeEmbed(UUID playerUUID, String discordId) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        String playerName = player.getName() != null ? player.getName() : "Bilinmiyor";

        String logChannelId = trDiscordSync.getInstance().getMainConfig().logChannelId;
        if (!isSnowflake(logChannelId)) {
            logger.warning("Log kanalı ID'si geçersiz veya ayarlanmamış.");
            return;
        }

        TextChannel logChannel = jda.getTextChannelById(logChannelId);
        if (logChannel == null) {
            logger.warning("Log kanalı bulunamadı.");
            return;
        }

        String avatarUrl = "https://minotar.net/helm/" + playerName + "/100.png";

        Map<String, String> vars = new HashMap<>();
        vars.put("playerName", playerName);

        String title = MessageUtil.getMessage("log-message-embed.title");
        String playerFieldName = MessageUtil.getMessage("log-message-embed.player-field-name");
        String discordFieldName = MessageUtil.getMessage("log-message-embed.discord-field-name");
        String dateFieldName = MessageUtil.getMessage("log-message-embed.date-field-name");
        String securityFieldName = MessageUtil.getMessage("log-message-embed.security-field-name");
        String footer = MessageUtil.getMessage("log-message-embed.footer");

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setColor(Color.GREEN)
                .setThumbnail(avatarUrl)
                .addField(playerFieldName, playerName, true)
                .addField(discordFieldName, "<@" + discordId + ">", true)
                .addField(dateFieldName,
                        java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                                .format(java.time.LocalDateTime.now()),
                        false)
                .addField(securityFieldName, EslestirmeManager.isIkiFAOpen(playerUUID) ? "2FA Aktif ✅" : "2FA Kapalı ❌",
                        false)
                .setFooter(footer);

        logChannel.sendMessageEmbeds(embed.build()).queue();
    }
}