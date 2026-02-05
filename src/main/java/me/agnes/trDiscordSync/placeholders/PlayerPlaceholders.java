package me.agnes.trDiscordSync.placeholders;

import com.bentahsin.benthpapimanager.annotations.Inject;
import com.bentahsin.benthpapimanager.annotations.Placeholder;
import com.bentahsin.benthpapimanager.annotations.PlaceholderIdentifier;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import me.agnes.trDiscordSync.data.EslestirmeManager;
import me.agnes.trDiscordSync.discord.DiscordBot;
import net.dv8tion.jda.api.JDA;
import org.bukkit.OfflinePlayer;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
@Placeholder(identifier = "trdiscordsync", author = "Agnes & bentahsin", version = "1.2.6")
public class PlayerPlaceholders {

    @Inject
    private DiscordBot discordBot;

    private final LoadingCache<String, String> discordNameCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(discordId -> {
                JDA jda = discordBot.getJda();
                if (jda == null)
                    return "Unknown";

                try {
                    return jda.retrieveUserById(discordId).complete().getName();
                } catch (Exception e) {
                    return "Unknown";
                }
            });

    @PlaceholderIdentifier(identifier = "durum", onError = "§cError")
    public String getDurum(OfflinePlayer player) {
        if (player == null)
            return "No Player";
        return EslestirmeManager.eslesmeVar(player.getUniqueId()) ? "§aLinked" : "§cNot Linked";
    }

    @PlaceholderIdentifier(identifier = "discord_id", onError = "-")
    public String getDiscordId(OfflinePlayer player) {
        if (player == null || !EslestirmeManager.eslesmeVar(player.getUniqueId())) {
            return "None";
        }
        return EslestirmeManager.getDiscordId(player.getUniqueId());
    }

    @PlaceholderIdentifier(identifier = "discord_adi", onError = "Unknown")
    public String getDiscordAdi(OfflinePlayer player) {
        if (player == null || !EslestirmeManager.eslesmeVar(player.getUniqueId())) {
            return "None";
        }
        String discordId = EslestirmeManager.getDiscordId(player.getUniqueId());
        if (discordId == null)
            return "Unknown";

        return discordNameCache.get(discordId);
    }

    @PlaceholderIdentifier(identifier = "2fa_durum", onError = "§cError")
    public String get2faDurum(OfflinePlayer player) {
        if (player == null || !EslestirmeManager.eslesmeVar(player.getUniqueId())) {
            return "§7-";
        }
        return EslestirmeManager.isIkiFAOpen(player.getUniqueId()) ? "§aActive" : "§cInactive";
    }
}