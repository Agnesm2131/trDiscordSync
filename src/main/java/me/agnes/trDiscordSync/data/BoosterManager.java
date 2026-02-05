package me.agnes.trDiscordSync.data;

import me.agnes.trDiscordSync.trDiscordSync;
import me.agnes.trDiscordSync.configuration.MainConfig;
import me.agnes.trDiscordSync.util.MessageUtil;
import me.agnes.trDiscordSync.util.SchedulerUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BoosterManager {

    public static void handleBoosterClaim(ButtonInteractionEvent event) {
        String discordId = event.getUser().getId();
        MainConfig config = trDiscordSync.getInstance().getMainConfig();
        UUID uuid = EslestirmeManager.getUUIDByDiscordId(discordId);

        if (uuid == null) {
            event.reply(MessageUtil.stripColors(MessageUtil.getMessage("booster-sistemi.hesap-eslenmemis")))
                    .setEphemeral(true).queue();
            return;
        }

        Member member = event.getMember();
        if (member == null)
            return;

        // Check if user has any of the booster roles
        String boosterRoleId = config.boosterRoleId;
        boolean isBooster = member.getRoles().stream().anyMatch(role -> role.getId().equals(boosterRoleId));

        if (!isBooster) {
            event.reply(MessageUtil.stripColors(MessageUtil.getMessage("booster-sistemi.booster-degil")))
                    .setEphemeral(true).queue();
            return;
        }

        long lastClaim = EslestirmeManager.getBoosterSonAlim(uuid);
        long now = System.currentTimeMillis();
        long cooldownMillis = config.boosterRoleTime * 1000L;

        if (now - lastClaim < cooldownMillis) {
            long remainingSeconds = (cooldownMillis - (now - lastClaim)) / 1000L;
            Map<String, String> vars = new HashMap<>();
            vars.put("time", formatTime(remainingSeconds));
            event.reply(MessageUtil.stripColors(MessageUtil.getMessage("booster-sistemi.bekleme-suresi", vars)))
                    .setEphemeral(true).queue();
            return;
        }

        // Give rewards
        SchedulerUtil.runSync(() -> {
            Player player = Bukkit.getPlayer(uuid);
            String playerName = player != null ? player.getName() : Bukkit.getOfflinePlayer(uuid).getName();

            if (playerName == null) {
                event.reply("❌ Oyuncu adı bulunamadı!").setEphemeral(true).queue();
                return;
            }

            for (MainConfig.RewardItem rewardItem : config.boosterRewards.values()) {
                if (rewardItem.commands != null) {
                    for (String cmd : rewardItem.commands) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", playerName));
                    }
                }
            }

            EslestirmeManager.setBoosterSonAlim(uuid, now);

            Map<String, String> announceVars = new HashMap<>();
            announceVars.put("player", playerName);
            String announceMsg = MessageUtil.getMessage("booster-sistemi.duyuru", announceVars);
            if (announceMsg != null && !announceMsg.isEmpty()) {
                Bukkit.broadcastMessage(announceMsg);
            }

            event.reply(MessageUtil.stripColors(MessageUtil.getMessage("booster-sistemi.basarili")))
                    .setEphemeral(true).queue();
        });
    }

    public static void handleBoosterStatus(ButtonInteractionEvent event) {
        String discordId = event.getUser().getId();
        MainConfig config = trDiscordSync.getInstance().getMainConfig();
        UUID uuid = EslestirmeManager.getUUIDByDiscordId(discordId);

        if (uuid == null) {
            event.reply(MessageUtil.stripColors(MessageUtil.getMessage("booster-sistemi.hesap-eslenmemis")))
                    .setEphemeral(true).queue();
            return;
        }

        long lastClaim = EslestirmeManager.getBoosterSonAlim(uuid);
        long now = System.currentTimeMillis();
        long cooldownMillis = config.boosterRoleTime * 1000L;

        if (now - lastClaim >= cooldownMillis) {
            event.reply(MessageUtil.stripColors(MessageUtil.getMessage("booster-sistemi.durum-hazir")))
                    .setEphemeral(true).queue();
        } else {
            long remainingSeconds = (cooldownMillis - (now - lastClaim)) / 1000L;
            Map<String, String> vars = new HashMap<>();
            vars.put("time", formatTime(remainingSeconds));
            event.reply(MessageUtil.stripColors(MessageUtil.getMessage("booster-sistemi.durum-beklemede", vars)))
                    .setEphemeral(true).queue();
        }
    }

    private static String formatTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d saat %02d dakika %02d saniye", h, m, s);
    }
}
