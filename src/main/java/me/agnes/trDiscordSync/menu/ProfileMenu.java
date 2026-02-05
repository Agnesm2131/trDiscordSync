package me.agnes.trDiscordSync.menu;

import me.agnes.trDiscordSync.trDiscordSync;
import me.agnes.trDiscordSync.configuration.GuiConfig;
import me.agnes.trDiscordSync.data.EslestirmeManager;
import me.agnes.trDiscordSync.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProfileMenu {

    private final trDiscordSync plugin;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public ProfileMenu(trDiscordSync plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, OfflinePlayer target) {
        GuiConfig guiConfig = plugin.getGuiConfig();
        GuiConfig.ProfileMenuSection section = guiConfig.profileMenu;

        String title = section.title.replace("%player%", target.getName() != null ? target.getName() : "Unknown");
        Inventory inv = Bukkit.createInventory(null, section.size, MessageUtil.color(title));

        ItemStack fill = createItem(section.fillItem, target);
        if (section.fillItem.slots != null) {
            for (int slot : section.fillItem.slots) {
                if (slot >= 0 && slot < inv.getSize())
                    inv.setItem(slot, fill);
            }
        }

        section.items.forEach((key, itemConfig) -> {
            ItemStack item = createItem(itemConfig, target);
            if (itemConfig.slot >= 0 && itemConfig.slot < inv.getSize()) {
                inv.setItem(itemConfig.slot, item);
            }
        });

        viewer.openInventory(inv);
    }

    private ItemStack createItem(GuiConfig.MenuItem config, OfflinePlayer target) {
        Material material = Material.matchMaterial(config.material);
        if (material == null)
            material = Material.BARRIER;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        String name = config.name.replace("%player%", target.getName() != null ? target.getName() : "Unknown");
        meta.setDisplayName(MessageUtil.color(name));

        UUID uuid = target.getUniqueId();
        boolean linked = EslestirmeManager.eslesmeVar(uuid);
        String discordId = EslestirmeManager.getDiscordId(uuid);
        String ip = EslestirmeManager.getKayitliIP(uuid);
        boolean twoFa = EslestirmeManager.isIkiFAOpen(uuid);
        long dateMillis = EslestirmeManager.getEslesmeTarihi(uuid);
        String date = dateMillis > 0 ? DATE_FORMAT.format(new Date(dateMillis)) : "ɴᴏɴᴇ";

        String boosterStatus = "&cʏᴏᴋ";
        String boosterReward = linked ? "&aʜᴀᴢɪʀ" : "&cᴇsʟᴇsᴍᴇᴍɪs";

        if (discordId != null && plugin.getDiscordBot() != null && plugin.getDiscordBot().getJda() != null) {
            String guildId = plugin.getMainConfig().guildId;
            String boosterRoleId = plugin.getMainConfig().boosterRoleId;

            if (isSnowflake(guildId) && isSnowflake(boosterRoleId)) {
                try {
                    net.dv8tion.jda.api.entities.Guild guild = plugin.getDiscordBot().getJda().getGuildById(guildId);
                    if (guild != null) {
                        net.dv8tion.jda.api.entities.Role boosterRole = guild.getRoleById(boosterRoleId);
                        net.dv8tion.jda.api.entities.Member member = guild.retrieveMemberById(discordId).complete();
                        if (member != null && boosterRole != null && member.getRoles().contains(boosterRole)) {
                            boosterStatus = "&dᴀᴋᴛɪꜰ";

                            long lastBoosterClaim = EslestirmeManager.getBoosterSonAlim(uuid);
                            long boosterCooldown = plugin.getMainConfig().boosterRoleTime * 1000L;
                            if (System.currentTimeMillis() - lastBoosterClaim < boosterCooldown) {
                                boosterReward = "&eʙᴇᴋʟᴇᴍᴇᴅᴇ";
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        final String finalBoosterStatus = boosterStatus;
        final String finalBoosterReward = boosterReward;

        List<String> lore = config.lore.stream().map(line -> {
            line = line.replace("%player%", target.getName() != null ? target.getName() : "Unknown")
                    .replace("%status%", linked ? "&aᴇsʟᴇsᴍɪs" : "&cᴇsʟᴇsᴍᴇᴍɪs")
                    .replace("%discord_id%", discordId != null ? discordId : "ʙᴜʟᴜɴᴀᴍᴀᴅɪ")
                    .replace("%2fa%", twoFa ? "&aᴀᴄɪᴋ" : "&cᴋᴀᴘᴀʟɪ")
                    .replace("%ip%", ip != null ? ip : "ʙɪʟɪɴᴍɪʏᴏʀ")
                    .replace("%date%", date)
                    .replace("%booster_status%", finalBoosterStatus)
                    .replace("%booster_reward%", finalBoosterReward);
            return MessageUtil.color(line);
        }).collect(Collectors.toList());

        meta.setLore(lore);

        if (meta instanceof SkullMeta && target.getName() != null) {
            ((SkullMeta) meta).setOwningPlayer(target);
        }

        item.setItemMeta(meta);
        return item;
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
}
