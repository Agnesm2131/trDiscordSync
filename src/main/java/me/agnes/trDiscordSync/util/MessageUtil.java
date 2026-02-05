package me.agnes.trDiscordSync.util;

import me.agnes.trDiscordSync.trDiscordSync;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessageUtil {

    private static final Map<String, FileConfiguration> messagesByLang = new HashMap<>();
    private static String lang = "en"; // default language

    // Loads language files, saves if missing, and logs to console
    public static void load() {
        String[] langs = { "tr", "en", "es", "fr", "de", "zh" };
        File pluginFolder = trDiscordSync.getInstance().getDataFolder();

        for (String l : langs) {
            File file = new File(pluginFolder, "langs/messages_" + l + ".yml");

            if (!file.exists()) {
                trDiscordSync.getInstance().saveResource("langs/messages_" + l + ".yml", false);
                trDiscordSync.getInstance().getLogger().info("[MessageUtil] " + file.getName() + " saved.");
            } else {
                trDiscordSync.getInstance().getLogger().info("[MessageUtil] " + file.getName() + " already exists.");
            }

            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            messagesByLang.put(l, cfg);
            trDiscordSync.getInstance().getLogger()
                    .info("[MessageUtil] " + l + " language loaded, " + cfg.getKeys(false).size() + " messages found.");
        }
    }

    // Changes language, falls back to 'en' if not found
    public static void setLang(String newLang) {
        if (newLang == null) {
            trDiscordSync.getInstance().getLogger().warning("[MessageUtil] setLang received null, using 'en'.");
            lang = "en";
            return;
        }

        newLang = newLang.toLowerCase();

        if (messagesByLang.containsKey(newLang)) {
            lang = newLang;
            trDiscordSync.getInstance().getLogger().info("[MessageUtil] Language changed to: " + lang);
        } else {
            trDiscordSync.getInstance().getLogger()
                    .warning("[MessageUtil] Language not found: " + newLang + ", falling back to 'en'.");
            lang = "en";
        }
    }

    // Returns the current language config, fallback to 'en'
    private static FileConfiguration getMessages() {
        FileConfiguration config = messagesByLang.get(lang);
        if (config == null) {
            trDiscordSync.getInstance().getLogger().warning("[MessageUtil] " + lang + " language file not found!");
            config = messagesByLang.get("en");
            if (config == null) {
                trDiscordSync.getInstance().getLogger().warning("[MessageUtil] English language file also not found!");
                return new YamlConfiguration();
            }
        }
        return config;
    }

    // Mesajı path'e göre alır, değişkenleri yerleştirir, renklendirir
    public static String getMessage(String path, Map<String, String> vars) {
        if (path == null || path.isEmpty())
            return "";

        FileConfiguration messages = getMessages();
        String message = messages.getString(path);

        if (message == null) {
            // Eğer path değil de doğrudan metin gönderilmişse (örn GUI isimleri) sadece
            // renklendir
            message = path;
        }

        if (vars != null) {
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        return color(message);
    }

    // Doğrudan renklendirme
    public static String color(String text) {
        if (text == null)
            return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    // Varsız getMessage overload
    public static String getMessage(String path) {
        return getMessage(path, null);
    }

    // Mesajdan renk kodlarını temizler
    public static String stripColors(String message) {
        return net.md_5.bungee.api.ChatColor.stripColor(message);
    }

    // Başlık gönderir, değişkenleri yerleştirir
    public static void sendTitle(Player p, String path, Map<String, String> vars) {
        FileConfiguration messages = getMessages();

        String title = messages.getString(path + ".title");
        String subtitle = messages.getString(path + ".subtitle");

        if (title == null)
            title = "§cTitle Not Found";
        if (subtitle == null)
            subtitle = "§7Subtitle not found";

        if (vars != null) {
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                title = title.replace("%" + entry.getKey() + "%", entry.getValue());
                subtitle = subtitle.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        p.sendTitle(ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subtitle),
                10, 70, 20);
    }

    public static void sendTitle(Player p, String path) {
        sendTitle(p, path, null);
    }

    // Reloads message files
    public static void yenile() {
        messagesByLang.clear();
        load();
        trDiscordSync plugin = trDiscordSync.getInstance();
        plugin.reloadConfig();
        trDiscordSync.getInstance().getLogger().info("[MessageUtil] Message files reloaded.");
    }
}
